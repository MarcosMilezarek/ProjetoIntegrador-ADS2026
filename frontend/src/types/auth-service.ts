import type { LoginRequest, LoginResponse } from '@/types/auth';

export interface AuthService {
  login(credentials: LoginRequest): Promise<LoginResponse>;
}

const mockAccounts: Record<string, { senha: string; response: LoginResponse }> = {
  'marina.souza@email.com': { senha: 'senha123', response: { id: 1, nome: 'Marina Souza Andrade', email: 'marina.souza@email.com', perfil: 'candidato' } },
  'camila.torres@email.com': { senha: 'senha123', response: { id: 2, nome: 'Camila Torres', email: 'camila.torres@email.com', perfil: 'rh' } },
};

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => window.setTimeout(() => resolve(value), 180));
}

export const mockAuthService: AuthService = {
  async login({ email, senha }) {
    const account = mockAccounts[email.trim().toLowerCase()];
    if (!account || account.senha !== senha) throw new Error('E-mail ou senha inválidos.');
    return delay(account.response);
  },
};

export const restAuthService: AuthService = {
  async login(credentials) {
    const baseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');
    if (!baseUrl) throw new Error('VITE_API_URL não está configurada.');
    const response = await fetch(`${baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
    if (response.status === 401) throw new Error('E-mail ou senha inválidos.');
    if (response.status === 403) throw new Error('Usuário bloqueado ou inativo. Entre em contato com o suporte.');
    if (!response.ok) throw new Error('Não foi possível entrar. Tente novamente em instantes.');
    return response.json() as Promise<LoginResponse>;
  },
};

export const authService = import.meta.env.VITE_USE_MOCK_API === 'false'
  ? restAuthService
  : mockAuthService;