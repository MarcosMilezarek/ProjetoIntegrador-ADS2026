const baseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '');

export async function apiClient<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!baseUrl) throw new Error('VITE_API_URL não está configurada.');

  const isFormData = init.body instanceof FormData;
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...init.headers },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Falha na API (${response.status}).`);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
