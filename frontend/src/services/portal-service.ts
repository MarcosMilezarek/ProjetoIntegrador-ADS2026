import type {
  Application,
  ApplicationStatus,
  Candidate,
  CandidateDocument,
  CandidateProfile,
  Job,
  JobStatus,
  NewJobInput,
  NotificationItem,
  StaffUser,
  StaffUserInput,
} from '@/types/domain';
import { emptyProfile, restPortalService } from '@/services/rest-portal-service';

const today = '21/08/2026';

const seedJobs: Job[] = [
  { id: 'job-java', title: 'Desenvolvedor(a) Back-end Java', city: 'Erechim, RS', workModel: 'Híbrido', contract: 'CLT', publishedAt: '02/08/2026', closesAt: '2026-09-02', status: 'aberta', description: 'Atue na squad responsável pelo Sistema de Gerenciamento de Vagas, construindo APIs REST, integrações e modelos de dados para os produtos internos do RH.', requirements: ['Experiência com Java e Spring Boot.', 'Conhecimento em banco de dados relacional.', 'Vivência com Git e metodologias ágeis.', 'Desejável conhecimento em JWT e APIs RESTful.'] },
  { id: 'job-react', title: 'Desenvolvedora Front-end React', city: 'Remoto', workModel: 'Remoto', contract: 'CLT', publishedAt: '04/08/2026', closesAt: '2026-09-12', status: 'aberta', description: 'Construa experiências acessíveis e consistentes para produtos de gestão de pessoas.', requirements: ['React e TypeScript.', 'Consumo de APIs REST.', 'Conhecimento de design systems.'] },
  { id: 'job-data', title: 'Analista de Dados Jr.', city: 'Erechim, RS', workModel: 'Híbrido', contract: 'Estágio', publishedAt: '28/07/2026', closesAt: '2026-08-26', status: 'aberta', description: 'Apoie a organização de dados e a criação de indicadores para as áreas de negócio.', requirements: ['SQL básico.', 'Interesse em visualização de dados.', 'Organização e comunicação.'] },
  { id: 'job-hr', title: 'Analista de Recursos Humanos', city: 'Erechim, RS', workModel: 'Presencial', contract: 'CLT', publishedAt: '24/07/2026', closesAt: '2026-08-23', status: 'aberta', description: 'Conduza processos seletivos e apoie as rotinas de pessoas.', requirements: ['Experiência com recrutamento.', 'Boa comunicação.', 'Organização de processos.'] },
  { id: 'job-support', title: 'Analista de Suporte Técnico', city: 'Erechim, RS', workModel: 'Presencial', contract: 'CLT', publishedAt: '30/06/2026', closesAt: '2026-07-30', status: 'encerrada', description: 'Atenda usuários internos e mantenha o ambiente tecnológico operacional.', requirements: ['Conhecimento básico de redes.', 'Experiência com atendimento.'] },
];

const seedProfile: CandidateProfile = {
  id: 'curriculo-marina',
  dataNascimento: '2000-05-14',
  idade: 26,
  sexo: 'feminino',
  cidade: 'Erechim',
  uf: 'RS',
  numeroContato: '54999990001',
  perfilLinkedin: '',
  skills: ['React', 'TypeScript', 'Java', 'SQL', 'Comunicação'],
  certificacoes: '',
  resumo: '',
  formacoes: [{ uid: 'seed-form-1', curso: 'Análise e Desenvolvimento de Sistemas', instituicao: 'IFRS Campus Erechim', dataInicio: '2023-03-01', dataTermino: '' }],
  experiencias: [{ uid: 'seed-exp-1', cargo: 'Estagiária de desenvolvimento web', empresa: 'Empresa Exemplo', dataContratacao: '2025-02-01', dataDemissao: '', trabalhoAtual: true, descricaoAtividades: 'React, TypeScript, APIs REST, SQL e atendimento a usuários internos.' }],
  arquivo: null,
  updatedAt: '02/08/2026',
};

const candidateIdentity = { id: 'candidate-marina', name: 'Marina Souza Andrade', email: 'marina.souza@email.com' };

const seedApplications: Application[] = [
  { id: 'app-java', jobId: 'job-java', jobTitle: 'Desenvolvedor(a) Back-end Java', jobStatus: 'aberta', candidateId: candidateIdentity.id, submittedAt: '05/08/2026', status: 'reviewing' },
  { id: 'app-data', jobId: 'job-data', jobTitle: 'Analista de Dados Jr.', jobStatus: 'aberta', candidateId: candidateIdentity.id, submittedAt: '02/08/2026', status: 'interview' },
  { id: 'app-hr', jobId: 'job-hr', jobTitle: 'Analista de Recursos Humanos', jobStatus: 'aberta', candidateId: candidateIdentity.id, submittedAt: '28/07/2026', status: 'approved' },
];

