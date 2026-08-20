import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';

import { api } from '../api';
import type { HouseholdMember, Session } from '../types';
import { Card, colors, PrimaryButton, Screen, SectionHeader } from '../ui';

export function InvitationAcceptScreen({
  session,
  token,
  onDone,
  onCancel,
}: {
  session: Session;
  token: string;
  onDone: (session: Session) => void;
  onCancel: () => void;
}) {
  const [accepted, setAccepted] = useState<{ member: HouseholdMember; session: Session } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function accept() {
    setBusy(true);
    setError(null);
    try {
      const member = await api.acceptHouseholdInvitation(session, token);
      const nextSession = await api.bootstrap();
      setAccepted({ member, session: nextSession });
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen>
      <SectionHeader title="초대" />
      <Card>
        {accepted ? (
          <>
            <Text style={styles.title}>참여 완료</Text>
            <Text style={styles.body}>{accepted.member.email}</Text>
            <PrimaryButton label="우리 집 보기" onPress={() => onDone(accepted.session)} />
          </>
        ) : (
          <>
            <Text style={styles.title}>우리 집에 참여할까요?</Text>
            <Text style={styles.body}>{session.email}</Text>
            {error ? <Text style={styles.error}>{error}</Text> : null}
            <PrimaryButton label={busy ? '확인 중…' : '초대 수락'} onPress={() => void accept()} disabled={busy} />
            <PrimaryButton label="나중에" onPress={onCancel} variant="quiet" disabled={busy} />
          </>
        )}
      </Card>
    </Screen>
  );
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '초대를 처리하지 못했습니다.';
}

const styles = StyleSheet.create({
  title: { color: colors.ink, fontSize: 20, fontWeight: '800' },
  body: { color: colors.muted, fontSize: 14 },
  error: { color: colors.danger, backgroundColor: '#F9E6E2', borderRadius: 12, padding: 12, fontSize: 13 },
});
