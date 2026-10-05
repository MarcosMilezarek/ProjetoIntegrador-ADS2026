import type { ApplicationStatus, DocumentStatus, InterviewPresence, Job, StaffUser } from '@/types/domain';
import type { StatusUsuario } from '@/types/auth';

export type Tone = 'green' | 'yellow' | 'red' | 'outline' | 'dashed' | undefined;

export const appStatus: Record<ApplicationStatus, string> = { applied: 'Inscrito', reviewing: 'Em análise', interview: 'Entrevista', approved: 'Aprovado', rejected: 'Não selecionado', hired: 'Contratado', cancelled: 'Cancelado' };

export const appTone: Record<ApplicationStatus, Tone> = { applied: undefined, reviewing: 'yellow', interview: 'outline', approved: 'green', rejected: 'red', hired: 'green', cancelled: 'dashed' };

export const userStatus: Record<StatusUsuario, string> = { ativo: 'Ativo', inativo: 'Inativo', bloqueado: 'Bloqueado' };

export const userTone: Record<StatusUsuario, Tone> = { ativo: 'green', inativo: 'dashed', bloqueado: 'red' };

export const staffRoles: Record<StaffUser['role'], string> = { rh: 'RH', administrador: 'Administrador' };

export const documentStatus: Record<DocumentStatus, string> = { pendente: 'Pendente', aprovado: 'Aprovado', recusado: 'Recusado' };

export const documentTone: Record<DocumentStatus, Tone> = { pendente: 'yellow', aprovado: 'green', recusado: 'red' };

export const presenceStatus: Record<InterviewPresence, string> = { pendente: 'Pendente', confirmado: 'Confirmado' };

export const presenceTone: Record<InterviewPresence, Tone> = { pendente: 'yellow', confirmado: 'green' };

export const jobStatus = { rascunho: 'Rascunho', aberta: 'Aberta', encerrada: 'Encerrada' } as const;

export const jobTone: Record<Job['status'], Tone> = { rascunho: 'dashed', aberta: 'green', encerrada: 'red' };
