export type UserRole = 'candidate' | 'hr';
export type JobStatus = 'open' | 'closing' | 'closed';
export type ApplicationStatus = 'applied' | 'reviewing' | 'interview' | 'approved' | 'rejected';
export type DocumentStatus = 'pending' | 'reviewing' | 'approved' | 'rejected';

export type Job = {
  id: string;
  title: string;
  department: string;
  city: string;
  workModel: 'Presencial' | 'Híbrido' | 'Remoto';
  contract: 'CLT' | 'Estágio' | 'PJ';
  publishedAt: string;
  closesAt: string;
  applicants: number;
  status: JobStatus;
  tags: string[];
  description: string;
  requirements: string[];
  benefits: string[];
};

export type CandidateProfile = {
  id: string;
  name: string;
  email: string;
  phone: string;
  city: string;
  education: string;
  experience: string;
  skills: string[];
  completion: number;
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

export type Candidate = CandidateProfile & {
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

export type NewJobInput = Omit<Job, 'id' | 'applicants' | 'publishedAt' | 'status'>;
