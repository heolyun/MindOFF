import * as ImagePicker from 'expo-image-picker';
import { useCallback, useEffect, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { api } from '../api';
import type { ReceiptLine, ReceiptView, Session } from '../types';
import { AsyncState, Card, colors, EmptyState, PrimaryButton, Screen, SectionHeader, TextField } from '../ui';

export function ReceiptsScreen({ session }: { session: Session }) {
  const [receipts, setReceipts] = useState<ReceiptView[]>([]);
  const [draft, setDraft] = useState<ReceiptView | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await api.getReceipts(session);
      setReceipts(next);
      setDraft(next.find((item) => item.receipt.status === 'DRAFT') ?? null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '영수증 기록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  async function pickReceipt(source: 'camera' | 'library') {
    setUploading(true);
    setError(null);
    try {
      if (source === 'camera' && Platform.OS !== 'web') {
        const permission = await ImagePicker.requestCameraPermissionsAsync();
        if (!permission.granted) {
          setError('카메라 권한이 필요합니다.');
          return;
        }
      }
      const options: ImagePicker.ImagePickerOptions = {
        mediaTypes: ['images'],
        quality: 0.8,
      };
      const result = source === 'camera'
        ? await ImagePicker.launchCameraAsync(options)
        : await ImagePicker.launchImageLibraryAsync(options);
      if (result.canceled || !result.assets[0]) return;
      const asset = result.assets[0];
      const next = await api.intakeReceipt(session, {
        uri: asset.uri,
        fileName: asset.fileName ?? `receipt-${Date.now()}.jpg`,
        mimeType: asset.mimeType ?? 'image/jpeg',
        file: asset.file,
      });
      setDraft(next);
      setReceipts((current) => [next, ...current.filter((item) => item.receipt.id !== next.receipt.id)]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '영수증을 분석하지 못했습니다.');
    } finally {
      setUploading(false);
    }
  }

  async function confirmDraft() {
    if (!draft) return;
    setUploading(true);
    setError(null);
    try {
      const confirmed = await api.confirmReceipt(session, draft.receipt.id, {
        merchantName: '영수증',
        purchasedAt: uploadDate(draft.receipt.createdAt),
        totalAmount: draftTotal(draft),
        lines: draft.lines.map((line) => ({
          name: line.name,
          quantity: Number(line.quantity),
          unitPrice: Number(line.unitPrice),
          lineTotal: Number(line.lineTotal),
          targetType: line.targetType,
          expiresAt: line.targetType === 'FRIDGE' ? expiryToIso(line.expiresAt) : null,
        })),
      });
      setReceipts((current) => current.map((item) => item.receipt.id === confirmed.receipt.id ? confirmed : item));
      setDraft(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '영수증을 확정하지 못했습니다.');
    } finally {
      setUploading(false);
    }
  }

  function updateLine(index: number, values: Partial<Pick<ReceiptLine, 'name' | 'quantity' | 'unitPrice' | 'lineTotal' | 'targetType' | 'expiresAt'>>) {
    setDraft((current) => current ? {
      ...current,
      lines: current.lines.map((line, lineIndex) => lineIndex === index ? { ...line, ...values } : line),
    } : current);
  }

  function updateLineNumber(index: number, key: 'quantity' | 'unitPrice' | 'lineTotal', value: string) {
    const amount = numberFrom(value);
    setDraft((current) => current ? {
      ...current,
      lines: current.lines.map((line, lineIndex) => {
        if (lineIndex !== index) return line;
        const next = { ...line, [key]: amount };
        if (key !== 'lineTotal') next.lineTotal = roundMoney(next.quantity * next.unitPrice);
        return next;
      }),
    } : current);
  }

  const confirmedReceipts = receipts.filter((item) => item.receipt.status === 'CONFIRMED');

  return (
    <Screen>
      <SectionHeader title="영수증" />
      <Card>
        <View style={styles.pickerRow}>
          <View style={styles.pickerButton}>
            <PrimaryButton label={uploading ? '분석 중…' : '촬영'} onPress={() => void pickReceipt('camera')} disabled={uploading} />
          </View>
          <View style={styles.pickerButton}>
            <PrimaryButton label="사진 선택" onPress={() => void pickReceipt('library')} disabled={uploading} variant="quiet" />
          </View>
        </View>
      </Card>

      <AsyncState loading={loading} error={error} />

      {draft && (
        <Card>
          <View style={styles.titleRow}>
            <View style={styles.draftBadge}><Text style={styles.draftBadgeText}>OCR 검토</Text></View>
            <Text style={styles.fileName}>{draft.receipt.imageName}</Text>
          </View>
          <Text style={styles.draftSummary}>{draft.lines.length}개 · {won(draftTotal(draft))}</Text>
          {draft.lines.map((line, index) => (
            <View key={line.id} style={styles.lineEditor}>
              <TextField label={`품목 ${index + 1}`} value={line.name} onChangeText={(name) => updateLine(index, { name })} />
              <View style={styles.numberRow}>
                <View style={styles.numberField}><TextField label="수량" value={String(line.quantity)} onChangeText={(value) => updateLineNumber(index, 'quantity', value)} keyboardType="decimal-pad" /></View>
                <View style={styles.numberField}><TextField label="단가" value={String(line.unitPrice)} onChangeText={(value) => updateLineNumber(index, 'unitPrice', value)} keyboardType="decimal-pad" /></View>
                <View style={styles.numberField}><TextField label="합계" value={String(line.lineTotal)} onChangeText={(value) => updateLineNumber(index, 'lineTotal', value)} keyboardType="decimal-pad" /></View>
              </View>
              <View style={styles.targetRow}>
                <TargetButton label="냉장고" selected={line.targetType === 'FRIDGE'} onPress={() => updateLine(index, { targetType: 'FRIDGE' })} />
                <TargetButton label="생활용품" selected={line.targetType === 'HOUSEHOLD_ITEM'} onPress={() => updateLine(index, { targetType: 'HOUSEHOLD_ITEM', expiresAt: null })} />
                <TargetButton label="제외" selected={line.targetType === 'IGNORE'} onPress={() => updateLine(index, { targetType: 'IGNORE', expiresAt: null })} />
              </View>
              {line.targetType === 'FRIDGE' ? (
                <ExpiryDateField
                  value={line.expiresAt}
                  onChange={(expiresAt) => updateLine(index, { expiresAt })}
                />
              ) : null}
            </View>
          ))}
          <PrimaryButton
            label={uploading ? '반영 중…' : '기록 반영'}
            onPress={() => void confirmDraft()}
            disabled={uploading || !isValidDraft(draft)}
          />
        </Card>
      )}

      <Text style={styles.listTitle}>구매 기록</Text>
      {!loading && confirmedReceipts.length === 0 && <EmptyState>비어 있어요.</EmptyState>}
      {confirmedReceipts.map(({ receipt, lines }) => (
        <Card key={receipt.id}>
          <View style={styles.titleRow}>
            <Text style={styles.receiptTitle}>품목 {lines.length}개</Text>
            <Text style={styles.total}>{won(receipt.totalAmount)}</Text>
          </View>
          {lines.length > 0 && <Text style={styles.cardBody}>{lines.map((line) => line.name).join(' · ')}</Text>}
        </Card>
      ))}
    </Screen>
  );
}

function ExpiryDateField({ value, onChange }: { value: string | null; onChange: (value: string | null) => void }) {
  const parts = expiryParts(value);

  function updatePart(index: number, nextValue: string) {
    const nextParts = [...parts];
    nextParts[index] = nextValue.replace(/\D/g, '').slice(0, 2);
    onChange(nextParts.every((part) => !part) ? null : nextParts.join('/'));
  }

  return (
    <View style={styles.expiryField}>
      <Text style={styles.expiryLabel}>유통기한</Text>
      <View style={styles.expiryRow}>
        {['YY', 'MM', 'DD'].map((placeholder, index) => (
          <View key={placeholder} style={styles.expiryPartRow}>
            {index > 0 ? <Text style={styles.expirySeparator}>/</Text> : null}
            <TextInput
              accessibilityLabel={`유통기한 ${placeholder}`}
              autoCapitalize="none"
              inputMode="numeric"
              keyboardType="number-pad"
              maxLength={2}
              onChangeText={(nextValue) => updatePart(index, nextValue)}
              placeholder={placeholder}
              placeholderTextColor="#9AA7A2"
              selectTextOnFocus
              style={styles.expiryInput}
              value={parts[index]}
            />
          </View>
        ))}
      </View>
    </View>
  );
}

function TargetButton({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[styles.target, selected && styles.targetSelected]}>
      <Text style={[styles.targetText, selected && styles.targetTextSelected]}>{label}</Text>
    </Pressable>
  );
}

