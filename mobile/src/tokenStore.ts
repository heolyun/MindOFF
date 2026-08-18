import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { StoredTokens } from './tokenStore.types';

const key = 'mindoff-auth-v1';

export async function loadTokens(): Promise<StoredTokens | null> {
  const value = Platform.OS === 'web'
    ? globalThis.sessionStorage?.getItem(key) ?? null
    : await SecureStore.getItemAsync(key);
  return value ? JSON.parse(value) as StoredTokens : null;
}

export async function saveTokens(tokens: StoredTokens): Promise<void> {
  const value = JSON.stringify(tokens);
  if (Platform.OS === 'web') {
    globalThis.sessionStorage?.setItem(key, value);
  } else {
    await SecureStore.setItemAsync(key, value);
  }
}

export async function clearTokens(): Promise<void> {
  if (Platform.OS === 'web') {
    globalThis.sessionStorage?.removeItem(key);
  } else {
    await SecureStore.deleteItemAsync(key);
  }
}
