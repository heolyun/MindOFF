import { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { HouseholdItem, Session } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function SuppliesScreen({ session }: { session: Session }) {
  const [items, setItems] = useState<HouseholdItem[]>([]);
  const [name, setName] = useState('');
  const [purchaseUrl, setPurchaseUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.getHouseholdItems(session));
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
      await api.addHouseholdItem(session, name.trim(), purchaseUrl.trim());
      setName('');
      setPurchaseUrl('');
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  async function finish(itemId: string) {
    setBusy(true);
    try {
      await api.finishHouseholdItem(session, itemId);
      await load();
    } catch (reason) {
      setError(messageOf(reason));
    } finally {
      setBusy(false);
    }
  }

  const activeItems = items.filter((item) => item.status === 'ACTIVE');

  return (
    <Screen>
      <SectionHeader title="생활용품" />
      <Card>
        <Text style={styles.formTitle}>추가</Text>
        <TextField label="품목명" value={name} onChangeText={setName} placeholder="예: 세제" />
        <TextField
          label="구매 링크"
          value={purchaseUrl}
          onChangeText={setPurchaseUrl}
          placeholder="https://"
          inputMode="url"
        />
        <PrimaryButton label="추가" onPress={() => void add()} disabled={busy || !name.trim()} />
      </Card>
      <AsyncState loading={loading} error={error} />
      {!loading && !error && activeItems.length === 0 && <EmptyState>비어 있어요.</EmptyState>}
      {activeItems.map((item) => (
        <Card key={item.id}>
          <View style={styles.itemTop}>
            <Text style={styles.itemName}>{item.name}</Text>
            <Text style={styles.badge}>사용 중</Text>
          </View>
          <Text style={styles.meta}>{item.purchasedAt}</Text>
          <PrimaryButton label="다 씀" onPress={() => void finish(item.id)} variant="quiet" disabled={busy} />
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
  itemName: { color: colors.ink, fontSize: 18, fontWeight: '800', flex: 1 },
  badge: { color: colors.ink, fontSize: 12, fontWeight: '800', backgroundColor: colors.mint, padding: 7, borderRadius: 10 },
  meta: { color: colors.muted, fontSize: 13 },
});