function won(value: number) {
  return `${Math.round(Number(value)).toLocaleString('ko-KR')}원`;
}

function numberFrom(value: string) {
  const number = Number(value.replace(/[^0-9.]/g, ''));
  return Number.isFinite(number) ? number : 0;
}

function roundMoney(value: number) {
  return Math.round(value * 100) / 100;
}

function draftTotal(draft: ReceiptView) {
  return roundMoney(draft.lines.reduce((total, line) => total + Number(line.lineTotal || 0), 0));
}

function uploadDate(createdAt: string) {
  const date = /^\d{4}-\d{2}-\d{2}/.exec(createdAt)?.[0];
  return date ?? new Date().toISOString().slice(0, 10);
}

function expiryParts(value: string | null) {
  if (!value) return ['', '', ''];
  const iso = /^20(\d{2})-(\d{2})-(\d{2})$/.exec(value);
  if (iso) return [iso[1], iso[2], iso[3]];
  const short = value.split('/');
  return [short[0] ?? '', short[1] ?? '', short[2] ?? ''];
}

function expiryToIso(value: string | null) {
  if (!value) return null;
  const parts = expiryParts(value);
  if (!parts.every((part) => /^\d{2}$/.test(part))) return null;

  const year = 2000 + Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;

  return `${year}-${parts[1]}-${parts[2]}`;
}

