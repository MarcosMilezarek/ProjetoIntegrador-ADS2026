import { apiClient } from '@/lib/api-client';
import { formatDate } from '@/lib/utils';
import type { Application, ApplicationStatus, Candidate, CandidateDocument, CandidateProfile, Job, JobStatus, NewJobInput, Sexo, StaffUser, StaffUserInput } from '@/types/domain';
import type { Perfil, StatusUsuario } from '@/types/auth';

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

/** Formato bruto de /candidaturas e /vagas/{id}/candidaturas (CandidaturaResponse do backend). */
interface CandidaturaResponseDTO {
  id: number;
  vagaId: number;
  vagaTitulo: string;
  vagaStatus: string;
  candidatoId: number;
  candidatoNome: string;
  candidatoEmail: string;
  status: string;
  dataCandidatura: string;
}

/** Formato bruto de /documentos (DocumentoResponse do backend). */
interface DocumentoResponseDTO {
  id: number;
  candidaturaId: number;
  vagaId: number;
  vagaTitulo: string;
  candidatoId: number;
  candidatoNome: string;
  tipo: string;
  formato: string;
  tamanhoBytes: number;
  dataEnvio: string;
}

/** Formato bruto de /usuarios (UsuarioResponse do backend). */
interface UsuarioResponseDTO {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  status: StatusUsuario;
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

const statusFromApi: Record<string, ApplicationStatus> = { inscrito: 'applied', em_triagem: 'reviewing', entrevista: 'interview', aprovado: 'approved', reprovado: 'rejected', contratado: 'hired', cancelado: 'cancelled' };
const statusToApi: Record<ApplicationStatus, string> = { applied: 'inscrito', reviewing: 'em_triagem', interview: 'entrevista', approved: 'aprovado', rejected: 'reprovado', hired: 'contratado', cancelled: 'cancelado' };

function applicationFromResponse(item: CandidaturaResponseDTO): Application {
  return {
    id: String(item.id),
    jobId: String(item.vagaId),
    jobTitle: item.vagaTitulo,
    jobStatus: item.vagaStatus as JobStatus,
    candidateId: String(item.candidatoId),
    submittedAt: formatDate(item.dataCandidatura),
    status: statusFromApi[item.status] ?? 'applied',
  };
}

function candidateFromResponse(item: CandidaturaResponseDTO): Candidate {
  return {
    id: String(item.candidatoId),
    name: item.candidatoNome,
    email: item.candidatoEmail,
    applicationId: String(item.id),
    submittedAt: formatDate(item.dataCandidatura),
    status: statusFromApi[item.status] ?? 'applied',
  };
}

function documentFromResponse(item: DocumentoResponseDTO): CandidateDocument {
  return {
    id: String(item.id),
    applicationId: String(item.candidaturaId),
    jobId: String(item.vagaId),
    jobTitle: item.vagaTitulo,
    candidateId: String(item.candidatoId),
    candidateName: item.candidatoNome,
    type: item.tipo,
    format: item.formato,
    sizeBytes: item.tamanhoBytes,
    sentAt: formatDate(item.dataEnvio),
  };
}

function userFromResponse(item: UsuarioResponseDTO): StaffUser {
  return { id: String(item.id), name: item.nome, email: item.email, role: item.perfil as StaffUser['role'], status: item.status };
}

function usuarioBody(input: StaffUserInput) {
  return { nome: input.name.trim(), email: input.email.trim(), perfil: input.role, status: input.status, senha: input.password || undefined };
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

  saveJob: async (input: NewJobInput & { id?: string; status?: JobStatus }) => {
    const vaga = input.id
      ? await apiClient<VagaResponseDTO>(`/vagas/${input.id}`, { method: 'PUT', body: JSON.stringify(vagaBody({ ...input, status: input.status ?? 'aberta' })) })
      : await apiClient<VagaResponseDTO>('/vagas', { method: 'POST', body: JSON.stringify(vagaBody({ ...input, status: 'aberta' })) });
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

  updateProfile: async (profile: CandidateProfile) => {
    const dadosCurriculo = curriculoBody(profile);
    const curriculo = profile.id
      ? await apiClient<CurriculoResponseDTO>(`/curriculos/${profile.id}`, { method: 'PUT', body: JSON.stringify(dadosCurriculo) })
      : await apiClient<CurriculoResponseDTO>('/curriculos', { method: 'POST', body: JSON.stringify(dadosCurriculo) });
    return profileFromResponse(curriculo);
  },

  /** Anexa (ou substitui) o PDF do currículo; devolve o currículo completo com o bloco `arquivo`. */
  uploadResumeFile: async (curriculoId: string, file: File) => {
    const body = new FormData();
    body.append('arquivo', file);
    return profileFromResponse(await apiClient<CurriculoResponseDTO>(`/curriculos/${curriculoId}/arquivo`, { method: 'POST', body }));
  },

  downloadResumeFile: (curriculoId: string) => apiClient<Blob>(`/curriculos/${curriculoId}/arquivo`, {}, { as: 'blob' }),

  getApplications: async () => (await apiClient<CandidaturaResponseDTO[]>('/candidaturas/minhas')).map(applicationFromResponse),

  apply: async (jobId: string) => applicationFromResponse(await apiClient<CandidaturaResponseDTO>('/candidaturas', { method: 'POST', body: JSON.stringify({ vagaId: Number(jobId) }) })),

  getCandidates: async (jobId: string) => (await apiClient<CandidaturaResponseDTO[]>(`/vagas/${jobId}/candidaturas`)).map(candidateFromResponse),

  updateApplicationStatus: async (id: string, status: ApplicationStatus) =>
    applicationFromResponse(await apiClient<CandidaturaResponseDTO>(`/candidaturas/${id}/status`, { method: 'PUT', body: JSON.stringify({ status: statusToApi[status] }) })),

  /** Candidato: os próprios. RH: os dos candidatos das suas vagas. Administrador: todos. */
  getDocuments: async () => (await apiClient<DocumentoResponseDTO[]>('/documentos')).map(documentFromResponse),

  uploadDocument: async (applicationId: string, type: string, file: File) => {
    const body = new FormData();
    body.append('arquivo', file);
    body.append('tipo', type);
    return documentFromResponse(await apiClient<DocumentoResponseDTO>(`/candidaturas/${applicationId}/documentos`, { method: 'POST', body }));
  },

  downloadDocument: (id: string) => apiClient<Blob>(`/documentos/${id}/arquivo`, {}, { as: 'blob' }),

  /** Só o RH e os administradores: os candidatos ficam de fora da lista de Configurações. */
  getUsers: async () => (await apiClient<UsuarioResponseDTO[]>('/usuarios')).filter((item) => item.perfil !== 'candidato').map(userFromResponse),

  createUser: async (input: StaffUserInput) => userFromResponse(await apiClient<UsuarioResponseDTO>('/usuarios', { method: 'POST', body: JSON.stringify(usuarioBody(input)) })),

  updateUser: async (id: string, input: StaffUserInput) => userFromResponse(await apiClient<UsuarioResponseDTO>(`/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(usuarioBody(input)) })),

  deleteUser: (id: string) => apiClient<void>(`/usuarios/${id}`, { method: 'DELETE' }),
};
