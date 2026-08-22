import type {
  Application,
  ApplicationStatus,
  Candidate,
  CandidateDocument,
  CandidateProfile,
  DocumentStatus,
  Job,
  NewJobInput,
  NotificationItem,
} from '@/types/domain';
import { restPortalService } from '@/services/rest-portal-service';

const today = '21/08/2026';

const seedJobs: Job[] = [
  { id: 'job-java', title: 'Desenvolvedor(a) Back-end Java', department: 'Tecnologia', city: 'Erechim, RS', workModel: 'Híbrido', contract: 'CLT', publishedAt: '02/08/2026', closesAt: '02/09/2026', applicants: 14, status: 'open', tags: ['Java', 'Spring Boot', 'SQL'], description: 'Atue na squad responsável pelo Sistema de Gerenciamento de Vagas, construindo APIs REST, integrações e modelos de dados para os produtos internos do RH.', requirements: ['Experiência com Java e Spring Boot.', 'Conhecimento em banco de dados relacional.', 'Vivência com Git e metodologias ágeis.', 'Desejável conhecimento em JWT e APIs RESTful.'], benefits: ['Vale-refeição', 'Plano de saúde', 'Horário flexível'] },
  { id: 'job-react', title: 'Desenvolvedora Front-end React', department: 'Tecnologia', city: 'Remoto', workModel: 'Remoto', contract: 'CLT', publishedAt: '04/08/2026', closesAt: '12/09/2026', applicants: 9, status: 'open', tags: ['React', 'TypeScript', 'UI'], description: 'Construa experiências acessíveis e consistentes para produtos de gestão de pessoas.', requirements: ['React e TypeScript.', 'Consumo de APIs REST.', 'Conhecimento de design systems.'], benefits: ['Trabalho remoto', 'Plano de saúde', 'Auxílio educação'] },
  { id: 'job-data', title: 'Analista de Dados Jr.', department: 'Dados', city: 'Erechim, RS', workModel: 'Híbrido', contract: 'Estágio', publishedAt: '28/07/2026', closesAt: '26/08/2026', applicants: 21, status: 'open', tags: ['Power BI', 'Python', 'SQL'], description: 'Apoie a organização de dados e a criação de indicadores para as áreas de negócio.', requirements: ['SQL básico.', 'Interesse em visualização de dados.', 'Organização e comunicação.'], benefits: ['Bolsa estágio', 'Vale-transporte', 'Mentoria'] },
  { id: 'job-hr', title: 'Analista de Recursos Humanos', department: 'Recursos Humanos', city: 'Erechim, RS', workModel: 'Presencial', contract: 'CLT', publishedAt: '24/07/2026', closesAt: '23/08/2026', applicants: 7, status: 'closing', tags: ['Recrutamento', 'Comunicação'], description: 'Conduza processos seletivos e apoie as rotinas de pessoas.', requirements: ['Experiência com recrutamento.', 'Boa comunicação.', 'Organização de processos.'], benefits: ['Vale-refeição', 'Plano de saúde'] },
  { id: 'job-support', title: 'Analista de Suporte Técnico', department: 'Infraestrutura', city: 'Erechim, RS', workModel: 'Presencial', contract: 'CLT', publishedAt: '30/06/2026', closesAt: '30/07/2026', applicants: 18, status: 'closed', tags: ['Redes', 'Atendimento'], description: 'Atenda usuários internos e mantenha o ambiente tecnológico operacional.', requirements: ['Conhecimento básico de redes.', 'Experiência com atendimento.'], benefits: ['Vale-refeição', 'Plano de saúde'] },
];

const seedProfile: CandidateProfile = {
  id: 'candidate-marina', name: 'Marina Souza Andrade', email: 'marina.souza@email.com', phone: '(54) 99911-2233', city: 'Erechim / RS', education: 'Tecnólogo em Análise e Desenvolvimento de Sistemas - IFRS Campus Erechim (2023 - 2026, cursando)', experience: 'Estágio em desenvolvimento web (2025 - atual). React, TypeScript, APIs REST, SQL e atendimento a usuários internos.', skills: ['React', 'TypeScript', 'Java', 'SQL', 'Comunicação'], completion: 82,
};

const seedApplications: Application[] = [
  { id: 'app-java', jobId: 'job-java', candidateId: 'candidate-marina', submittedAt: '05/08/2026', status: 'reviewing', match: 92 },
  { id: 'app-data', jobId: 'job-data', candidateId: 'candidate-marina', submittedAt: '02/08/2026', status: 'interview', match: 87 },
  { id: 'app-hr', jobId: 'job-hr', candidateId: 'candidate-marina', submittedAt: '28/07/2026', status: 'approved', match: 68 },
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
  saveJob(input: NewJobInput & { id?: string }): Promise<Job>;
  deleteJob(id: string): Promise<void>;
  getProfile(): Promise<CandidateProfile>;
  updateProfile(profile: CandidateProfile): Promise<CandidateProfile>;
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
    const job: Job = input.id
      ? { ...store.jobs.find((item) => item.id === input.id)!, ...input }
      : { ...input, id: crypto.randomUUID(), applicants: 0, publishedAt: today, status: 'open' };
    store.jobs = input.id ? store.jobs.map((item) => item.id === input.id ? job : item) : [job, ...store.jobs];
    saveStore(store);
    return delay(job);
  },
  async deleteJob(id) { const store = getStore(); store.jobs = store.jobs.filter((job) => job.id !== id); saveStore(store); return delay(undefined); },
  async getProfile() { return delay(getStore().profile); },
  async updateProfile(profile) { const store = getStore(); store.profile = profile; saveStore(store); return delay(profile); },
  async getApplications() { return delay(getStore().applications); },
  async apply(jobId) {
    const store = getStore();
    const existing = store.applications.find((app) => app.jobId === jobId);
    if (existing) return delay(existing);
    const application: Application = { id: crypto.randomUUID(), jobId, candidateId: store.profile.id, submittedAt: today, status: 'applied', match: 82 };
    store.applications = [application, ...store.applications];
    store.jobs = store.jobs.map((job) => job.id === jobId ? { ...job, applicants: job.applicants + 1 } : job);
    saveStore(store);
    return delay(application);
  },
  async getCandidates(jobId) {
    const store = getStore();
    const realCandidate = store.applications.filter((app) => app.jobId === jobId).map((app) => ({ ...store.profile, applicationId: app.id, submittedAt: app.submittedAt, match: app.match, status: app.status }));
    const samples: Candidate[] = [
      { ...store.profile, id: 'candidate-rafael', name: 'Rafael Lima Costa', email: 'rafael.lima@email.com', applicationId: 'sample-rafael', submittedAt: '03/08/2026', match: 87, status: 'reviewing' },
      { ...store.profile, id: 'candidate-juliana', name: 'Juliana Ferreira Melo', email: 'juliana.melo@email.com', applicationId: 'sample-juliana', submittedAt: '02/08/2026', match: 68, status: 'reviewing' },
      { ...store.profile, id: 'candidate-caio', name: 'Caio Henrique Alves', email: 'caio.alves@email.com', applicationId: 'sample-caio', submittedAt: '01/08/2026', match: 61, status: 'interview' },
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

export const portalService = import.meta.env.VITE_USE_MOCK_API === 'false'
  ? restPortalService
  : mockPortalService;
