import type {
  Application,
  ApplicationStatus,
  Candidate,
  CandidateDocument,
  CandidateProfile,
  DocumentStatus,
  Job,
  JobStatus,
  NewJobInput,
  NotificationItem,
} from '@/types/domain';
import { restPortalService } from '@/services/rest-portal-service';

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
  education: 'Tecnólogo em Análise e Desenvolvimento de Sistemas - IFRS Campus Erechim (2023 - 2026, cursando)',
  experience: 'Estágio em desenvolvimento web (2025 - atual). React, TypeScript, APIs REST, SQL e atendimento a usuários internos.',
  skills: ['React', 'TypeScript', 'Java', 'SQL', 'Comunicação'],
  resumo: '',
  updatedAt: '02/08/2026',
};

const candidateIdentity = { id: 'candidate-marina', name: 'Marina Souza Andrade', email: 'marina.souza@email.com' };

const seedApplications: Application[] = [
  { id: 'app-java', jobId: 'job-java', candidateId: candidateIdentity.id, submittedAt: '05/08/2026', status: 'reviewing', match: 92 },
  { id: 'app-data', jobId: 'job-data', candidateId: candidateIdentity.id, submittedAt: '02/08/2026', status: 'interview', match: 87 },
  { id: 'app-hr', jobId: 'job-hr', candidateId: candidateIdentity.id, submittedAt: '28/07/2026', status: 'approved', match: 68 },
];

const seedDocuments: CandidateDocument[] = [
  { id: 'doc-rg', label: 'RG (frente e verso)', filename: 'rg_marina_souza.pdf', status: 'approved', updatedAt: '06/08/2026' },
  { id: 'doc-cpf', label: 'CPF', filename: 'cpf_marina_souza.pdf', status: 'approved', updatedAt: '06/08/2026' },
  { id: 'doc-address', label: 'Comprovante de residência', filename: 'comprovante_residencia.pdf', status: 'reviewing', updatedAt: '12/08/2026' },
  { id: 'doc-diploma', label: 'Diploma ou declaração de matrícula', status: 'pending' },
  { id: 'doc-bank', label: 'Dados bancários (comprovante)', status: 'pending' },
];

const seedNotifications: NotificationItem[] = [
  { id: 'n1', title: 'Entrevista agendada', description: 'A entrevista para Analista de Dados Jr. está marcada para 25/08.', read: false },
  { id: 'n2', title: 'Documentos pendentes', description: 'Envie dois documentos para seguir com sua contratação.', read: false },
];

type Store = { jobs: Job[]; profile: CandidateProfile; applications: Application[]; documents: CandidateDocument[]; notifications: NotificationItem[] };
const storeKey = 'vagas-plus-mock-db';

function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T; }
function getStore(): Store {
  const saved = localStorage.getItem(storeKey);
  if (saved) return JSON.parse(saved) as Store;
  const initial = { jobs: seedJobs, profile: seedProfile, applications: seedApplications, documents: seedDocuments, notifications: seedNotifications };
  localStorage.setItem(storeKey, JSON.stringify(initial));
  return clone(initial);
}
function saveStore(store: Store) { localStorage.setItem(storeKey, JSON.stringify(store)); }
function delay<T>(value: T): Promise<T> { return new Promise((resolve) => window.setTimeout(() => resolve(clone(value)), 180)); }

export interface PortalService {
  getJobs(): Promise<Job[]>;
  saveJob(input: NewJobInput & { id?: string; status?: JobStatus }, rhId: string): Promise<Job>;
  closeJob(id: string): Promise<Job>;
  getProfile(usuarioId: string): Promise<CandidateProfile>;
  updateProfile(usuarioId: string, profile: CandidateProfile): Promise<CandidateProfile>;
  getApplications(): Promise<Application[]>;
  apply(jobId: string): Promise<Application>;
  getCandidates(jobId: string): Promise<Candidate[]>;
  updateApplicationStatus(id: string, status: ApplicationStatus): Promise<Application>;
  getDocuments(): Promise<CandidateDocument[]>;
  uploadDocument(id: string, file: File): Promise<CandidateDocument>;
  reviewDocument(id: string, status: Extract<DocumentStatus, 'approved' | 'rejected'>): Promise<CandidateDocument>;
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
  async getProfile() { return delay(getStore().profile); },
  async updateProfile(_usuarioId, profile) { const store = getStore(); store.profile = profile; saveStore(store); return delay(profile); },
  async getApplications() { return delay(getStore().applications); },
  async apply(jobId) {
    const store = getStore();
    const existing = store.applications.find((app) => app.jobId === jobId);
    if (existing) return delay(existing);
    const application: Application = { id: crypto.randomUUID(), jobId, candidateId: candidateIdentity.id, submittedAt: today, status: 'applied', match: 82 };
    store.applications = [application, ...store.applications];
    saveStore(store);
    return delay(application);
  },
  async getCandidates(jobId) {
    const store = getStore();
    const realCandidate: Candidate[] = store.applications.filter((app) => app.jobId === jobId).map((app) => ({ ...candidateIdentity, applicationId: app.id, submittedAt: app.submittedAt, match: app.match, status: app.status }));
    const samples: Candidate[] = [
      { id: 'candidate-rafael', name: 'Rafael Lima Costa', email: 'rafael.lima@email.com', applicationId: 'sample-rafael', submittedAt: '03/08/2026', match: 87, status: 'reviewing' },
      { id: 'candidate-juliana', name: 'Juliana Ferreira Melo', email: 'juliana.melo@email.com', applicationId: 'sample-juliana', submittedAt: '02/08/2026', match: 68, status: 'reviewing' },
      { id: 'candidate-caio', name: 'Caio Henrique Alves', email: 'caio.alves@email.com', applicationId: 'sample-caio', submittedAt: '01/08/2026', match: 61, status: 'interview' },
    ];
    return delay([...realCandidate, ...samples].sort((a, b) => b.match - a.match));
  },
  async updateApplicationStatus(id, status) { const store = getStore(); const application = store.applications.find((item) => item.id === id); if (!application) throw new Error('Candidatura não encontrada'); application.status = status; saveStore(store); return delay(application); },
  async getDocuments() { return delay(getStore().documents); },
  async uploadDocument(id, file) { const store = getStore(); const document = store.documents.find((item) => item.id === id)!; document.filename = file.name; document.updatedAt = today; document.status = 'reviewing'; saveStore(store); return delay(document); },
  async reviewDocument(id, status) { const store = getStore(); const document = store.documents.find((item) => item.id === id)!; document.status = status; document.updatedAt = today; saveStore(store); return delay(document); },
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
    }
  : mockPortalService;
