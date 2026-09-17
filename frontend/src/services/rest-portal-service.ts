import { apiClient } from '@/lib/api-client';
import { formatDate } from '@/lib/utils';
import type { CandidateProfile, Job, JobStatus, NewJobInput } from '@/types/domain';

/** Formato bruto retornado por /vagas (VagaResponse do backend). */
interface VagaResponseDTO {
  id: number;
  rhId: number;
  titulo: string;
  descricao: string;
  requisitos: string | null;
  local: string | null;
  modalidade: string;
  tipoContrato: string;
  status: string;
  prazo: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

/** Formato bruto retornado por /curriculos (CurriculoResponse do backend). */
interface CurriculoResponseDTO {
  id: number;
  usuarioId: number;
  formacao: string | null;
  experiencias: string | null;
  competencias: string | null;
  resumo: string | null;
  atualizadoEm: string;
}

const modalidadeToApi: Record<Job['workModel'], string> = { Presencial: 'presencial', Remoto: 'remoto', Híbrido: 'hibrido' };
const modalidadeFromApi: Record<string, Job['workModel']> = { presencial: 'Presencial', remoto: 'Remoto', hibrido: 'Híbrido' };
const tipoContratoToApi: Record<Job['contract'], string> = { CLT: 'clt', PJ: 'pj', Estágio: 'estagio', Temporário: 'temporario' };
const tipoContratoFromApi: Record<string, Job['contract']> = { clt: 'CLT', pj: 'PJ', estagio: 'Estágio', temporario: 'Temporário' };

function jobFromResponse(vaga: VagaResponseDTO): Job {
  return {
    id: String(vaga.id),
    title: vaga.titulo,
    city: vaga.local ?? '',
    workModel: modalidadeFromApi[vaga.modalidade] ?? 'Presencial',
    contract: tipoContratoFromApi[vaga.tipoContrato] ?? 'CLT',
    publishedAt: formatDate(vaga.criadoEm),
    closesAt: vaga.prazo ?? undefined,
    status: vaga.status as JobStatus,
    description: vaga.descricao,
    requirements: vaga.requisitos ? vaga.requisitos.split('\n').map((item) => item.trim()).filter(Boolean) : [],
  };
}

function vagaBody(input: NewJobInput & { status: JobStatus }) {
  return {
    titulo: input.title,
    descricao: input.description,
    requisitos: input.requirements.join('\n'),
    local: input.city,
    modalidade: modalidadeToApi[input.workModel],
    tipoContrato: tipoContratoToApi[input.contract],
    status: input.status,
    prazo: input.closesAt || null,
  };
}

function profileFromResponse(curriculo: CurriculoResponseDTO): CandidateProfile {
  return {
    id: String(curriculo.id),
    education: curriculo.formacao ?? '',
    experience: curriculo.experiencias ?? '',
    skills: curriculo.competencias ? curriculo.competencias.split(',').map((item) => item.trim()).filter(Boolean) : [],
    resumo: curriculo.resumo ?? '',
    updatedAt: formatDate(curriculo.atualizadoEm),
  };
}

const emptyProfile: CandidateProfile = { education: '', experience: '', skills: [], resumo: '' };

export const restPortalService = {
  getJobs: async () => (await apiClient<VagaResponseDTO[]>('/vagas')).map(jobFromResponse),

  saveJob: async (input: NewJobInput & { id?: string; status?: JobStatus }, rhId: string) => {
    const vaga = input.id
      ? await apiClient<VagaResponseDTO>(`/vagas/${input.id}`, { method: 'PUT', body: JSON.stringify(vagaBody({ ...input, status: input.status ?? 'aberta' })) })
      : await apiClient<VagaResponseDTO>('/vagas', { method: 'POST', body: JSON.stringify({ ...vagaBody({ ...input, status: 'aberta' }), rhId: Number(rhId) }) });
    return jobFromResponse(vaga);
  },

  closeJob: async (id: string) => {
    const current = await apiClient<VagaResponseDTO>(`/vagas/${id}`);
    const updated = await apiClient<VagaResponseDTO>(`/vagas/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        titulo: current.titulo,
        descricao: current.descricao,
        requisitos: current.requisitos,
        local: current.local,
        modalidade: current.modalidade,
        tipoContrato: current.tipoContrato,
        status: 'encerrada',
        prazo: current.prazo,
      }),
    });
    return jobFromResponse(updated);
  },

  getProfile: async (usuarioId: string) => {
    const curriculo = await apiClient<CurriculoResponseDTO | null>(`/curriculos/usuario/${usuarioId}`, {}, { notFoundAsNull: true });
    return curriculo ? profileFromResponse(curriculo) : emptyProfile;
  },

  updateProfile: async (usuarioId: string, profile: CandidateProfile) => {
    const dadosCurriculo = { formacao: profile.education, experiencias: profile.experience, competencias: profile.skills.join(', '), resumo: profile.resumo };
    const curriculo = profile.id
      ? await apiClient<CurriculoResponseDTO>(`/curriculos/${profile.id}`, { method: 'PUT', body: JSON.stringify(dadosCurriculo) })
      : await apiClient<CurriculoResponseDTO>('/curriculos', { method: 'POST', body: JSON.stringify({ usuarioId: Number(usuarioId), ...dadosCurriculo }) });
    return profileFromResponse(curriculo);
  },
};