const seedDocuments: CandidateDocument[] = [
  { id: 'doc-rg', applicationId: 'app-hr', jobId: 'job-hr', jobTitle: 'Analista de Recursos Humanos', candidateId: candidateIdentity.id, candidateName: candidateIdentity.name, type: 'RG', format: 'pdf', sizeBytes: 184320, sentAt: '06/08/2026' },
  { id: 'doc-cpf', applicationId: 'app-hr', jobId: 'job-hr', jobTitle: 'Analista de Recursos Humanos', candidateId: candidateIdentity.id, candidateName: candidateIdentity.name, type: 'CPF', format: 'pdf', sizeBytes: 90112, sentAt: '06/08/2026' },
];

const seedUsers: StaffUser[] = [
  { id: 'user-admin', name: 'Administrador Geral', email: 'admin@exemplo.test', role: 'administrador', status: 'ativo' },
  { id: 'user-rita', name: 'Rita Nogueira', email: 'rita.rh@exemplo.test', role: 'rh', status: 'ativo' },
  { id: 'user-paulo', name: 'Paulo Fontes', email: 'paulo.rh@exemplo.test', role: 'rh', status: 'ativo' },
];

const seedNotifications: NotificationItem[] = [
  { id: 'n1', title: 'Entrevista agendada', description: 'A entrevista para Analista de Dados Jr. está marcada para 25/08.', read: false },
  { id: 'n2', title: 'Documentos pendentes', description: 'Envie dois documentos para seguir com sua contratação.', read: false },
];

type Store = { jobs: Job[]; profile: CandidateProfile; applications: Application[]; documents: CandidateDocument[]; notifications: NotificationItem[]; users: StaffUser[] };
const storeKey = 'vagas-plus-mock-db-v2';

function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T; }
function getStore(): Store {
  const saved = localStorage.getItem(storeKey);
  if (saved) return JSON.parse(saved) as Store;
  const initial = { jobs: seedJobs, profile: seedProfile, applications: seedApplications, documents: seedDocuments, notifications: seedNotifications, users: seedUsers };
  localStorage.setItem(storeKey, JSON.stringify(initial));
  return clone(initial);
}
function saveStore(store: Store) { localStorage.setItem(storeKey, JSON.stringify(store)); }
function delay<T>(value: T): Promise<T> { return new Promise((resolve) => window.setTimeout(() => resolve(clone(value)), 180)); }

export interface PortalService {
  getJobs(): Promise<Job[]>;
  saveJob(input: NewJobInput & { id?: string; status?: JobStatus }): Promise<Job>;
  closeJob(id: string): Promise<Job>;
  getProfile(usuarioId: string): Promise<CandidateProfile>;
  updateProfile(profile: CandidateProfile): Promise<CandidateProfile>;
  uploadResumeFile(curriculoId: string, file: File): Promise<CandidateProfile>;
  downloadResumeFile(curriculoId: string): Promise<Blob>;
  getApplications(): Promise<Application[]>;
  apply(jobId: string): Promise<Application>;
  getCandidates(jobId: string): Promise<Candidate[]>;
  updateApplicationStatus(id: string, status: ApplicationStatus): Promise<Application>;
  getDocuments(): Promise<CandidateDocument[]>;
  uploadDocument(applicationId: string, type: string, file: File): Promise<CandidateDocument>;
  downloadDocument(id: string): Promise<Blob>;
  getUsers(): Promise<StaffUser[]>;
  createUser(input: StaffUserInput): Promise<StaffUser>;
  updateUser(id: string, input: StaffUserInput): Promise<StaffUser>;
  deleteUser(id: string): Promise<void>;
  getNotifications(): Promise<NotificationItem[]>;
  markNotificationsRead(): Promise<void>;
}

