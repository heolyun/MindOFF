import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';

import { clearTokens, loadTokens, saveTokens } from './tokenStore';
import type { StoredTokens } from './tokenStore.types';

WebBrowser.maybeCompleteAuthSession();

export const AUTH_MODE = process.env.EXPO_PUBLIC_AUTH_MODE ?? 'dev';
const issuer = process.env.EXPO_PUBLIC_COGNITO_ISSUER_URI ?? '';
const clientId = process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? '';

export async function restoreAccessToken(): Promise<string | null> {
  if (AUTH_MODE !== 'cognito') return null;
  const tokens = await loadTokens();
  if (!tokens) return null;
  if (tokens.expiresAt > Date.now() + 30_000) return tokens.accessToken;
  return refreshAccessToken();
}

export async function refreshAccessToken(): Promise<string | null> {
  if (AUTH_MODE !== 'cognito' || !issuer || !clientId) return null;
  const current = await loadTokens();
  if (!current?.refreshToken) {
    await clearTokens();
    return null;
  }
  try {
    const discovery = await AuthSession.fetchDiscoveryAsync(issuer);
    const token = await AuthSession.refreshAsync({
      clientId,
      refreshToken: current.refreshToken,
    }, discovery);
    const stored: StoredTokens = {
      accessToken: token.accessToken,
      refreshToken: token.refreshToken ?? current.refreshToken,
      idToken: token.idToken ?? current.idToken,
      expiresAt: Date.now() + (token.expiresIn ?? 3600) * 1000,
    };
    await saveTokens(stored);
    return stored.accessToken;
  } catch {
    await clearTokens();
    return null;
  }
}

export async function signInWithCognito(): Promise<string> {
  if (!issuer || !clientId) {
    throw new Error('Cognito 환경설정이 필요합니다.');
  }
  const discovery = await AuthSession.fetchDiscoveryAsync(issuer);
  const redirectUri = AuthSession.makeRedirectUri({ scheme: 'mindoff', path: 'auth' });
  const request = new AuthSession.AuthRequest({
    clientId,
    redirectUri,
    responseType: AuthSession.ResponseType.Code,
    scopes: ['openid', 'email', 'profile'],
    usePKCE: true,
  });
  const result = await request.promptAsync(discovery);
  if (result.type !== 'success' || !result.params.code || !request.codeVerifier) {
    throw new Error(result.type === 'cancel' ? '로그인이 취소되었습니다.' : '로그인에 실패했습니다.');
  }
  const token = await AuthSession.exchangeCodeAsync({
    clientId,
    code: result.params.code,
    redirectUri,
    extraParams: { code_verifier: request.codeVerifier },
  }, discovery);
  const stored: StoredTokens = {
    accessToken: token.accessToken,
    refreshToken: token.refreshToken,
    idToken: token.idToken,
    expiresAt: Date.now() + (token.expiresIn ?? 3600) * 1000,
  };
  await saveTokens(stored);
  return stored.accessToken;
}

export async function signOut(): Promise<void> {
  await clearTokens();
}
