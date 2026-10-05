const baseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');

/** Token da sessão em uso. O App também o guarda em localStorage (`upteam.sessao`) e o redefine ao restaurar a sessão no F5. */
let authToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

/** Define (ou limpa, com null) o token enviado em `Authorization: Bearer` nas chamadas autenticadas. */
export function setAuthToken(token: string | null) { authToken = token; }
/** Registra o que fazer quando uma chamada autenticada volta 401 (sessão expirada ou token inválido). */
export function setUnauthorizedHandler(handler: (() => void) | null) { onUnauthorized = handler; }

/** Erro da API. `campos` traz o mapa campo → mensagem quando o backend responde 400 de validação. */
export class ApiError extends Error {
  status: number;
  campos?: Record<string, string>;
  constructor(message: string, status: number, campos?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.campos = campos;
  }
}

/** Extrai a mensagem do ErroResponse do backend ({mensagem, campos}). Sem ele (ex.: página HTML de 502 do nginx), usa um aviso genérico. */
async function erroDaResposta(response: Response): Promise<ApiError> {
  const texto = await response.text();
  try {
    const corpo = JSON.parse(texto);
    const campos = corpo?.campos as Record<string, string> | undefined;
    if (campos && typeof campos === 'object') {
      const primeiro = Object.values(campos)[0];
      if (typeof primeiro === 'string') return new ApiError(primeiro, response.status, campos);
    }
    if (typeof corpo?.mensagem === 'string') return new ApiError(corpo.mensagem, response.status);
  } catch {
    // corpo não é JSON
  }
  return new ApiError(`O servidor não conseguiu atender agora (código ${response.status}). Tente novamente em instantes.`, response.status);
}

export async function apiClient<T>(
  path: string,
  init: RequestInit = {},
  options?: { notFoundAsNull?: boolean; as?: 'json' | 'blob' | 'stream' },
): Promise<T> {
  if (!baseUrl) throw new Error('VITE_API_URL não está configurada.');

  const isFormData = init.body instanceof FormData;
  const token = authToken;
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: {
        ...(isFormData || options?.as === 'blob' || options?.as === 'stream' ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new Error('Não conseguimos conectar ao servidor. Verifique sua conexão com a internet e tente novamente em instantes.');
  }

  // Só reage se o token enviado ainda é o da sessão (um 401 tardio, depois de sair, não deve avisar nada).
  if (response.status === 401 && token && token === authToken) { authToken = null; onUnauthorized?.(); }
  if (response.status === 404 && options?.notFoundAsNull) return null as T;
  if (!response.ok) throw await erroDaResposta(response);

  if (options?.as === 'blob') return response.blob() as Promise<T>;
  if (options?.as === 'stream') return response as T;
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
