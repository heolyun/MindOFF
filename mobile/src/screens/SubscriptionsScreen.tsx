import { useCallback, useEffect, useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { Session, Subscription } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function SubscriptionsScreen({ session }: { session: Session }) {
  const [items, setItems] = useState<Subscription[]>([]);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [nextBillingAt, setNextBillingAt] = useState('');
  const [trialEndAt, setTrialEndAt] = useState('');
  const [managementUrl, setManagementUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.getSubscriptions(session));
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  async function add() {
    const parsedAmount = Number(amount.replaceAll(',', ''));
    if (!name.trim() || !Number.isFinite(parsedAmount) || parsedAmount < 0) return;
    setBusy(true);
    try {
      await api.addSubscription(session, {
        name: name.trim(),
        amount: parsedAmount,
        nextBillingAt: nextBillingAt.trim() || null,
        trialEndAt: trialEndAt.trim() || null,
        managementUrl: managementUrl.trim(),
      });
      setName('');
      setAmount('');
      setNextBillingAt('');
      setTrialEndAt('');
      setManagementUrl('');
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen>
      <SectionHeader title="구독" />
      <Card>
        <Text style={styles.formTitle}>추가</Text>
        <TextField label="서비스명" value={name} onChangeText={setName} placeholder="예: Netflix" />
        <TextField label="월 요금" value={amount} onChangeText={setAmount} placeholder="17000" inputMode="numeric" />
        <TextField label="결제일" value={nextBillingAt} onChangeText={setNextBillingAt} placeholder="YYYY-MM-DD" inputMode="numeric" />
        <TextField label="체험 종료" value={trialEndAt} onChangeText={setTrialEndAt} placeholder="YYYY-MM-DD" inputMode="numeric" />
        <TextField label="관리 URL" value={managementUrl} onChangeText={setManagementUrl} placeholder="https://" inputMode="url" />
        <PrimaryButton label="추가" onPress={() => void add()} disabled={busy || !name.trim() || !amount.trim()} />
      </Card>
      <AsyncState loading={loading} error={error} />
      {!loading && !error && items.length === 0 && <EmptyState>비어 있어요.</EmptyState>}
      {items.map((item) => (
        <Card key={item.id}>
          <View style={styles.itemTop}>
            <View style={styles.itemCopy}>
              <Text style={styles.itemName}>{item.name}</Text>
              <Text style={styles.meta}>{item.nextBillingAt ?? '결제일 없음'}</Text>
            </View>
            <Text style={styles.amount}>{Number(item.amount).toLocaleString('ko-KR')}원</Text>
          </View>
          {item.trialEndAt && <Text style={styles.trial}>체험 종료 {item.trialEndAt}</Text>}
          {item.managementUrl && (
            <PrimaryButton label="관리" onPress={() => void Linking.openURL(item.managementUrl!)} variant="quiet" />
          )}
        </Card>
      ))}
    </Screen>
  );
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.';
}

const styles = StyleSheet.create({
  formTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  itemTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  itemCopy: { gap: 4, flex: 1 },
  itemName: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  meta: { color: colors.muted, fontSize: 13 },
  amount: { color: colors.ink, fontSize: 16, fontWeight: '900' },
  trial: { color: '#7A4A08', backgroundColor: '#FBE6C5', borderRadius: 12, padding: 10, fontSize: 13, fontWeight: '700' },
});
