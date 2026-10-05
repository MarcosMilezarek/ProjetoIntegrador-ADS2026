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
  requirements: JobRequirements;
};

/** Requisitos da vaga em três níveis, um item por linha. */
export type JobRequirements = { required: string[]; desirable: string[]; differential: string[] };

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

/** O candidato confirmou ou não a presença na entrevista marcada. Os valores são os da API. */
export type InterviewPresence = 'pendente' | 'confirmado';

export type Application = {
  id: string;
  jobId: string;
  jobTitle: string;
  jobStatus: JobStatus;
  candidateId: string;
  submittedAt: string;
  status: ApplicationStatus;
  /** Data e hora da entrevista marcada pelo RH, em UTC (ISO 8601). Mostrar no horário de Brasília. */
  interviewAt?: string;
  /** Só existe com entrevista marcada; remarcar volta para `pendente`. */
  presence?: InterviewPresence;
  /** Instante em UTC; ausente enquanto a presença está pendente. */
  presenceConfirmedAt?: string;
  notes?: string;
};

export type AnaliseStatus = 'PENDENTE' | 'CONCLUIDA' | 'FALHA';
/** Triagem do currículo por IA, só na lista de inscritos do RH. Recomendação, não decisão. `aderencia` (0 a 100) só vem com CONCLUIDA. */
export type AnaliseCandidatura = { status: AnaliseStatus; aderencia: number | null; pontosPositivos: string[]; pontosNegativos: string[] };

export type Candidate = {
  id: string;
  name: string;
  email: string;
  applicationId: string;
  jobTitle: string;
  submittedAt: string;
  status: ApplicationStatus;
  interviewAt?: string;
  presence?: InterviewPresence;
  /** Nulo em candidatura anterior à triagem por IA. */
  analise: AnaliseCandidatura | null;
};

/** Revisão do RH sobre um documento enviado. Reenviar o mesmo tipo volta para `pendente`. */
export type DocumentStatus = 'pendente' | 'aprovado' | 'recusado';

/** Arquivo enviado pelo candidato numa candidatura aprovada. Espelha a resposta de /documentos. */
export type CandidateDocument = {
  id: string;
  applicationId: string;
  jobId: string;
  jobTitle: string;
  candidateId: string;
  candidateName: string;
  /** Nome para exibir, ex.: "Comprovante de residência". */
  type: string;
  /** Código da lista de tipos (`rg`); nulo em envio antigo de texto livre que não correspondeu a nenhum tipo. */
  typeCode: string | null;
  /** Extensão do arquivo: `pdf` ou `docx`. */
  format: string;
  status: DocumentStatus;
  sizeBytes: number;
  sentAt: string;
};

/** Item da lista fechada de documentos de contratação (`GET /documentos/tipos`). */
export type DocumentType = {
  code: string;
  name: string;
  required: boolean;
  /** Quando o documento passa a ser exigido; nulo nos obrigatórios. */
  condition: string | null;
};

/** Quadro de documentos de uma candidatura: o que foi enviado e o que falta. */
export type DocumentBoard = {
  applicationId: string;
  candidateName: string;
  jobTitle: string;
  sentRequired: number;
  totalRequired: number;
  sent: CandidateDocument[];
  pending: DocumentType[];
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

/** Funcionário: candidato contratado. Inativar não apaga nada, o registro continua na lista. */
export type Employee = {
  id: string;
  applicationId: string;
  candidateId: string;
  candidateName: string;
  candidateEmail: string;
  jobId: string;
  jobTitle: string;
  status: 'ativo' | 'inativo';
  /** Só a data, `dd/MM/yyyy`. */
  hiredAt: string;
};

export type EmployeeProfile = { employee: Employee; documents: CandidateDocument[] };

/** `candidatura` avisa sobre uma candidatura; `nova_vaga`, sobre uma vaga publicada. */
export type NotificationType = 'candidatura' | 'nova_vaga';

export type NotificationItem = {
  id: string;
  type: NotificationType;
  title: string;
  description: string;
  read: boolean;
};

/** O que chega pelo fluxo de notificações: a conexão abriu (recarregue a lista) ou há uma notificação nova. */
export type NotificationEvent = { type: 'connected' } | { type: 'notification'; item: NotificationItem };

export type NewJobInput = Omit<Job, 'id' | 'publishedAt' | 'status'>;
