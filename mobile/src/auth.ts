import { clearTokens, loadTokens, saveTokens } from './tokenStore';
import type { StoredTokens } from './tokenStore.types';

export const AUTH_MODE = process.env.EXPO_PUBLIC_AUTH_MODE ?? 'dev';
const issuer = process.env.EXPO_PUBLIC_COGNITO_ISSUER_URI ?? '';
const clientId = process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? '';
const cognitoEndpoint = issuer ? new URL(issuer).origin : '';

type AuthenticationResult = {
  AccessToken?: string;
  RefreshToken?: string;
  IdToken?: string;
  ExpiresIn?: number;
};

type InitiateAuthResponse = {
  AuthenticationResult?: AuthenticationResult;
  ChallengeName?: string;
};

type SignUpResponse = {
  UserConfirmed?: boolean;
};

type CognitoErrorBody = {
  __type?: string;
  message?: string;
};

export class CognitoAuthError extends Error {
  constructor(public readonly code: string, message: string) {
    super(message);
    this.name = 'CognitoAuthError';
  }
}

export async function restoreAccessToken(): Promise<string | null> {
  if (AUTH_MODE !== 'cognito') return null;
  const tokens = await loadTokens();
  if (!tokens) return null;
  if (tokens.expiresAt > Date.now() + 30_000) return tokens.accessToken;
  return refreshAccessToken();
}

export async function refreshAccessToken(): Promise<string | null> {
  if (AUTH_MODE !== 'cognito' || !isConfigured()) return null;
  const current = await loadTokens();
  if (!current?.refreshToken) {
    await clearTokens();
    return null;
  }
  try {
    const response = await cognitoRequest<InitiateAuthResponse>('InitiateAuth', {
      AuthFlow: 'REFRESH_TOKEN_AUTH',
      ClientId: clientId,
      AuthParameters: { REFRESH_TOKEN: current.refreshToken },
    });
    const stored = storedTokens(response.AuthenticationResult, current.refreshToken, current.idToken);
    await saveTokens(stored);
    return stored.accessToken;
  } catch {
    await clearTokens();
    return null;
  }
}

export async function signInWithCognito(email: string, password: string): Promise<string> {
  requireConfiguration();
  const response = await cognitoRequest<InitiateAuthResponse>('InitiateAuth', {
    AuthFlow: 'USER_PASSWORD_AUTH',
    ClientId: clientId,
    AuthParameters: {
      USERNAME: normalizeEmail(email),
      PASSWORD: password,
    },
  });
  if (response.ChallengeName) {
    throw new CognitoAuthError(response.ChallengeName, '추가 인증이 필요합니다.');
  }
  const stored = storedTokens(response.AuthenticationResult);
  await saveTokens(stored);
  return stored.accessToken;
}

export async function signUpWithCognito(email: string, password: string, name: string): Promise<boolean> {
  requireConfiguration();
  const normalizedEmail = normalizeEmail(email);
  const response = await cognitoRequest<SignUpResponse>('SignUp', {
    ClientId: clientId,
    Username: normalizedEmail,
    Password: password,
    UserAttributes: [
      { Name: 'email', Value: normalizedEmail },
      { Name: 'name', Value: name.trim() || normalizedEmail.split('@')[0] },
    ],
  });
  return response.UserConfirmed === true;
}

export async function confirmSignUp(email: string, code: string): Promise<void> {
  requireConfiguration();
  await cognitoRequest('ConfirmSignUp', {
    ClientId: clientId,
    Username: normalizeEmail(email),
    ConfirmationCode: code.trim(),
  });
}

export async function resendConfirmationCode(email: string): Promise<void> {
  requireConfiguration();
  await cognitoRequest('ResendConfirmationCode', {
    ClientId: clientId,
    Username: normalizeEmail(email),
  });
}

export async function requestPasswordReset(email: string): Promise<void> {
  requireConfiguration();
  await cognitoRequest('ForgotPassword', {
    ClientId: clientId,
    Username: normalizeEmail(email),
  });
}

export async function confirmPasswordReset(
  email: string,
  code: string,
  newPassword: string,
): Promise<void> {
  requireConfiguration();
  await cognitoRequest('ConfirmForgotPassword', {
    ClientId: clientId,
    Username: normalizeEmail(email),
    ConfirmationCode: code.trim(),
    Password: newPassword,
  });
}

export async function signOut(): Promise<void> {
  await clearTokens();
}

async function cognitoRequest<T = Record<string, never>>(operation: string, payload: object): Promise<T> {
  const response = await fetch(`${cognitoEndpoint}/`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-amz-json-1.1',
      'X-Amz-Target': `AWSCognitoIdentityProviderService.${operation}`,
    },
    body: JSON.stringify(payload),
  });
  const body = await response.json().catch(() => ({})) as T & CognitoErrorBody;
  if (!response.ok) {
    const code = errorCode(body.__type);
    throw new CognitoAuthError(code, localizedError(code));
  }
  return body;
}

function storedTokens(
  result: AuthenticationResult | undefined,
  fallbackRefreshToken?: string,
  fallbackIdToken?: string,
): StoredTokens {
  if (!result?.AccessToken) {
    throw new CognitoAuthError('MissingAuthenticationResult', '로그인 정보를 받지 못했습니다.');
  }
  return {
    accessToken: result.AccessToken,
    refreshToken: result.RefreshToken ?? fallbackRefreshToken,
    idToken: result.IdToken ?? fallbackIdToken,
    expiresAt: Date.now() + (result.ExpiresIn ?? 3600) * 1000,
  };
}

function requireConfiguration() {
  if (!isConfigured()) {
    throw new CognitoAuthError('MissingConfiguration', '로그인 환경설정이 필요합니다.');
  }
}

function isConfigured() {
  return Boolean(cognitoEndpoint && clientId);
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

function errorCode(type: string | undefined) {
  return type?.split(/[#:/]/).filter(Boolean).at(-1) ?? 'CognitoError';
}

function localizedError(code: string) {
  const messages: Record<string, string> = {
    AliasExistsException: '이미 사용 중인 이메일입니다.',
    CodeMismatchException: '인증번호가 올바르지 않습니다.',
    ExpiredCodeException: '인증번호가 만료되었습니다.',
    InvalidParameterException: '입력한 정보를 다시 확인해 주세요.',
    InvalidPasswordException: '비밀번호 조건을 확인해 주세요.',
    LimitExceededException: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
    NotAuthorizedException: '이메일 또는 비밀번호가 올바르지 않습니다.',
    PasswordResetRequiredException: '비밀번호 재설정이 필요합니다.',
    TooManyRequestsException: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
    UserNotConfirmedException: '이메일 인증이 필요합니다.',
    UserNotFoundException: '이메일 또는 비밀번호가 올바르지 않습니다.',
    UsernameExistsException: '이미 가입된 이메일입니다.',
  };
  return messages[code] ?? '인증 요청을 처리하지 못했습니다.';
}
