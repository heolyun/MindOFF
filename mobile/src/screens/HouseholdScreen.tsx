import { useCallback, useEffect, useState } from 'react';
import { Platform, Share, StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { HouseholdDetails, HouseholdInvitation, Session } from '../types';
import { AsyncState, Card, colors, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

const inviteBaseUrl = 'https://mindoff-project-preview.vercel.app/?invite=';

export function HouseholdScreen({ session }: { session: Session }) {
  const [household, setHousehold] = useState<HouseholdDetails | null>(null);
  const [invitations, setInvitations] = useState<HouseholdInvitation[]>([]);
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [nextHousehold, nextInvitations] = await Promise.all([
        api.getHousehold(session),
        api.getHouseholdInvitations(session),
      ]);
      setHousehold(nextHousehold);
      setInvitations(nextInvitations);
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  async function invite() {
    const normalizedEmail = email.trim().toLowerCase();
    if (!isEmail(normalizedEmail)) {
      setError('이메일을 확인해 주세요.');
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const invitation = await api.createHouseholdInvitation(session, normalizedEmail);
      setInvitations((current) => [
        invitation,
        ...current.filter((item) => item.id !== invitation.id),
      ]);
      setEmail('');
      setNotice('초대 링크를 만들었어요.');
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  async function share(invitation: HouseholdInvitation) {
    const link = `${inviteBaseUrl}${encodeURIComponent(invitation.token)}`;
    setNotice(null);
    try {
      if (Platform.OS === 'web' && globalThis.navigator?.clipboard) {
        await globalThis.navigator.clipboard.writeText(link);
        setNotice('링크를 복사했어요.');
        return;
      }
      await Share.share({ message: `MindOFF 초대\n${link}` });
    } catch (reason) {
      setError(messageOf(reason));
    }
  }

  return (
    <Screen>
      <SectionHeader title="우리 집" />
      <AsyncState loading={loading} error={error} />
      {household ? (
        <Card>
          <Text style={styles.cardTitle}>구성원</Text>
          {household.members.map((member) => (
            <View key={member.userId} style={styles.memberRow}>
              <View style={styles.memberCopy}>
                <Text style={styles.memberName}>{member.name}</Text>
                <Text style={styles.meta}>{member.email}</Text>
              </View>
              <Text style={styles.role}>{member.role === 'OWNER' ? '관리자' : '구성원'}</Text>
            </View>
          ))}
        </Card>
      ) : null}

      <Card>
        <Text style={styles.cardTitle}>초대</Text>
        <TextField
          label="이메일"
          value={email}
          onChangeText={setEmail}
          placeholder="name@example.com"
          inputMode="email"
          autoComplete="email"
        />
        <PrimaryButton label="링크 만들기" onPress={() => void invite()} disabled={busy || !email.trim()} />
        {notice ? <Text style={styles.notice}>{notice}</Text> : null}
      </Card>

      {invitations.map((invitation) => (
        <Card key={invitation.id}>
          <View style={styles.invitationTop}>
            <View style={styles.memberCopy}>
              <Text style={styles.memberName}>{invitation.email}</Text>
              <Text style={styles.meta}>{statusLabel(invitation.status)} · {invitation.expiresAt.slice(0, 10)}</Text>
            </View>
            <Text style={[styles.status, invitation.status !== 'PENDING' && styles.statusQuiet]}>
              {statusLabel(invitation.status)}
            </Text>
          </View>
          {invitation.status === 'PENDING' ? (
            <PrimaryButton
              label={Platform.OS === 'web' ? '링크 복사' : '공유'}
              onPress={() => void share(invitation)}
              variant="quiet"
            />
          ) : null}
        </Card>
      ))}
    </Screen>
  );
}

function statusLabel(status: HouseholdInvitation['status']) {
  if (status === 'ACCEPTED') return '완료';
  if (status === 'EXPIRED') return '만료';
  return '대기';
}

function isEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.';
}

const styles = StyleSheet.create({
  cardTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    paddingTop: 12,
  },
  memberCopy: { flex: 1, minWidth: 0, gap: 3 },
  memberName: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  meta: { color: colors.muted, fontSize: 12 },
  role: { color: colors.ink, backgroundColor: colors.mint, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6, fontSize: 11, fontWeight: '800' },
  invitationTop: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  status: { color: '#7A4A08', backgroundColor: '#FBE6C5', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6, fontSize: 11, fontWeight: '800' },
  statusQuiet: { color: colors.muted, backgroundColor: colors.mint },
  notice: { color: colors.ink, backgroundColor: colors.mint, borderRadius: 12, padding: 10, fontSize: 12, fontWeight: '700', textAlign: 'center' },
});
