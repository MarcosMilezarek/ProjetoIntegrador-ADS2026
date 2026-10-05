import { useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { authService } from '@/types/auth-service';
import type { LoginResponse } from '@/types/auth';
import { Glow, Brand, CtaArrow, Field, PasswordInput, FormError } from '@/components/common';
import { ThemeSwitch } from '@/components/theme';

function AuthShell({ title, text, children }: { title: string; text: string; children: ReactNode }) {
  return <div className="auth-page">
    <Glow />
    <header className="glass-nav auth-nav"><Brand /><ThemeSwitch /></header>
    <main className="auth">
      <section className="auth-copy">
        <h1 className="display">{title}</h1>
        <p className="lede">{text}</p>
        <ul className="facts">
          <li><span><Check /></span>Um currículo para todas as candidaturas</li>
          <li><span><Check /></span>Cada etapa do processo visível para você</li>
          <li><span><Check /></span>Decisão sempre de uma pessoa do RH</li>
        </ul>
      </section>
      <section className="auth-stage">
        <div className="orb" aria-hidden="true"><span /></div>
        <div className="glass-card auth-card">{children}</div>
      </section>
    </main>
    <footer className="auth-foot">© 2026 Projeto Integrador III · TeamUp</footer>
  </div>;
}

export function Login({ onAccess, onCreateAccount }: { onAccess: (account: LoginResponse) => void; onCreateAccount: () => void }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(null); setSubmitting(true);
    try {
      onAccess(await authService.login({ email, senha }));
    } catch (err) { setError(err instanceof Error ? err.message : 'Não foi possível entrar. Tente novamente.'); }
    finally { setSubmitting(false); }
  };
  return <AuthShell title="Sua próxima vaga, do jeito que faz sentido acompanhar." text="Cadastre seu currículo uma vez, candidate-se em poucos cliques e acompanhe todas as etapas do processo.">
    <h2 className="display">Entrar</h2>
    <p className="card-lede">Use o e-mail e a senha do seu cadastro. Candidatos e RH entram pelo mesmo acesso.</p>
    <form className="auth-form" onSubmit={submit}>
      {error && <FormError title="Não foi possível entrar" text={error} />}
      <Field label="E-mail"><Input value={email} onChange={(event) => setEmail(event.target.value)} type="email" inputMode="email" autoComplete="email" required aria-invalid={Boolean(error) || undefined} /></Field>
      <Field label="Senha"><PasswordInput aria-invalid={Boolean(error) || undefined} value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></Field>
      <Button type="submit" size="lg" className="w-full cta" disabled={submitting}>{submitting ? 'Entrando…' : <>Entrar<CtaArrow /></>}</Button>
    </form>
    <p className="auth-alt">Ainda não tem conta? <Button type="button" variant="link" onClick={onCreateAccount}>Criar cadastro de candidato</Button></p>
  </AuthShell>;
}

export function Signup({ onBackToLogin }: { onBackToLogin: () => void }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const update = (key: keyof typeof form, value: string) => setForm((current) => ({ ...current, [key]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form.name.trim() || !form.email.trim()) { setError('Preencha nome e e-mail para continuar.'); return; }
    if (form.password.length < 8) { setError('A senha deve ter pelo menos 8 caracteres.'); return; }
    if (form.password !== form.confirmPassword) { setError('As senhas informadas não coincidem.'); return; }
    if (!accepted) { setError('É necessário aceitar os termos de uso e a política de privacidade.'); return; }
    setError(null); setSubmitting(true);
    try {
      await authService.cadastrar({ nome: form.name.trim(), email: form.email.trim(), senha: form.password });
      setSubmitted(true);
    } catch (err) { setError(err instanceof Error ? err.message : 'Não foi possível concluir o cadastro.'); }
    finally { setSubmitting(false); }
  };

  return <AuthShell title="Crie sua conta e comece a se candidatar." text="Cadastro gratuito para candidatos: monte seu currículo e acompanhe todas as suas candidaturas em um só lugar.">
    {submitted
      ? <div className="auth-done">
          <div className="done-mark"><Check /></div>
          <h2 className="display">Cadastro realizado</h2>
          <p className="card-lede">Entre com seu e-mail e senha e complete seu currículo antes de se candidatar.</p>
          <Button size="lg" className="w-full cta" onClick={onBackToLogin}>Ir para o login<CtaArrow /></Button>
        </div>
      : <>
          <h2 className="display">Criar cadastro</h2>
          <p className="card-lede">Leva menos de dois minutos. O currículo você completa depois, com calma.</p>
          <form className="auth-form" onSubmit={submit}>
            <Field label="Nome completo"><Input required autoComplete="name" value={form.name} onChange={(event) => update('name', event.target.value)} /></Field>
            <Field label="E-mail"><Input required type="email" inputMode="email" autoComplete="email" value={form.email} onChange={(event) => update('email', event.target.value)} /></Field>
            <div className="field-row">
              <Field label="Senha" hint="Mínimo de 8 caracteres"><PasswordInput required autoComplete="new-password" value={form.password} onChange={(event) => update('password', event.target.value)} /></Field>
              <Field label="Confirmar senha"><PasswordInput required autoComplete="new-password" value={form.confirmPassword} onChange={(event) => update('confirmPassword', event.target.value)} /></Field>
            </div>
            <label className="check-label"><Checkbox checked={accepted} onCheckedChange={(value) => setAccepted(value === true)} /> Li e aceito os termos de uso e a política de privacidade (LGPD).</label>
            {error && <FormError title="Não foi possível concluir o cadastro" text={error} />}
            <Button type="submit" size="lg" className="w-full cta" disabled={submitting}>{submitting ? 'Criando conta…' : <>Criar minha conta<CtaArrow /></>}</Button>
          </form>
          <p className="auth-alt">Já tem uma conta? <Button type="button" variant="link" onClick={onBackToLogin}>Entrar</Button></p>
        </>}
  </AuthShell>;
}
