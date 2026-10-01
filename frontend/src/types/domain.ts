import type { Perfil, StatusUsuario } from '@/types/auth';

export type UserRole = 'candidate' | 'hr';
export type JobStatus = 'rascunho' | 'aberta' | 'encerrada';
export type ApplicationStatus = 'applied' | 'reviewing' | 'interview' | 'approved' | 'rejected' | 'hired' | 'cancelled';

export type Job = {
  id: string;
  title: string;
  city: string;
  workModel: 'Presencial' | 'Híbrido' | 'Remoto';
  contract: 'CLT' | 'Estágio' | 'PJ' | 'Temporário';
  publishedAt: string;
  closesAt?: string;
  status: JobStatus;
  description: string;
  requirements: string[];
};

export type Sexo = 'feminino' | 'masculino' | 'outro' | 'nao_informado';

/** Experiência profissional. Datas em `YYYY-MM-DD`; '' = vazio. `uid` só existe no cliente (chave de lista). */
export type ResumeExperience = {
  uid: string;
  cargo: string;
  empresa: string;
  dataContratacao: string;
  dataDemissao: string;
  trabalhoAtual: boolean;
  descricaoAtividades: string;
};

export type ResumeEducation = {
  uid: string;
  curso: string;
  instituicao: string;
  dataInicio: string;
  /** '' = curso em andamento. */
  dataTermino: string;
};

export type ResumeFile = {
  id: string;
  nomeOriginal: string;
  tamanhoBytes: number;
  enviadoEm: string;
};

export type CandidateProfile = {
  id?: string;
  dataNascimento: string;
  /** Calculada pela API; nunca é enviada. */
  idade?: number;
  sexo: Sexo | '';
  cidade: string;
  uf: string;
  numeroContato: string;
  perfilLinkedin: string;
  skills: string[];
  certificacoes: string;
  resumo: string;
  formacoes: ResumeEducation[];
  experiencias: ResumeExperience[];
  arquivo?: ResumeFile | null;
  updatedAt?: string;
};

export type Application = {
  id: string;
  jobId: string;
  jobTitle: string;
  jobStatus: JobStatus;
  candidateId: string;
  submittedAt: string;
  status: ApplicationStatus;
  notes?: string;
};

export type Candidate = {
  id: string;
  name: string;
  email: string;
  applicationId: string;
  submittedAt: string;
  status: ApplicationStatus;
};

/** Arquivo enviado pelo candidato numa candidatura aprovada. Espelha a resposta de /documentos. */
export type CandidateDocument = {
  id: string;
  applicationId: string;
  jobId: string;
  jobTitle: string;
  candidateId: string;
  candidateName: string;
  /** Texto livre, ex.: "RG" ou "Comprovante de residência". */
  type: string;
  /** Extensão do arquivo: `pdf` ou `docx`. */
  format: string;
  sizeBytes: number;
  sentAt: string;
};

/** Usuário do RH, listado em Configurações. */
export type StaffUser = {
  id: string;
  name: string;
  email: string;
  role: Exclude<Perfil, 'candidato'>;
  status: StatusUsuario;
};

/** Dados enviados ao criar ou editar um usuário do RH. `password` é opcional na edição (vazio mantém a atual). */
export type StaffUserInput = Omit<StaffUser, 'id'> & { password?: string };

export type NotificationItem = {
  id: string;
  title: string;
  description: string;
  read: boolean;
};

export type NewJobInput = Omit<Job, 'id' | 'publishedAt' | 'status'>;
