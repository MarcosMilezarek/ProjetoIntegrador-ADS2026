import { apiClient } from '@/lib/api-client';
import type { PortalService } from '@/services/portal-service';
import type { Application, ApplicationStatus, Candidate, CandidateDocument, CandidateProfile, DocumentStatus, Job, NewJobInput, NotificationItem } from '@/types/domain';

// Contract expected from the future backend. Keep endpoint names here so UI code stays API-agnostic.
export const restPortalService: PortalService = {
  getJobs: () => apiClient<Job[]>('/jobs'),
  saveJob: (input) => input.id
    ? apiClient<Job>(`/jobs/${input.id}`, { method: 'PUT', body: JSON.stringify(input) })
    : apiClient<Job>('/jobs', { method: 'POST', body: JSON.stringify(input) }),
  deleteJob: (id) => apiClient<void>(`/jobs/${id}`, { method: 'DELETE' }),
  getProfile: () => apiClient<CandidateProfile>('/candidate/profile'),
  updateProfile: (profile) => apiClient<CandidateProfile>('/candidate/profile', { method: 'PUT', body: JSON.stringify(profile) }),
  getApplications: () => apiClient<Application[]>('/candidate/applications'),
  apply: (jobId) => apiClient<Application>(`/jobs/${jobId}/applications`, { method: 'POST' }),
  getCandidates: (jobId) => apiClient<Candidate[]>(`/jobs/${jobId}/candidates`),
  updateApplicationStatus: (id, status) => apiClient<Application>(`/applications/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  getDocuments: () => apiClient<CandidateDocument[]>('/candidate/documents'),
  uploadDocument: (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient<CandidateDocument>(`/candidate/documents/${id}/upload`, { method: 'POST', body: formData });
  },
  reviewDocument: (id, status: Extract<DocumentStatus, 'approved' | 'rejected'>) => apiClient<CandidateDocument>(`/documents/${id}/review`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  getNotifications: () => apiClient<NotificationItem[]>('/notifications'),
  markNotificationsRead: () => apiClient<void>('/notifications/read', { method: 'POST' }),
};
