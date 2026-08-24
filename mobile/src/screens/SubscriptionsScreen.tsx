import { useCallback, useEffect, useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { Session, Subscription } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function SubscriptionsScreen({ session }: { session: Session }) {
  const [items, setItems] = useState<Subscription[]>([]);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [billingCycle, setBillingCycle] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [shared, setShared] = useState(false);
  const [nextBillingAt, setNextBillingAt] = useState('');
  const [trialEndAt, setTrialEndAt] = useState('');
  const [managementUrl, setManagementUrl] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deleteCandidateId, setDeleteCandidateId] = useState<string | null>(null);
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

  function resetForm() {
    setName('');
    setAmount('');
    setNextBillingAt('');
    setTrialEndAt('');
    setManagementUrl('');
    setBillingCycle('MONTHLY');
    setShared(false);
    setEditingId(null);
    setDeleteCandidateId(null);
  }

  function startEdit(item: Subscription) {
    setName(item.name);
    setAmount(String(item.amount));
    setBillingCycle(item.billingCycle === 'ANNUAL' ? 'ANNUAL' : 'MONTHLY');
    setNextBillingAt(item.nextBillingAt ?? '');
    setTrialEndAt(item.trialEndAt ?? '');
    setManagementUrl(item.managementUrl ?? '');
    setShared(item.shared);
    setEditingId(item.id);
    setDeleteCandidateId(null);
    setError(null);
  }

  async function save() {
    const parsedAmount = Number(amount.replaceAll(',', ''));
    if (!name.trim() || !Number.isFinite(parsedAmount) || parsedAmount < 0) return;
    const input = {
      name: name.trim(),
      amount: parsedAmount,
      billingCycle,
      nextBillingAt: nextBillingAt.trim() || null,
      trialEndAt: trialEndAt.trim() || null,
      managementUrl: managementUrl.trim(),
      shared,
    };
    setBusy(true);
    setError(null);
    try {
      if (editingId) {
        await api.updateSubscription(session, editingId, input);
      } else {
        await api.addSubscription(session, input);
      }
      resetForm();
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  async function deleteItem(subscriptionId: string) {
    setBusy(true);
    setError(null);
    try {
      await api.deleteSubscription(session, subscriptionId);
      if (editingId === subscriptionId) resetForm();
      setDeleteCandidateId(null);
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  const validForm = name.trim().length > 0 && amount.trim().length > 0;

  return (
    <Screen>
      <SectionHeader title="구독" />
      {editingId === null ? <Card>{renderForm('추가', '추가')}</Card> : null}
      <AsyncState loading={loading} error={error} />
      {!loading && !error && items.length === 0 ? <EmptyState>비어 있어요.</EmptyState> : null}
      {items.map((item) => (
        <Card key={item.id}>
          {editingId === item.id ? (
            renderForm('수정', '저장', resetForm)
          ) : (
            <>
              <View style={styles.itemTop}>
                <View style={styles.itemCopy}>
                  <Text style={styles.itemName}>{item.name}</Text>
                  <Text style={styles.meta}>{item.nextBillingAt ?? '결제일 없음'}</Text>
                </View>
                <View style={styles.amountCopy}>
                  <Text style={styles.amount}>{Number(item.amount).toLocaleString('ko-KR')}원/{item.billingCycle === 'ANNUAL' ? '년' : '월'}</Text>
                  {item.billingCycle === 'ANNUAL' ? <Text style={styles.monthly}>월 {monthlyAmount(item).toLocaleString('ko-KR')}원</Text> : null}
                </View>
              </View>
              {item.shared ? <Text style={styles.shared}>우리 집 공유</Text> : null}
              {item.trialEndAt ? <Text style={styles.trial}>체험 종료 {item.trialEndAt}</Text> : null}
              {item.managementUrl ? (
                <PrimaryButton label="관리" onPress={() => void Linking.openURL(item.managementUrl!)} variant="quiet" />
              ) : null}
              {item.userId === session.userId ? (
                deleteCandidateId === item.id ? (
                  <>
                    <Text style={styles.deleteNotice}>이 구독을 삭제할까요?</Text>
                    <View style={styles.actionRow}>
                      <ActionButton label="취소" onPress={() => setDeleteCandidateId(null)} disabled={busy} />
                      <ActionButton label="삭제하기" onPress={() => void deleteItem(item.id)} disabled={busy} />
                    </View>
                  </>
                ) : (
                  <View style={styles.actionRow}>
                    <ActionButton label="수정" onPress={() => startEdit(item)} disabled={busy} />
                    <ActionButton label="삭제" onPress={() => setDeleteCandidateId(item.id)} disabled={busy} />
                  </View>
                )
              ) : null}
            </>
          )}
        </Card>
      ))}
    </Screen>
  );

  function renderForm(title: string, submitLabel: string, onCancel?: () => void) {
    return (
      <>
        <Text style={styles.formTitle}>{title}</Text>
        <TextField label="서비스명" value={name} onChangeText={setName} placeholder="예: Netflix" editable={!busy} />
        <View style={styles.choiceRow}>
          <ChoiceButton label="월간" selected={billingCycle === 'MONTHLY'} onPress={() => setBillingCycle('MONTHLY')} disabled={busy} />
          <ChoiceButton label="연간" selected={billingCycle === 'ANNUAL'} onPress={() => setBillingCycle('ANNUAL')} disabled={busy} />
        </View>
        <TextField label={billingCycle === 'ANNUAL' ? '연 요금' : '월 요금'} value={amount} onChangeText={setAmount} placeholder="17000" inputMode="numeric" editable={!busy} />
        <TextField label="결제일" value={nextBillingAt} onChangeText={setNextBillingAt} placeholder="YYYY-MM-DD" inputMode="numeric" editable={!busy} />
        <TextField label="체험 종료" value={trialEndAt} onChangeText={setTrialEndAt} placeholder="YYYY-MM-DD" inputMode="numeric" editable={!busy} />
        <TextField label="관리 URL" value={managementUrl} onChangeText={setManagementUrl} placeholder="https://" inputMode="url" editable={!busy} />
        <PrimaryButton
          label={shared ? '✓ 우리 집 공유' : '우리 집 공유'}
          onPress={() => setShared((current) => !current)}
          variant={shared ? 'primary' : 'quiet'}
          disabled={busy}
        />
        <PrimaryButton label={submitLabel} onPress={() => void save()} disabled={busy || !validForm} />
        {onCancel ? <PrimaryButton label="취소" onPress={onCancel} variant="quiet" disabled={busy} /> : null}
      </>
    );
  }
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.';
}

function monthlyAmount(item: Subscription) {
  return Math.round(Number(item.amount) / 12);
}

function ChoiceButton({ label, selected, onPress, disabled }: { label: string; selected: boolean; onPress: () => void; disabled?: boolean }) {
  return (
    <View style={styles.choiceWrap}>
      <PrimaryButton label={selected ? `✓ ${label}` : label} onPress={onPress} variant={selected ? 'primary' : 'quiet'} disabled={disabled} />
    </View>
  );
}

function ActionButton({ label, onPress, disabled }: { label: string; onPress: () => void; disabled?: boolean }) {
  return (
    <View style={styles.actionButton}>
      <PrimaryButton label={label} onPress={onPress} variant="quiet" disabled={disabled} />
    </View>
  );
}

const styles = StyleSheet.create({
  formTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  itemTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  itemCopy: { gap: 4, flex: 1 },
  itemName: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  meta: { color: colors.muted, fontSize: 13 },
  choiceRow: { flexDirection: 'row', gap: 8 },
  choiceWrap: { flex: 1, minWidth: 0 },
  amountCopy: { alignItems: 'flex-end', gap: 3 },
  amount: { color: colors.ink, fontSize: 16, fontWeight: '900' },
  monthly: { color: colors.muted, fontSize: 11 },
  shared: { color: colors.ink, backgroundColor: colors.mint, alignSelf: 'flex-start', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6, fontSize: 11, fontWeight: '800' },
  trial: { color: '#7A4A08', backgroundColor: '#FBE6C5', borderRadius: 12, padding: 10, fontSize: 13, fontWeight: '700' },
  deleteNotice: { color: colors.danger, fontSize: 13, fontWeight: '700', textAlign: 'center' },
  actionRow: { flexDirection: 'row', gap: 8 },
  actionButton: { flex: 1, minWidth: 0 },
});
