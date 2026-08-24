import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';

import {
  CognitoAuthError,
  confirmPasswordReset,
  confirmSignUp,
  requestPasswordReset,
  resendConfirmationCode,
  signInWithCognito,
  signUpWithCognito,
} from '../auth';
import { Card, PrimaryButton, TextField, colors } from '../ui';

type AuthMode = 'sign-in' | 'sign-up' | 'confirm' | 'forgot' | 'reset';

export function AuthScreen({ onAuthenticated }: { onAuthenticated: (token: string) => Promise<void> }) {
  const [mode, setMode] = useState<AuthMode>('sign-in');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode);
    setCode('');
    setPasswordConfirm('');
    setError(null);
    setNotice(null);
  }

  async function submitSignIn() {
    await run(async () => {
      try {
        const token = await signInWithCognito(email, password);
        await onAuthenticated(token);
      } catch (reason) {
        if (reason instanceof CognitoAuthError && reason.code === 'UserNotConfirmedException') {
          changeMode('confirm');
          setNotice('이메일로 보낸 인증번호를 입력해 주세요.');
          return;
        }
        throw reason;
      }
    });
  }

  async function submitSignUp() {
    if (password !== passwordConfirm) {
      setError('비밀번호가 서로 다릅니다.');
      return;
    }
    await run(async () => {
      const confirmed = await signUpWithCognito(email, password, name);
      if (confirmed) {
        const token = await signInWithCognito(email, password);
        await onAuthenticated(token);
        return;
      }
      changeMode('confirm');
      setNotice('이메일로 인증번호를 보냈습니다.');
    });
  }

  async function submitConfirmation() {
    await run(async () => {
      await confirmSignUp(email, code);
      const token = await signInWithCognito(email, password);
      await onAuthenticated(token);
    });
  }

  async function resendCode() {
    await run(async () => {
      await resendConfirmationCode(email);
      setNotice('인증번호를 다시 보냈습니다.');
    });
  }

  async function submitForgotPassword() {
    await run(async () => {
      await requestPasswordReset(email);
      changeMode('reset');
      setPassword('');
      setNotice('이메일로 인증번호를 보냈습니다.');
    });
  }

  async function submitPasswordReset() {
    if (password !== passwordConfirm) {
      setError('비밀번호가 서로 다릅니다.');
      return;
    }
    await run(async () => {
      await confirmPasswordReset(email, code, password);
      changeMode('sign-in');
      setPassword('');
      setNotice('비밀번호가 변경되었습니다.');
    });
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '요청을 처리하지 못했습니다.');
    } finally {
      setBusy(false);
    }
  }

  const validEmail = email.includes('@') && email.includes('.');
  const validPassword = password.length >= 10;

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.page}
    >
      <View style={styles.container}>
        <View style={styles.brandRow}>
          <Text style={styles.brand}>MindOFF</Text>
          <View style={styles.dot} />
        </View>

        <Card>
          <View style={styles.heading}>
            <Text style={styles.title}>{titleFor(mode)}</Text>
            {subtitleFor(mode) ? <Text style={styles.subtitle}>{subtitleFor(mode)}</Text> : null}
          </View>

          {mode === 'sign-up' ? (
            <TextField
              label="이름"
              value={name}
              onChangeText={setName}
              autoComplete="name"
              textContentType="name"
              editable={!busy}
            />
          ) : null}

          {mode !== 'reset' || !email ? (
            <TextField
              label="이메일"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoComplete="email"
              textContentType="emailAddress"
              editable={!busy && mode !== 'confirm'}
            />
          ) : (
            <Text style={styles.email}>{email.trim().toLowerCase()}</Text>
          )}

          {mode === 'sign-in' || mode === 'sign-up' ? (
            <TextField
              label="비밀번호"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoComplete={mode === 'sign-up' ? 'new-password' : 'current-password'}
              textContentType={mode === 'sign-up' ? 'newPassword' : 'password'}
              editable={!busy}
            />
          ) : null}

          {mode === 'sign-up' || mode === 'reset' ? (
            <>
              {mode === 'reset' ? (
                <TextField
                  label="인증번호"
                  value={code}
                  onChangeText={setCode}
                  keyboardType="number-pad"
                  autoComplete="one-time-code"
                  textContentType="oneTimeCode"
                  editable={!busy}
                />
              ) : null}
              {mode === 'reset' ? (
                <TextField
                  label="새 비밀번호"
                  value={password}
                  onChangeText={setPassword}
                  secureTextEntry
                  autoComplete="new-password"
                  textContentType="newPassword"
                  editable={!busy}
                />
              ) : null}
              <TextField
                label="비밀번호 확인"
                value={passwordConfirm}
                onChangeText={setPasswordConfirm}
                secureTextEntry
                autoComplete="new-password"
                textContentType="newPassword"
                editable={!busy}
              />
              <Text style={styles.rule}>10자 이상 · 대문자 · 소문자 · 숫자</Text>
            </>
          ) : null}

          {mode === 'confirm' ? (
            <TextField
              label="인증번호"
              value={code}
              onChangeText={setCode}
              keyboardType="number-pad"
              autoComplete="one-time-code"
              textContentType="oneTimeCode"
              editable={!busy}
            />
          ) : null}

          {notice ? <Text style={styles.notice}>{notice}</Text> : null}
          {error ? <Text style={styles.error}>{error}</Text> : null}

          <PrimaryButton
            label={busy ? '처리 중…' : actionLabel(mode)}
            onPress={() => void submitFor(mode)}
            disabled={busy || !canSubmit(mode)}
          />

          {busy ? <ActivityIndicator color={colors.ink} /> : null}

          <View style={styles.links}>
            {mode === 'sign-in' ? (
              <>
                <Link label="회원가입" onPress={() => changeMode('sign-up')} />
                <Link label="비밀번호 재설정" onPress={() => changeMode('forgot')} />
              </>
            ) : null}
            {mode === 'confirm' ? <Link label="인증번호 다시 받기" onPress={() => void resendCode()} /> : null}
            {mode !== 'sign-in' ? <Link label="로그인으로 돌아가기" onPress={() => changeMode('sign-in')} /> : null}
          </View>
        </Card>
      </View>
    </KeyboardAvoidingView>
  );

  function canSubmit(currentMode: AuthMode) {
    if (currentMode === 'sign-in') return validEmail && password.length > 0;
    if (currentMode === 'sign-up') return validEmail && name.trim().length > 0 && validPassword && passwordConfirm.length > 0;
    if (currentMode === 'confirm') return validEmail && code.trim().length >= 4 && password.length > 0;
    if (currentMode === 'forgot') return validEmail;
    return validEmail && code.trim().length >= 4 && validPassword && passwordConfirm.length > 0;
  }

  async function submitFor(currentMode: AuthMode) {
    if (currentMode === 'sign-in') return submitSignIn();
    if (currentMode === 'sign-up') return submitSignUp();
    if (currentMode === 'confirm') return submitConfirmation();
    if (currentMode === 'forgot') return submitForgotPassword();
    return submitPasswordReset();
  }
}

