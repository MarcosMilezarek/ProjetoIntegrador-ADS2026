export type Perfil = 'candidato' | 'rh' | 'administrador';

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
}