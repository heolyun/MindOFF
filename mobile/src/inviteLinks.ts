import * as Linking from 'expo-linking';
import { Platform } from 'react-native';

export function inviteTokenFromUrl(url: string | null): string | null {
  if (!url) return null;
  const value = Linking.parse(url).queryParams?.invite;
  const token = Array.isArray(value) ? value[0] : value;
  if (typeof token !== 'string') return null;
  const normalized = token.trim();
  return normalized.length > 0 && normalized.length <= 100 ? normalized : null;
}

export function clearInviteFromWebUrl() {
  if (Platform.OS !== 'web' || !globalThis.location || !globalThis.history) return;
  const url = new URL(globalThis.location.href);
  url.searchParams.delete('invite');
  globalThis.history.replaceState(null, '', url.toString());
}