export const mockPortalService: PortalService = {
  async getJobs() { return delay(getStore().jobs); },
  async saveJob(input) {
    const store = getStore();
    const existing = input.id ? store.jobs.find((item) => item.id === input.id) : undefined;
    const job: Job = existing
      ? { ...existing, ...input, status: input.status ?? existing.status }
      : { ...input, id: crypto.randomUUID(), publishedAt: today, status: 'aberta' };
    store.jobs = existing ? store.jobs.map((item) => item.id === job.id ? job : item) : [job, ...store.jobs];
    saveStore(store);
    return delay(job);
  },
  async closeJob(id) {
    const store = getStore();
    const job = store.jobs.find((item) => item.id === id);
    if (!job) throw new Error('Vaga não encontrada');
    job.status = 'encerrada';
    saveStore(store);
    return delay(job);
  },
  // O banco local pode ter sido gravado no formato antigo do currículo; completa o que faltar.
  async getProfile() { return delay({ ...emptyProfile, ...getStore().profile, formacoes: getStore().profile.formacoes ?? [], experiencias: getStore().profile.experiencias ?? [] }); },
  async updateProfile(profile) {
    const store = getStore();
    store.profile = { ...profile, id: profile.id ?? 'curriculo-local', updatedAt: today };
    saveStore(store);
    return delay(store.profile);
  },
  async uploadResumeFile(_curriculoId, file) {
    const store = getStore();
    store.profile = { ...store.profile, arquivo: { id: 'arquivo-local', nomeOriginal: file.name, tamanhoBytes: file.size, enviadoEm: new Date().toISOString() } };
    saveStore(store);
    return delay(store.profile);
  },
  async downloadResumeFile() { return new Blob(['Arquivo de demonstração'], { type: 'application/pdf' }); },
  async getApplications() { return delay(getStore().applications); },
  async apply(jobId) {
    const store = getStore();
    const existing = store.applications.find((app) => app.jobId === jobId);
    if (existing) return delay(existing);
    const job = store.jobs.find((item) => item.id === jobId);
    if (!job) throw new Error('Vaga não encontrada');
    const application: Application = { id: crypto.randomUUID(), jobId, jobTitle: job.title, jobStatus: job.status, candidateId: candidateIdentity.id, submittedAt: today, status: 'applied' };
    store.applications = [application, ...store.applications];
    saveStore(store);
    return delay(application);
  },
  async getCandidates(jobId) {
    const store = getStore();
    return delay(store.applications.filter((app) => app.jobId === jobId).map((app): Candidate => ({ ...candidateIdentity, applicationId: app.id, submittedAt: app.submittedAt, status: app.status })));
  },
  async updateApplicationStatus(id, status) { const store = getStore(); const application = store.applications.find((item) => item.id === id); if (!application) throw new Error('Candidatura não encontrada'); application.status = status; saveStore(store); return delay(application); },
  async getDocuments() { return delay(getStore().documents); },
  async uploadDocument(applicationId, type, file) {
    const store = getStore();
    const application = store.applications.find((item) => item.id === applicationId);
    if (!application) throw new Error('Candidatura não encontrada.');
    if (application.status !== 'approved' && application.status !== 'hired') throw new Error('Os documentos só podem ser enviados depois da aprovação na vaga.');
    const document: CandidateDocument = { id: crypto.randomUUID(), applicationId, jobId: application.jobId, jobTitle: application.jobTitle, candidateId: candidateIdentity.id, candidateName: candidateIdentity.name, type, format: file.name.split('.').pop()?.toLowerCase() ?? 'pdf', sizeBytes: file.size, sentAt: today };
    store.documents = [document, ...store.documents];
    saveStore(store);
    return delay(document);
  },
  async downloadDocument() { return new Blob(['Arquivo de demonstração'], { type: 'application/pdf' }); },
  async getUsers() { return delay(getStore().users); },
  async createUser(input) {
    const store = getStore();
    if (store.users.some((item) => item.email.toLowerCase() === input.email.trim().toLowerCase())) throw new Error('Já existe um usuário com este e-mail.');
    const user: StaffUser = { id: crypto.randomUUID(), name: input.name.trim(), email: input.email.trim(), role: input.role, status: input.status };
    store.users = [...store.users, user];
    saveStore(store);
    return delay(user);
  },
  async updateUser(id, input) {
    const store = getStore();
    if (!store.users.some((item) => item.id === id)) throw new Error('Usuário não encontrado.');
    const user: StaffUser = { id, name: input.name.trim(), email: input.email.trim(), role: input.role, status: input.status };
    store.users = store.users.map((item) => item.id === id ? user : item);
    saveStore(store);
    return delay(user);
  },
  async deleteUser(id) {
    const store = getStore();
    store.users = store.users.filter((item) => item.id !== id);
    saveStore(store);
    return delay(undefined);
  },
  async getNotifications() { return delay(getStore().notifications); },
  async markNotificationsRead() { const store = getStore(); store.notifications = store.notifications.map((notification) => ({ ...notification, read: true })); saveStore(store); return delay(undefined); },
};

export const portalService: PortalService = import.meta.env.VITE_USE_MOCK_API === 'false'
  ? {
      ...mockPortalService,
      getJobs: restPortalService.getJobs,
      saveJob: restPortalService.saveJob,
      closeJob: restPortalService.closeJob,
      getProfile: restPortalService.getProfile,
      updateProfile: restPortalService.updateProfile,
      uploadResumeFile: restPortalService.uploadResumeFile,
      downloadResumeFile: restPortalService.downloadResumeFile,
      getApplications: restPortalService.getApplications,
      apply: restPortalService.apply,
      getCandidates: restPortalService.getCandidates,
      updateApplicationStatus: restPortalService.updateApplicationStatus,
      getDocuments: restPortalService.getDocuments,
      uploadDocument: restPortalService.uploadDocument,
      downloadDocument: restPortalService.downloadDocument,
      getUsers: restPortalService.getUsers,
      createUser: restPortalService.createUser,
      updateUser: restPortalService.updateUser,
      deleteUser: restPortalService.deleteUser,
    }
  : mockPortalService;
