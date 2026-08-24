export type StoredTokens = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  expiresAt: number;
  rememberLogin?: boolean;
};
