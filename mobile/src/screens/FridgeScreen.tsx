import { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { FridgeItem, Session } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function FridgeScreen({ session }: { session: Session }) {
  const [items, setItems] = useState<FridgeItem[]>([]);
  const [name, setName] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.getFridge(session));
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
    setError(null);
    try {
      await api.addFridge(session, name.trim(), expiresAt.trim() || null);
      setName('');
      setExpiresAt('');
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
      await api.finishFridge(session, itemId);
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
      <SectionHeader title="냉장고" />
      <Card>
        <Text style={styles.formTitle}>추가</Text>
        <TextField label="품목명" value={name} onChangeText={setName} placeholder="예: 우유" />
        <TextField
          label="유통기한"
          value={expiresAt}
          onChangeText={setExpiresAt}
          placeholder="YYYY-MM-DD"
          inputMode="numeric"
        />
        <PrimaryButton label="추가" onPress={() => void add()} disabled={busy || !name.trim()} />
      </Card>
      <AsyncState loading={loading} error={error} />
      {!loading && !error && activeItems.length === 0 && <EmptyState>비어 있어요.</EmptyState>}
      {activeItems.map((item) => (
        <Card key={item.id}>
          <View style={styles.itemTop}>
            <Text style={styles.itemName}>{item.name}</Text>
            <Text style={[styles.badge, isSoon(item.expiresAt) && styles.badgeSoon]}>
              {item.expiresAt ? `~ ${item.expiresAt}` : '기한 미입력'}
            </Text>
          </View>
          <Text style={styles.meta}>{item.purchasedAt}</Text>
          <PrimaryButton label="다 먹음" onPress={() => void finish(item.id)} variant="quiet" disabled={busy} />
        </Card>
      ))}
    </Screen>
  );
}

function isSoon(date: string | null) {
  if (!date) return false;
  const limit = new Date();
  limit.setDate(limit.getDate() + 2);
  return date <= limit.toISOString().slice(0, 10);
}

function messageOf(reason: unknown) {
  return reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.';
}

const styles = StyleSheet.create({
  formTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  itemTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  itemName: { color: colors.ink, fontSize: 18, fontWeight: '800', flex: 1 },
  badge: { color: colors.muted, fontSize: 12, fontWeight: '700', backgroundColor: '#EDF1EF', padding: 7, borderRadius: 10 },
  badgeSoon: { color: '#7A4A08', backgroundColor: '#FBE6C5' },
  meta: { color: colors.muted, fontSize: 13 },
});