function Link({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => pressed && styles.linkPressed}>
      <Text style={styles.link}>{label}</Text>
    </Pressable>
  );
}

function titleFor(mode: AuthMode) {
  if (mode === 'sign-up') return '회원가입';
  if (mode === 'confirm') return '이메일 인증';
  if (mode === 'forgot') return '비밀번호 재설정';
  if (mode === 'reset') return '새 비밀번호';
  return '로그인';
}

function subtitleFor(mode: AuthMode) {
  if (mode === 'confirm') return '이메일로 받은 번호를 입력하세요.';
  if (mode === 'forgot') return '가입한 이메일을 입력하세요.';
  return '';
}

function actionLabel(mode: AuthMode) {
  if (mode === 'sign-up') return '가입하기';
  if (mode === 'confirm') return '인증하기';
  if (mode === 'forgot') return '인증번호 받기';
  if (mode === 'reset') return '변경하기';
  return '로그인';
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background },
  container: { width: '88%', maxWidth: 412, alignSelf: 'center', flex: 1, justifyContent: 'center', paddingVertical: 24, gap: 24 },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  brand: { color: colors.ink, fontSize: 38, fontWeight: '900', letterSpacing: -1.4 },
  dot: { width: 12, height: 12, borderRadius: 6, backgroundColor: colors.accent, marginTop: 6 },
  heading: { gap: 5, marginBottom: 4 },
  title: { color: colors.ink, fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  subtitle: { color: colors.muted, fontSize: 14, lineHeight: 20 },
  email: { color: colors.ink, fontSize: 14, fontWeight: '700', paddingVertical: 4 },
  rule: { color: colors.muted, fontSize: 12, lineHeight: 18 },
  notice: { color: colors.ink, backgroundColor: colors.mint, borderRadius: 12, padding: 12, fontSize: 13 },
  error: { color: colors.danger, backgroundColor: '#F9E6E2', borderRadius: 12, padding: 12, fontSize: 13 },
  links: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'center', gap: 18, paddingTop: 2 },
  link: { color: colors.ink, fontSize: 13, fontWeight: '700' },
  linkPressed: { opacity: 0.55 },
});
