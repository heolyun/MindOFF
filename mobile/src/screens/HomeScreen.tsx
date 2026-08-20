import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { api } from '../api';
import type { AttentionItem, HomeSummary, Session } from '../types';
import { AsyncState, Card, colors, Screen, SectionHeader } from '../ui';

type HomeTarget = 'receipts' | 'fridge' | 'supplies' | 'needs' | 'subscriptions';

export function HomeScreen({ session, onNavigate }: { session: Session; onNavigate: (target: HomeTarget) => void }) {
  const [summary, setSummary] = useState<HomeSummary | null>(null);
  const [attention, setAttention] = useState<AttentionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [nextSummary, nextAttention] = await Promise.all([
        api.getHome(session),
        api.getAttention(session),
      ]);
      setSummary(nextSummary);
      setAttention(nextAttention);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '홈 정보를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Screen>
      <SectionHeader title="오늘" />
      <AsyncState loading={loading} error={error} />
      {summary && (
        <>
          <View style={styles.metricGrid}>
            <Metric label="확인" value={`${summary.attentionCount}건`} tone="accent" onPress={() => onNavigate('fridge')} />
            <Metric label="구매" value={`${summary.needListCount}개`} tone="mint" onPress={() => onNavigate('needs')} />
            <Metric label="기록된 고정비" value={won(summary.recordedFixedLivingCost)} onPress={() => onNavigate('subscriptions')} />
            <Metric label="이번 달 기록" value={won(summary.receiptPurchaseTotal)} onPress={() => onNavigate('receipts')} />
          </View>
          {attention.length > 0 && (
            <Card>
              <Text style={styles.cardTitle}>확인</Text>
              {attention.slice(0, 3).map((item) => (
                <Pressable
                  key={`${item.type}-${item.sourceId}`}
                  accessibilityRole="button"
                  onPress={() => onNavigate(targetForAttention(item.type))}
                  style={({ pressed }) => [styles.attentionRow, pressed && styles.pressed]}
                >
                  <View style={styles.attentionDot} />
                  <View style={styles.attentionCopy}>
                    <Text style={styles.attentionTitle}>{item.title}</Text>
                    <Text style={styles.attentionBody}>{item.message} · {item.dueAt}</Text>
                  </View>
                </Pressable>
              ))}
            </Card>
          )}
        </>
      )}
    </Screen>
  );
}

function Metric({
  label,
  value,
  tone = 'plain',
  onPress,
}: {
  label: string;
  value: string;
  tone?: 'plain' | 'accent' | 'mint';
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.metric, tone === 'accent' && styles.metricAccent, tone === 'mint' && styles.metricMint, pressed && styles.pressed]}
    >
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={styles.metricValue}>{value}</Text>
    </Pressable>
  );
}

function targetForAttention(type: AttentionItem['type']): HomeTarget {
  if (type === 'USAGE_PREDICTION') return 'supplies';
  if (type === 'TRIAL_END') return 'subscriptions';
  return 'fridge';
}

function won(value: number) {
  return `${Math.round(value).toLocaleString('ko-KR')}원`;
}

const styles = StyleSheet.create({
  metricGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  metric: {
    width: '47.8%',
    minHeight: 126,
    borderRadius: 22,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 16,
    justifyContent: 'space-between',
  },
  metricAccent: { backgroundColor: '#FBE6C5', borderColor: '#F6D39B' },
  metricMint: { backgroundColor: colors.mint, borderColor: '#C6E3D8' },
  metricLabel: { color: colors.muted, fontSize: 13, lineHeight: 18, fontWeight: '700' },
  metricValue: { color: colors.ink, fontSize: 24, fontWeight: '900', letterSpacing: -0.6 },
  cardTitle: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  attentionRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 10, borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 12 },
  attentionDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.accent, marginTop: 5 },
  attentionCopy: { flex: 1, gap: 3 },
  attentionTitle: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  attentionBody: { color: colors.muted, fontSize: 12, lineHeight: 18 },
  pressed: { opacity: 0.72 },
});
