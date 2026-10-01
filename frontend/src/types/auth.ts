export type Perfil = 'candidato' | 'rh' | 'administrador';
export type StatusUsuario = 'ativo' | 'inativo' | 'bloqueado';

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  /** JWT para `Authorization: Bearer`. Vale 8 horas. */
  token: string;
}

export interface SignupRequest {
  nome: string;
  email: string;
  senha: string;
}

/** Resposta de POST/GET /usuarios. */
export interface UsuarioResponse extends Omit<LoginResponse, 'token'> {
  status: StatusUsuario;
}
