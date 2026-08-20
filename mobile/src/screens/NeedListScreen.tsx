import { useCallback, useEffect, useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';

import { api, today, type CompleteNeedInput } from '../api';
import type { NeedListItem, Session } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function NeedListScreen({ session }: { session: Session }) {
  const [items, setItems] = useState<NeedListItem[]>([]);
  const [name, setName] = useState('');
  const [purchaseUrl, setPurchaseUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [purchaseDraft, setPurchaseDraft] = useState<(CompleteNeedInput & { itemId: string }) | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.getNeeds(session));
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
    if (!name.trim()) return;
    setBusy(true);
    try {
      await api.addNeed(session, name.trim(), purchaseUrl.trim());
      setName('');
      setPurchaseUrl('');
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  async function complete() {
    if (!purchaseDraft || !isValidPurchase(purchaseDraft)) return;
    setBusy(true);
    setError(null);
    try {
      await api.completeNeed(session, purchaseDraft.itemId, purchaseDraft);
      setPurchaseDraft(null);
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  function startPurchase(item: NeedListItem) {
    setPurchaseDraft({
      itemId: item.id,
      name: item.name,
      purchasedAt: today(),
      expiresAt: null,
      purchaseUrl: item.purchaseUrl ?? '',
    });
  }

  function updatePurchase(values: Partial<CompleteNeedInput>) {
    setPurchaseDraft((current) => current ? { ...current, ...values } : current);
  }

  const needed = items.filter((item) => item.status === 'NEEDED');

  return (
    <Screen>
      <SectionHeader title="Need" />
      <Card>
        <Text style={styles.formTitle}>추가</Text>
        <TextField label="품목" value={name} onChangeText={setName} placeholder="예: 종량제봉투" />
        <TextField label="구매 링크" value={purchaseUrl} onChangeText={setPurchaseUrl} placeholder="https://" inputMode="url" />
        <PrimaryButton label="추가" onPress={() => void add()} disabled={busy || !name.trim()} />
      </Card>
      <AsyncState loading={loading} error={error} />
      {!loading && !error && needed.length === 0 && <EmptyState>비어 있어요.</EmptyState>}
      {needed.map((item) => (
        <Card key={item.id}>
          <View style={styles.itemTop}>
            <View style={styles.itemCopy}>
              <Text style={styles.itemName}>{item.name}</Text>
              <Text style={styles.meta}>{sourceLabel(item.sourceType)}</Text>
            </View>
            <Text style={styles.needMark}>NEED</Text>
          </View>
          {item.purchaseUrl && (
            <PrimaryButton label="구매 링크" onPress={() => void Linking.openURL(item.purchaseUrl!)} variant="quiet" />
          )}
          {purchaseDraft?.itemId === item.id ? (
            <View style={styles.purchaseEditor}>
              <TextField label="품목" value={purchaseDraft.name} onChangeText={(name) => updatePurchase({ name })} />
              <TextField label="구매일" value={purchaseDraft.purchasedAt} onChangeText={(purchasedAt) => updatePurchase({ purchasedAt })} placeholder="YYYY-MM-DD" />
              {item.sourceType === 'FRIDGE' ? (
                <TextField label="유통기한" value={purchaseDraft.expiresAt ?? ''} onChangeText={(expiresAt) => updatePurchase({ expiresAt: expiresAt || null })} placeholder="YYYY-MM-DD" />
              ) : null}
              {item.sourceType !== 'FRIDGE' ? (
                <TextField label="구매 링크" value={purchaseDraft.purchaseUrl} onChangeText={(purchaseUrl) => updatePurchase({ purchaseUrl })} placeholder="https://" inputMode="url" />
              ) : null}
              <PrimaryButton label={busy ? '반영 중…' : '반영'} onPress={() => void complete()} disabled={busy || !isValidPurchase(purchaseDraft)} />
              <PrimaryButton label="취소" onPress={() => setPurchaseDraft(null)} variant="quiet" disabled={busy} />
            </View>
          ) : (
            <PrimaryButton label="구매 완료" onPress={() => startPurchase(item)} disabled={busy || purchaseDraft !== null} />
          )}
        </Card>
      ))}
    </Screen>
  );
}

function sourceLabel(source: NeedListItem['sourceType']) {
  if (source === 'FRIDGE') return '냉장고';
  if (source === 'HOUSEHOLD_ITEM') return '생활용품';
  return '직접';
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.';
}

function isValidPurchase(input: CompleteNeedInput) {
  return Boolean(
    input.name.trim()
    && /^\d{4}-\d{2}-\d{2}$/.test(input.purchasedAt)
    && (!input.expiresAt || /^\d{4}-\d{2}-\d{2}$/.test(input.expiresAt)),
  );
}

const styles = StyleSheet.create({
  formTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  itemTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  itemCopy: { gap: 4, flex: 1 },
  itemName: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  meta: { color: colors.muted, fontSize: 13 },
  needMark: { color: '#7A4A08', fontSize: 11, fontWeight: '900', letterSpacing: 1, backgroundColor: '#FBE6C5', padding: 8, borderRadius: 10 },
  purchaseEditor: { borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 12, gap: 10 },
});
