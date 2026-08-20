import { StatusBar } from 'expo-status-bar';
import * as Linking from 'expo-linking';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { API_BASE_URL, api, setAccessToken, setAccessTokenRefresher } from './src/api';
import { AUTH_MODE, refreshAccessToken, restoreAccessToken, signInWithCognito, signOut } from './src/auth';
import { clearInviteFromWebUrl, inviteTokenFromUrl } from './src/inviteLinks';
import { FridgeScreen } from './src/screens/FridgeScreen';
import { HomeScreen } from './src/screens/HomeScreen';
import { HouseholdScreen } from './src/screens/HouseholdScreen';
import { InvitationAcceptScreen } from './src/screens/InvitationAcceptScreen';
import { NeedListScreen } from './src/screens/NeedListScreen';
import { ReceiptsScreen } from './src/screens/ReceiptsScreen';
import { SubscriptionsScreen } from './src/screens/SubscriptionsScreen';
import { SuppliesScreen } from './src/screens/SuppliesScreen';
import type { Session } from './src/types';
import { colors } from './src/ui';

type Tab = 'home' | 'household' | 'receipts' | 'fridge' | 'supplies' | 'needs' | 'subscriptions';

const tabs: { key: Tab; label: string; mark: string }[] = [
  { key: 'home', label: '홈', mark: '01' },
  { key: 'receipts', label: '영수증', mark: '02' },
  { key: 'fridge', label: '냉장고', mark: '03' },
  { key: 'supplies', label: '생활용품', mark: '04' },
  { key: 'needs', label: 'Need', mark: '05' },
  { key: 'subscriptions', label: '구독', mark: '06' },
];

