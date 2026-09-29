const baseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');

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

/** Extrai a mensagem do ErroResponse do backend ({mensagem, campos}); usa o texto cru quando o corpo não ajuda. */
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
  return new ApiError(texto || `Falha na API (${response.status}).`, response.status);
}

export async function apiClient<T>(
  path: string,
  init: RequestInit = {},
  options?: { notFoundAsNull?: boolean; as?: 'json' | 'blob' },
): Promise<T> {
  if (!baseUrl) throw new Error('VITE_API_URL não está configurada.');

  const isFormData = init.body instanceof FormData;
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: { ...(isFormData || options?.as === 'blob' ? {} : { 'Content-Type': 'application/json' }), ...init.headers },
    });
  } catch {
    throw new Error('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.');
  }

  if (response.status === 404 && options?.notFoundAsNull) return null as T;
  if (!response.ok) throw await erroDaResposta(response);

  if (options?.as === 'blob') return response.blob() as Promise<T>;
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
