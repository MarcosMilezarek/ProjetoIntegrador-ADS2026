import type { LoginResponse } from '@/types/auth';

export type CandidateView = 'jobs' | 'resume' | 'applications' | 'documents' | 'detail' | 'success';

export type HrView = 'jobs' | 'candidates' | 'agenda' | 'documents' | 'employees' | 'settings';

// Trechos do hash por tela: #/candidato/documentos, #/rh/vagas. A tela de sucesso volta para Candidaturas.
const candidatePaths: Record<CandidateView, string> = { jobs: 'vagas', detail: 'vaga', resume: 'curriculo', applications: 'candidaturas', documents: 'documentos', success: 'candidaturas' };

const hrPaths: Record<HrView, string> = { jobs: 'vagas', candidates: 'candidatos', agenda: 'agenda', documents: 'documentos', employees: 'funcionarios', settings: 'configuracoes' };

export function hashOf(mode: 'candidate' | 'hr', candidateView: CandidateView, hrView: HrView, jobId: string) {
  if (mode === 'candidate') return `#/candidato/${candidatePaths[candidateView]}${candidateView === 'detail' && jobId ? `/${jobId}` : ''}`;
  return `#/rh/${hrPaths[hrView]}${hrView === 'candidates' && jobId ? `/${jobId}` : ''}`;
}

/** Lê a tela pedida pelo hash. Se ela não combina com o perfil (ou não existe), cai na tela inicial do perfil. */
export function routeFromHash(perfil: LoginResponse['perfil']): { mode: 'candidate'; view: CandidateView; jobId: string } | { mode: 'hr'; view: HrView; jobId: string } {
  const [, area, path, jobId = ''] = window.location.hash.split('/');
  if (perfil === 'candidato') {
    const view = area === 'candidato' ? (Object.keys(candidatePaths) as CandidateView[]).find((key) => key !== 'success' && candidatePaths[key] === path) : undefined;
    return { mode: 'candidate', view: !view || (view === 'detail' && !jobId) ? 'jobs' : view, jobId };
  }
  const view = area === 'rh' ? (Object.keys(hrPaths) as HrView[]).find((key) => hrPaths[key] === path) : undefined;
  return { mode: 'hr', view: !view || (view === 'settings' && perfil !== 'administrador') ? 'jobs' : view, jobId };
}
