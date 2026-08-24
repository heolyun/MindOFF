import type { PropsWithChildren, ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
} from 'react-native';

export const colors = {
  background: '#F4F1EA',
  surface: '#FFFFFF',
  ink: '#173B34',
  muted: '#6E7F79',
  line: '#DDE4E0',
  mint: '#DDEFE8',
  accent: '#F2B45B',
  danger: '#A94D42',
};

export function Screen({ children }: PropsWithChildren) {
  return (
    <ScrollView
      contentContainerStyle={styles.screen}
      keyboardShouldPersistTaps="handled"
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  );
}

export function SectionHeader({ title, description }: { title: string; description?: string }) {
  return (
    <View style={styles.sectionHeader}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {description ? <Text style={styles.sectionDescription}>{description}</Text> : null}
    </View>
  );
}

export function Card({ children }: PropsWithChildren) {
  return <View style={styles.card}>{children}</View>;
}

type TextFieldProps = TextInputProps & {
  label: string;
  rightAction?: {
    label: string;
    accessibilityLabel: string;
    onPress: () => void;
  };
};

export function TextField({ label, rightAction, ...props }: TextFieldProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={styles.inputRow}>
        <TextInput
          placeholderTextColor="#9AA7A2"
          style={[styles.input, rightAction ? styles.inputWithAction : null]}
          autoCapitalize="none"
          {...props}
        />
        {rightAction ? (
          <Pressable
            accessibilityLabel={rightAction.accessibilityLabel}
            accessibilityRole="button"
            disabled={props.editable === false}
            onPress={rightAction.onPress}
            hitSlop={8}
            style={({ pressed }) => [styles.inputAction, pressed && styles.inputActionPressed]}
          >
            <Text style={styles.inputActionText}>{rightAction.label}</Text>
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}

export function PrimaryButton({
  label,
  onPress,
  disabled,
  variant = 'primary',
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  variant?: 'primary' | 'quiet';
}) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        variant === 'quiet' && styles.quietButton,
        pressed && styles.buttonPressed,
        disabled && styles.buttonDisabled,
      ]}
    >
      <Text style={[styles.buttonText, variant === 'quiet' && styles.quietButtonText]}>{label}</Text>
    </Pressable>
  );
}

export function AsyncState({ loading, error }: { loading: boolean; error: string | null }) {
  if (loading) {
    return (
      <View style={styles.stateBox}>
        <ActivityIndicator color={colors.ink} />
        <Text style={styles.stateText}>불러오는 중</Text>
      </View>
    );
  }
  if (error) {
    return (
      <View style={styles.errorBox}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }
  return null;
}

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <View style={styles.stateBox}>
      <Text style={styles.stateText}>{children}</Text>
    </View>
  );
}

export const styles = StyleSheet.create({
  screen: {
    width: '100%',
    maxWidth: 720,
    alignSelf: 'center',
    paddingHorizontal: 20,
    paddingTop: 24,
    paddingBottom: 132,
    gap: 16,
  },
  sectionHeader: { gap: 6, marginBottom: 4, minWidth: 0 },
  sectionTitle: { color: colors.ink, fontSize: 30, lineHeight: 36, fontWeight: '800', letterSpacing: -0.8 },
  sectionDescription: { color: colors.muted, fontSize: 15, lineHeight: 22 },
  card: {
    width: '100%',
    minWidth: 0,
    backgroundColor: colors.surface,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
    gap: 12,
    shadowColor: '#173B34',
    shadowOpacity: 0.05,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 2,
  },
  field: { gap: 7 },
  fieldLabel: { color: colors.ink, fontSize: 13, fontWeight: '700' },
  inputRow: { position: 'relative', justifyContent: 'center' },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 14,
    color: colors.ink,
    backgroundColor: '#FBFCFB',
    fontSize: 15,
  },
  inputWithAction: { paddingRight: 64 },
  inputAction: { position: 'absolute', right: 14, minHeight: 34, justifyContent: 'center', paddingHorizontal: 4 },
  inputActionPressed: { opacity: 0.55 },
  inputActionText: { color: colors.ink, fontSize: 13, fontWeight: '800' },
  button: {
    width: '100%',
    minWidth: 0,
    minHeight: 46,
    borderRadius: 14,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  quietButton: { backgroundColor: colors.mint },
  buttonPressed: { opacity: 0.78 },
  buttonDisabled: { opacity: 0.4 },
  buttonText: { color: '#FFFFFF', fontSize: 14, fontWeight: '800', flexShrink: 1, textAlign: 'center' },
  quietButtonText: { color: colors.ink },
  stateBox: { borderRadius: 18, backgroundColor: colors.mint, padding: 18, alignItems: 'center', gap: 10 },
  stateText: { color: colors.muted, fontSize: 14, lineHeight: 20, textAlign: 'center' },
  errorBox: { borderRadius: 18, backgroundColor: '#F9E6E2', padding: 18 },
  errorText: { color: colors.danger, fontSize: 14, lineHeight: 20 },
});
