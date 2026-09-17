const baseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');

/** Extrai a mensagem do ErroResponse do backend ({mensagem, campos}); usa o texto cru quando o corpo não ajuda. */
async function mensagemDeErro(response: Response): Promise<string> {
  const texto = await response.text();
  try {
    const corpo = JSON.parse(texto);
    const campos = corpo?.campos as Record<string, string> | undefined;
    if (campos) {
      const primeiro = Object.values(campos)[0];
      if (typeof primeiro === 'string') return primeiro;
    }
    if (typeof corpo?.mensagem === 'string') return corpo.mensagem;
  } catch {
    // corpo não é JSON
  }
  return texto || `Falha na API (${response.status}).`;
}

export async function apiClient<T>(path: string, init: RequestInit = {}, options?: { notFoundAsNull?: boolean }): Promise<T> {
  if (!baseUrl) throw new Error('VITE_API_URL não está configurada.');

  const isFormData = init.body instanceof FormData;
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...init.headers },
  });

  if (response.status === 404 && options?.notFoundAsNull) return null as T;
  if (!response.ok) throw new Error(await mensagemDeErro(response));

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