export default function App() {
  const linkingUrl = Linking.useLinkingURL();
  const linkedInviteToken = inviteTokenFromUrl(linkingUrl);
  const [session, setSession] = useState<Session | null>(null);
  const [tab, setTab] = useState<Tab>('home');
  const [error, setError] = useState<string | null>(null);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [signingIn, setSigningIn] = useState(false);
  const [dismissedInviteToken, setDismissedInviteToken] = useState<string | null>(null);
  const activeInviteToken = linkedInviteToken === dismissedInviteToken ? null : linkedInviteToken;

  useEffect(() => {
    let active = true;
    setAccessTokenRefresher(AUTH_MODE === 'cognito' ? refreshAccessToken : null);
    async function initialize() {
      try {
        if (AUTH_MODE === 'cognito') {
          const token = await restoreAccessToken();
          if (!token) {
            if (active) setNeedsLogin(true);
            return;
          }
          setAccessToken(token);
        }
        const nextSession = await api.bootstrap();
        if (active) setSession(nextSession);
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : 'API에 연결하지 못했습니다.');
      }
    }
    void initialize();
    return () => {
      active = false;
      setAccessTokenRefresher(null);
    };
  }, []);

  async function login() {
    setSigningIn(true);
    setError(null);
    try {
      const token = await signInWithCognito();
      setAccessToken(token);
      setSession(await api.bootstrap());
      setNeedsLogin(false);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '로그인에 실패했습니다.');
    } finally {
      setSigningIn(false);
    }
  }

  async function logout() {
    await signOut();
    setAccessToken(null);
    setSession(null);
    setNeedsLogin(true);
    setTab('home');
  }

  function leaveInvitation(nextTab: Tab) {
    if (activeInviteToken) setDismissedInviteToken(activeInviteToken);
    clearInviteFromWebUrl();
    setTab(nextTab);
  }

  if (!session) {
    return (
      <View style={styles.loadingPage}>
        <StatusBar style="dark" />
        <View style={styles.wordmarkRow}>
          <Text style={styles.wordmark}>MindOFF</Text>
          <View style={styles.dot} />
        </View>
        {needsLogin ? (
          <View style={styles.connectionCard}>
            <Text style={styles.connectionTitle}>로그인</Text>
            {error ? <Text style={styles.connectionBody}>{error}</Text> : null}
            <Pressable accessibilityRole="button" onPress={() => void login()} disabled={signingIn} style={styles.loginButton}>
              <Text style={styles.loginButtonText}>{signingIn ? '연결 중…' : 'MindOFF 시작'}</Text>
            </Pressable>
          </View>
        ) : error ? (
          <View style={styles.connectionCard}>
            <Text style={styles.connectionTitle}>백엔드 연결이 필요해요</Text>
            <Text style={styles.connectionBody}>{error}</Text>
            <Text style={styles.connectionHint}>현재 API: {API_BASE_URL}</Text>
          </View>
        ) : (
          <View style={styles.connecting}>
            <ActivityIndicator color={colors.ink} />
            <Text style={styles.connectionBody}>내가 기억하지 않아도 되는 생활을 준비 중입니다.</Text>
          </View>
        )}
      </View>
    );
  }

  return (
    <View style={styles.page}>
      <StatusBar style="dark" />
      <View style={styles.topBar}>
        <Text style={styles.logo}>MindOFF</Text>
        <View style={styles.topActions}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="우리 집 관리"
            onPress={() => leaveInvitation('household')}
            style={({ pressed }) => [
              styles.householdPill,
              tab === 'household' && styles.householdPillSelected,
              pressed && styles.householdPillPressed,
            ]}
          >
            <View style={styles.onlineDot} />
            <Text style={styles.householdText}>{session.householdName}</Text>
          </Pressable>
          {AUTH_MODE === 'cognito' && (
            <Pressable accessibilityRole="button" onPress={() => void logout()} style={styles.logoutButton}>
              <Text style={styles.logoutText}>로그아웃</Text>
            </Pressable>
          )}
        </View>
      </View>

      <View style={styles.content}>
        {activeInviteToken ? (
          <InvitationAcceptScreen
            session={session}
            token={activeInviteToken}
            onDone={(nextSession) => {
              setSession(nextSession);
              leaveInvitation('household');
            }}
            onCancel={() => leaveInvitation('home')}
          />
        ) : (
          <>
            {tab === 'home' && <HomeScreen session={session} />}
            {tab === 'household' && <HouseholdScreen session={session} />}
            {tab === 'receipts' && <ReceiptsScreen session={session} />}
            {tab === 'fridge' && <FridgeScreen session={session} />}
            {tab === 'supplies' && <SuppliesScreen session={session} />}
            {tab === 'needs' && <NeedListScreen session={session} />}
            {tab === 'subscriptions' && <SubscriptionsScreen session={session} />}
          </>
        )}
      </View>

      {!activeInviteToken ? <View style={styles.tabBar}>
        {tabs.map((item) => {
          const selected = tab === item.key;
          return (
            <Pressable
              key={item.key}
              accessibilityRole="tab"
              accessibilityState={{ selected }}
              onPress={() => leaveInvitation(item.key)}
              style={({ pressed }) => [styles.tab, selected && styles.tabSelected, pressed && styles.tabPressed]}
            >
              <Text style={[styles.tabMark, selected && styles.tabTextSelected]}>{item.mark}</Text>
              <Text numberOfLines={1} style={[styles.tabLabel, selected && styles.tabTextSelected]}>{item.label}</Text>
            </Pressable>
          );
        })}
      </View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background, paddingTop: 42 },
  content: { flex: 1 },
  topBar: {
    minHeight: 58,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: '#E4E1D9',
  },
  logo: { color: colors.ink, fontSize: 20, fontWeight: '900', letterSpacing: -0.5 },
  topActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  householdPill: { flexDirection: 'row', alignItems: 'center', gap: 7, backgroundColor: colors.surface, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999 },
  householdPillSelected: { backgroundColor: colors.mint },
  householdPillPressed: { opacity: 0.72 },
  onlineDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: '#54A883' },
  householdText: { color: colors.ink, fontSize: 12, fontWeight: '700' },
  logoutButton: { paddingHorizontal: 10, paddingVertical: 8 },
  logoutText: { color: colors.muted, fontSize: 11, fontWeight: '700' },
  tabBar: {
    position: 'absolute',
    left: 12,
    right: 12,
    bottom: 12,
    minHeight: 72,
    flexDirection: 'row',
    alignItems: 'stretch',
    borderRadius: 22,
    backgroundColor: '#173B34',
    padding: 7,
    shadowColor: '#000000',
    shadowOpacity: 0.16,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    elevation: 8,
  },
  tab: { flex: 1, borderRadius: 16, alignItems: 'center', justifyContent: 'center', gap: 3, paddingHorizontal: 2 },
  tabSelected: { backgroundColor: '#F4F1EA' },
  tabPressed: { opacity: 0.75 },
  tabMark: { color: '#9AB1AA', fontSize: 9, fontWeight: '800', letterSpacing: 1 },
  tabLabel: { color: '#DCE8E4', fontSize: 11, fontWeight: '700' },
  tabTextSelected: { color: colors.ink },
  loadingPage: { flex: 1, backgroundColor: colors.background, padding: 28, justifyContent: 'center', gap: 28 },
  wordmarkRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  wordmark: { color: colors.ink, fontSize: 42, fontWeight: '900', letterSpacing: -1.6 },
  dot: { width: 14, height: 14, borderRadius: 7, backgroundColor: colors.accent, marginTop: 7 },
  connecting: { borderRadius: 22, backgroundColor: colors.mint, padding: 22, alignItems: 'center', gap: 14 },
  connectionCard: { borderRadius: 22, backgroundColor: '#F9E6E2', padding: 22, gap: 10 },
  connectionTitle: { color: colors.danger, fontSize: 18, fontWeight: '800' },
  connectionBody: { color: colors.muted, fontSize: 14, lineHeight: 21, textAlign: 'center' },
  connectionHint: { color: colors.danger, fontSize: 12, fontWeight: '700' },
  loginButton: { minHeight: 48, borderRadius: 14, backgroundColor: colors.ink, alignItems: 'center', justifyContent: 'center' },
  loginButtonText: { color: '#FFFFFF', fontSize: 14, fontWeight: '800' },
});
