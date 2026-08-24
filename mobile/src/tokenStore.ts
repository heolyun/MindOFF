import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { StoredTokens } from './tokenStore.types';

const key = 'mindoff-auth-v1';
let memoryTokens: StoredTokens | null = null;

export async function loadTokens(): Promise<StoredTokens | null> {
  if (Platform.OS !== 'web' && memoryTokens) return memoryTokens;

  const value = Platform.OS === 'web'
    ? globalThis.sessionStorage?.getItem(key) ?? globalThis.localStorage?.getItem(key) ?? null
    : await SecureStore.getItemAsync(key);
  return value ? JSON.parse(value) as StoredTokens : null;
}

export async function saveTokens(tokens: StoredTokens): Promise<void> {
  const value = JSON.stringify(tokens);
  if (Platform.OS === 'web') {
    globalThis.sessionStorage?.removeItem(key);
    globalThis.localStorage?.removeItem(key);
    const storage = tokens.rememberLogin ? globalThis.localStorage : globalThis.sessionStorage;
    storage?.setItem(key, value);
  } else if (tokens.rememberLogin) {
    memoryTokens = tokens;
    await SecureStore.setItemAsync(key, value);
  } else {
    memoryTokens = tokens;
    await SecureStore.deleteItemAsync(key);
  }
}

export async function clearTokens(): Promise<void> {
  memoryTokens = null;
  if (Platform.OS === 'web') {
    globalThis.sessionStorage?.removeItem(key);
    globalThis.localStorage?.removeItem(key);
  } else {
    await SecureStore.deleteItemAsync(key);
  }
}
