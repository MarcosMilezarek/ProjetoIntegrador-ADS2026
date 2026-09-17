export type UserRole = 'candidate' | 'hr';
export type JobStatus = 'rascunho' | 'aberta' | 'encerrada';
export type ApplicationStatus = 'applied' | 'reviewing' | 'interview' | 'approved' | 'rejected';
export type DocumentStatus = 'pending' | 'reviewing' | 'approved' | 'rejected';

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

export type CandidateProfile = {
  id?: string;
  education: string;
  experience: string;
  skills: string[];
  resumo: string;
  updatedAt?: string;
};

export type Application = {
  id: string;
  jobId: string;
  candidateId: string;
  submittedAt: string;
  status: ApplicationStatus;
  match: number;
  notes?: string;
};

export type Candidate = {
  id: string;
  name: string;
  email: string;
  applicationId: string;
  submittedAt: string;
  match: number;
  status: ApplicationStatus;
};

export type CandidateDocument = {
  id: string;
  label: string;
  filename?: string;
  status: DocumentStatus;
  updatedAt?: string;
  reviewNote?: string;
};

export type NotificationItem = {
  id: string;
  title: string;
  description: string;
  read: boolean;
};

export type NewJobInput = Omit<Job, 'id' | 'publishedAt' | 'status'>;
