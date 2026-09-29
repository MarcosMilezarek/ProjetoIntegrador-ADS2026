import { apiClient } from '@/lib/api-client';
import { formatDate } from '@/lib/utils';
import type { CandidateProfile, Job, JobStatus, NewJobInput, Sexo } from '@/types/domain';

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
  dataNascimento: string | null;
  idade: number | null;
  sexo: string | null;
  cidade: string | null;
  uf: string | null;
  numeroContato: string | null;
  perfilLinkedin: string | null;
  competencias: string | null;
  certificacoes: string | null;
  resumo: string | null;
  formacoes: { id: number; curso: string; instituicao: string; dataInicio: string; dataTermino: string | null }[] | null;
  experiencias:
    | { id: number; cargo: string; empresa: string; dataContratacao: string; dataDemissao: string | null; trabalhoAtual: boolean; descricaoAtividades: string | null }[]
    | null;
  arquivo: { id: number; nomeOriginal: string; contentType: string; tamanhoBytes: number; enviadoEm: string } | null;
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

const sexos: Sexo[] = ['feminino', 'masculino', 'outro', 'nao_informado'];

function profileFromResponse(curriculo: CurriculoResponseDTO): CandidateProfile {
  return {
    id: String(curriculo.id),
    dataNascimento: curriculo.dataNascimento ?? '',
    idade: curriculo.idade ?? undefined,
    sexo: sexos.find((item) => item === curriculo.sexo) ?? '',
    cidade: curriculo.cidade ?? '',
    uf: curriculo.uf ?? '',
    numeroContato: curriculo.numeroContato ?? '',
    perfilLinkedin: curriculo.perfilLinkedin ?? '',
    skills: curriculo.competencias ? curriculo.competencias.split(',').map((item) => item.trim()).filter(Boolean) : [],
    certificacoes: curriculo.certificacoes ?? '',
    resumo: curriculo.resumo ?? '',
    formacoes: (curriculo.formacoes ?? []).map((item) => ({
      uid: crypto.randomUUID(),
      curso: item.curso,
      instituicao: item.instituicao,
      dataInicio: item.dataInicio,
      dataTermino: item.dataTermino ?? '',
    })),
    experiencias: (curriculo.experiencias ?? []).map((item) => ({
      uid: crypto.randomUUID(),
      cargo: item.cargo,
      empresa: item.empresa,
      dataContratacao: item.dataContratacao,
      dataDemissao: item.dataDemissao ?? '',
      trabalhoAtual: item.trabalhoAtual,
      descricaoAtividades: item.descricaoAtividades ?? '',
    })),
    arquivo: curriculo.arquivo
      ? { id: String(curriculo.arquivo.id), nomeOriginal: curriculo.arquivo.nomeOriginal, tamanhoBytes: curriculo.arquivo.tamanhoBytes, enviadoEm: curriculo.arquivo.enviadoEm }
      : null,
    updatedAt: formatDate(curriculo.atualizadoEm),
  };
}

export const emptyProfile: CandidateProfile = {
  dataNascimento: '', sexo: '', cidade: '', uf: '', numeroContato: '', perfilLinkedin: '',
  skills: [], certificacoes: '', resumo: '', formacoes: [], experiencias: [], arquivo: null,
};

const orNull = (value: string) => (value.trim() ? value.trim() : null);

/** Corpo do POST/PUT. `idade` e os ids dos itens não são enviados; as listas vão sempre completas (o PUT as substitui). */
function curriculoBody(profile: CandidateProfile) {
  return {
    dataNascimento: orNull(profile.dataNascimento),
    sexo: profile.sexo || null,
    cidade: orNull(profile.cidade),
    uf: orNull(profile.uf)?.toUpperCase() ?? null,
    numeroContato: orNull(profile.numeroContato),
    perfilLinkedin: orNull(profile.perfilLinkedin),
    competencias: profile.skills.join(', '),
    certificacoes: orNull(profile.certificacoes),
    resumo: profile.resumo,
    formacoes: profile.formacoes.map((item) => ({
      curso: item.curso.trim(),
      instituicao: item.instituicao.trim(),
      dataInicio: orNull(item.dataInicio),
      dataTermino: orNull(item.dataTermino),
    })),
    experiencias: profile.experiencias.map((item) => ({
      cargo: item.cargo.trim(),
      empresa: item.empresa.trim(),
      dataContratacao: orNull(item.dataContratacao),
      dataDemissao: item.trabalhoAtual ? null : orNull(item.dataDemissao),
      trabalhoAtual: item.trabalhoAtual,
      descricaoAtividades: orNull(item.descricaoAtividades),
    })),
  };
}

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
    const dadosCurriculo = curriculoBody(profile);
    const curriculo = profile.id
      ? await apiClient<CurriculoResponseDTO>(`/curriculos/${profile.id}`, { method: 'PUT', body: JSON.stringify(dadosCurriculo) })
      : await apiClient<CurriculoResponseDTO>('/curriculos', { method: 'POST', body: JSON.stringify({ usuarioId: Number(usuarioId), ...dadosCurriculo }) });
    return profileFromResponse(curriculo);
  },

  /** Anexa (ou substitui) o PDF do currículo; devolve o currículo completo com o bloco `arquivo`. */
  uploadResumeFile: async (curriculoId: string, file: File) => {
    const body = new FormData();
    body.append('arquivo', file);
    return profileFromResponse(await apiClient<CurriculoResponseDTO>(`/curriculos/${curriculoId}/arquivo`, { method: 'POST', body }));
  },

  downloadResumeFile: (curriculoId: string) => apiClient<Blob>(`/curriculos/${curriculoId}/arquivo`, {}, { as: 'blob' }),
};
