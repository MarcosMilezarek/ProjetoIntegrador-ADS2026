import { apiClient } from '@/lib/api-client';
import type { LoginRequest, LoginResponse, SignupRequest, UsuarioResponse } from '@/types/auth';

const baseUrl = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');

export interface AuthService {
  login(credentials: LoginRequest): Promise<LoginResponse>;
  cadastrar(dados: SignupRequest): Promise<UsuarioResponse>;
  /** Confere o token já definido com `setAuthToken` e devolve quem está logado (GET /auth/me). */
  usuarioAtual(): Promise<UsuarioResponse>;
}

async function postJson(caminho: string, corpo: unknown): Promise<Response> {
  try {
    return await fetch(`${baseUrl}${caminho}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(corpo),
    });
  } catch {
    throw new Error('Não conseguimos conectar ao servidor. Verifique sua conexão com a internet e tente novamente em instantes.');
  }
}

/** Extrai a mensagem do ErroResponse do backend; usa o padrao quando o corpo nao ajuda. */
async function mensagemDeErro(response: Response, padrao: string): Promise<string> {
  try {
    const corpo = await response.json();
    const campos = corpo?.campos as Record<string, string> | undefined;
    if (campos) {
      const primeiro = Object.values(campos)[0];
      if (typeof primeiro === 'string') return primeiro;
    }
    if (typeof corpo?.mensagem === 'string') return corpo.mensagem;
  } catch {
    // corpo vazio ou fora do formato esperado
  }
  return padrao;
}

export const authService: AuthService = {
  async login(credentials) {
    const response = await postJson('/auth/login', credentials);
    if (response.status === 401) throw new Error('E-mail ou senha incorretos. Confira os dados e tente novamente.');
    if (response.status === 403) throw new Error('Seu acesso está bloqueado ou inativo no momento. Entre em contato com o suporte para entender o que aconteceu.');
    if (!response.ok) throw new Error(await mensagemDeErro(response, 'Não foi possível entrar. Tente novamente em instantes.'));
    return response.json() as Promise<LoginResponse>;
  },

  async cadastrar({ nome, email, senha }) {
    const response = await postJson('/usuarios', { nome, email, senha, perfil: 'candidato' });
    if (response.status === 409) throw new Error('Já existe uma conta cadastrada com este e-mail. Use a tela de entrada para acessá-la.');
    if (!response.ok) throw new Error(await mensagemDeErro(response, 'Não foi possível concluir o cadastro. Tente novamente em instantes.'));
    return response.json() as Promise<UsuarioResponse>;
  },

  usuarioAtual: () => apiClient<UsuarioResponse>('/auth/me'),
};