function isValidDraft(draft: ReceiptView) {
  return Boolean(
    draft.lines.length > 0
    && draft.lines.every((line) => (
      line.name.trim()
      && Number(line.quantity) > 0
      && Number(line.unitPrice) >= 0
      && Number(line.lineTotal) >= 0
      && (line.targetType !== 'FRIDGE' || !line.expiresAt || expiryToIso(line.expiresAt))
    )),
  );
}

const styles = StyleSheet.create({
  cardBody: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  pickerRow: { flexDirection: 'row', gap: 10 },
  pickerButton: { flex: 1, minWidth: 0 },
  titleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  draftBadge: { backgroundColor: '#FBE6C5', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6 },
  draftBadgeText: { color: '#7A5522', fontSize: 11, fontWeight: '900' },
  fileName: { color: colors.muted, fontSize: 12, flex: 1, textAlign: 'right' },
  draftSummary: { color: colors.ink, fontSize: 16, fontWeight: '900', textAlign: 'right' },
  lineEditor: { borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 14, gap: 10 },
  numberRow: { flexDirection: 'row', gap: 8 },
  numberField: { flex: 1, minWidth: 0 },
  targetRow: { flexDirection: 'row', gap: 8 },
  target: { flex: 1, minHeight: 38, borderRadius: 12, borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' },
  targetSelected: { backgroundColor: colors.mint, borderColor: '#9BC8B8' },
  targetText: { color: colors.muted, fontSize: 12, fontWeight: '700' },
  targetTextSelected: { color: colors.ink },
  expiryField: { gap: 7 },
  expiryLabel: { color: colors.ink, fontSize: 13, fontWeight: '700' },
  expiryRow: { flexDirection: 'row', alignItems: 'center' },
  expiryPartRow: { flexDirection: 'row', alignItems: 'center' },
  expirySeparator: { color: colors.muted, fontSize: 18, paddingHorizontal: 8 },
  expiryInput: {
    width: 64,
    minHeight: 46,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    backgroundColor: '#FBFCFB',
    color: colors.ink,
    fontSize: 16,
    fontWeight: '800',
    textAlign: 'center',
  },
  listTitle: { color: colors.ink, fontSize: 18, fontWeight: '800', marginTop: 4 },
  receiptTitle: { color: colors.ink, fontSize: 17, fontWeight: '800' },
  total: { color: colors.ink, fontSize: 18, fontWeight: '900' },
});
