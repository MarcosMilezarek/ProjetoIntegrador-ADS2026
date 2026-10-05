import type { LoginResponse } from '@/types/auth';

const sessionKey = 'upteam.sessao';

export function readSession(): LoginResponse | null {
  try {
    const saved = JSON.parse(localStorage.getItem(sessionKey) ?? 'null');
    const known = saved && typeof saved.token === 'string' && typeof saved.id === 'number' && ['candidato', 'rh', 'administrador'].includes(saved.perfil);
    return known ? saved as LoginResponse : null;
  } catch { return null; }
}

export function writeSession(account: LoginResponse | null) {
  try {
    if (account) localStorage.setItem(sessionKey, JSON.stringify({ token: account.token, id: account.id, nome: account.nome, email: account.email, perfil: account.perfil }));
    else localStorage.removeItem(sessionKey);
  } catch { /* sem armazenamento, a sessão só não sobrevive ao F5 */ }
}
