import { Fragment, useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import type { ChangeEvent, ComponentProps, FormEvent, ReactNode } from 'react';
import { Ban, Bell, BriefcaseBusiness, CalendarDays, Check, ChevronLeft, ChevronRight, CircleAlert, ClipboardList, Download, Eye, EyeOff, FileText, Info, LogOut, Inbox, MapPin, Monitor, Moon, MoreHorizontal, Pencil, Plus, Search, Settings, Sun, Trash2, Upload, UsersRound, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { portalService } from '@/services/portal-service';
import { ApiError, setAuthToken, setUnauthorizedHandler } from '@/lib/api-client';
import { formatDate, formatDateTime } from '@/lib/utils';
import type { Application, ApplicationStatus, Candidate, CandidateDocument, CandidateProfile, DocumentBoard, DocumentType, Job, NewJobInput, NotificationItem, ResumeEducation, ResumeExperience, Sexo, StaffUser, StaffUserInput } from '@/types/domain';
import { authService } from './types/auth-service';
import type { LoginResponse, StatusUsuario } from './types/auth';

type CandidateView = 'jobs' | 'resume' | 'applications' | 'documents' | 'detail' | 'success';
type HrView = 'jobs' | 'candidates' | 'documents' | 'settings';
type AppMode = 'login' | 'signup' | 'candidate' | 'hr';
type Notice = { tone: 'ok' | 'error'; text: string } | null;
type Tone = 'green' | 'yellow' | 'red' | 'outline' | 'dashed' | undefined;
type JobInput = NewJobInput & { id?: string; status?: Job['status'] };

const appStatus: Record<ApplicationStatus, string> = { applied: 'Inscrito', reviewing: 'Em análise', interview: 'Entrevista', approved: 'Aprovado', rejected: 'Não selecionado', hired: 'Contratado', cancelled: 'Cancelado' };
const appTone: Record<ApplicationStatus, Tone> = { applied: undefined, reviewing: 'yellow', interview: 'outline', approved: 'green', rejected: 'red', hired: 'green', cancelled: 'dashed' };
const userStatus: Record<StatusUsuario, string> = { ativo: 'Ativo', inativo: 'Inativo', bloqueado: 'Bloqueado' };
const userTone: Record<StatusUsuario, Tone> = { ativo: 'green', inativo: 'dashed', bloqueado: 'red' };
const staffRoles: Record<StaffUser['role'], string> = { rh: 'RH', administrador: 'Administrador' };
const jobStatus = { rascunho: 'Rascunho', aberta: 'Aberta', encerrada: 'Encerrada' } as const;
const jobTone: Record<Job['status'], Tone> = { rascunho: 'dashed', aberta: 'green', encerrada: 'red' };
const stageNames = ['Inscrito', 'Em análise', 'Entrevista', 'Aprovado'];
const nextStep: Record<ApplicationStatus, string> = {
  applied: 'Próximo passo: o RH vai revisar seu currículo.',
  reviewing: 'Próximo passo: o RH está avaliando seu currículo.',
  interview: 'Próximo passo: aguarde o contato do RH para a entrevista.',
  approved: 'Você foi aprovado. Envie os documentos de contratação para seguir com a admissão.',
  rejected: 'Desta vez você não seguiu no processo. Seu currículo continua salvo para as próximas vagas.',
  hired: 'Você foi contratado. Bem-vindo à equipe!',
  cancelled: 'Esta candidatura foi cancelada. Seu currículo continua salvo para as próximas vagas.',
};
const maxUploadBytes = 5 * 1024 * 1024;

/* ---------- Tema: claro, escuro ou seguindo o sistema ---------- */
type ThemePref = 'system' | 'light' | 'dark';
const themeKey = 'teamup-theme';
const themeListeners = new Set<() => void>();
const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');
let themePref: ThemePref = (() => { try { const saved = localStorage.getItem(themeKey); return saved === 'light' || saved === 'dark' ? saved : 'system'; } catch { return 'system'; } })();
function applyTheme() {
  const dark = themePref === 'dark' || (themePref === 'system' && darkQuery.matches);
  document.documentElement.classList.toggle('dark', dark);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#06100b' : '#ffffff');
}
function setThemePref(next: ThemePref) {
  themePref = next;
  try { if (next === 'system') localStorage.removeItem(themeKey); else localStorage.setItem(themeKey, next); } catch { /* preferência só não fica salva */ }
  applyTheme();
  themeListeners.forEach((listener) => listener());
}
darkQuery.addEventListener('change', () => { if (themePref === 'system') applyTheme(); });
applyTheme();
function useThemePref() { return useSyncExternalStore((listener) => { themeListeners.add(listener); return () => { themeListeners.delete(listener); }; }, () => themePref); }

/* ---------- Sessão salva e tela na URL ---------- */
const sessionKey = 'upteam.sessao';
function readSession(): LoginResponse | null {
  try {
    const saved = JSON.parse(localStorage.getItem(sessionKey) ?? 'null');
    const known = saved && typeof saved.token === 'string' && typeof saved.id === 'number' && ['candidato', 'rh', 'administrador'].includes(saved.perfil);
    return known ? saved as LoginResponse : null;
  } catch { return null; }
}
function writeSession(account: LoginResponse | null) {
  try {
    if (account) localStorage.setItem(sessionKey, JSON.stringify({ token: account.token, id: account.id, nome: account.nome, email: account.email, perfil: account.perfil }));
    else localStorage.removeItem(sessionKey);
  } catch { /* sem armazenamento, a sessão só não sobrevive ao F5 */ }
}

// Trechos do hash por tela: #/candidato/documentos, #/rh/vagas. A tela de sucesso volta para Candidaturas.
const candidatePaths: Record<CandidateView, string> = { jobs: 'vagas', detail: 'vaga', resume: 'curriculo', applications: 'candidaturas', documents: 'documentos', success: 'candidaturas' };
const hrPaths: Record<HrView, string> = { jobs: 'vagas', candidates: 'candidatos', documents: 'documentos', settings: 'configuracoes' };

function hashOf(mode: 'candidate' | 'hr', candidateView: CandidateView, hrView: HrView, jobId: string) {
  if (mode === 'candidate') return `#/candidato/${candidatePaths[candidateView]}${candidateView === 'detail' && jobId ? `/${jobId}` : ''}`;
  return `#/rh/${hrPaths[hrView]}${hrView === 'candidates' && jobId ? `/${jobId}` : ''}`;
}

/** Lê a tela pedida pelo hash. Se ela não combina com o perfil (ou não existe), cai na tela inicial do perfil. */
function routeFromHash(perfil: LoginResponse['perfil']): { mode: 'candidate'; view: CandidateView; jobId: string } | { mode: 'hr'; view: HrView; jobId: string } {
  const [, area, path, jobId = ''] = window.location.hash.split('/');
  if (perfil === 'candidato') {
    const view = area === 'candidato' ? (Object.keys(candidatePaths) as CandidateView[]).find((key) => key !== 'success' && candidatePaths[key] === path) : undefined;
    return { mode: 'candidate', view: !view || (view === 'detail' && !jobId) ? 'jobs' : view, jobId };
  }
  const view = area === 'rh' ? (Object.keys(hrPaths) as HrView[]).find((key) => hrPaths[key] === path) : undefined;
  return { mode: 'hr', view: !view || (view === 'settings' && perfil !== 'administrador') ? 'jobs' : view, jobId };
}

function App() {
  const [mode, setMode] = useState<AppMode>('login');
  const [candidateView, setCandidateView] = useState<CandidateView>('jobs');
  const [hrView, setHrView] = useState<HrView>('jobs');
  const [currentUser, setCurrentUser] = useState<LoginResponse | null>(null);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [applications, setApplications] = useState<Application[]>([]);
  const [documents, setDocuments] = useState<CandidateDocument[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [selectedJobId, setSelectedJobId] = useState('');
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  // Com sessão salva, o login não aparece enquanto o token é conferido em GET /auth/me.
  const [restoring, setRestoring] = useState(() => readSession() !== null);
  const [restoreError, setRestoreError] = useState<string | null>(null);
  /** Sobe a cada notificação recebida: telas com dados próprios (inscritos, quadro de documentos) recarregam. */
  const [liveVersion, setLiveVersion] = useState(0);
  const loaded = useRef(false);
  /** Sobe a cada saída: respostas de uma sessão que já acabou são descartadas. */
  const session = useRef(0);
  const refresh = async (user: LoginResponse) => {
    const mine = session.current;
    if (!loaded.current) { setLoading(true); setLoadError(null); }
    try {
      // RH e administrador não têm currículo nem candidaturas próprias (a API responde 403). O candidato lê os documentos
      // no quadro de cada candidatura aprovada; a lista geral é do RH.
      const candidate = user.perfil === 'candidato';
      const [nextJobs, nextProfile, nextApplications, nextDocuments, nextNotifications] = await Promise.all([portalService.getJobs(), candidate ? portalService.getProfile(String(user.id)) : null, candidate ? portalService.getApplications() : [], candidate ? [] : portalService.getDocuments(), portalService.getNotifications()]);
      if (mine !== session.current) return;
      setJobs(nextJobs); setProfile(nextProfile); setApplications(nextApplications); setDocuments(nextDocuments); setNotifications(nextNotifications);
      loaded.current = true; setLoadError(null);
    } catch (error) {
      if (mine !== session.current) return;
      const text = messageOf(error, 'Não foi possível carregar os dados.');
      if (loaded.current) setNotice({ tone: 'error', text }); else setLoadError(text);
    } finally { if (mine === session.current) setLoading(false); }
  };
  useEffect(() => { if (currentUser) void refresh(currentUser); }, [currentUser]);
  /** Executa uma ação, recarrega os dados e avisa o resultado sem derrubar a tela atual. */
  const act = async (task: () => Promise<unknown>, success?: string) => {
    if (!currentUser) return false;
    try {
      await task();
      await refresh(currentUser);
      if (success) setNotice({ tone: 'ok', text: success });
      return true;
    } catch (error) {
      setNotice({ tone: 'error', text: messageOf(error, 'Não foi possível concluir a ação. Tente novamente.') });
      return false;
    }
  };
  const selectedJob = jobs.find((job) => job.id === selectedJobId) ?? jobs[0];
  const detailJob = jobs.find((job) => job.id === selectedJobId);
  // Status de candidatura e documentos mudam por ação de outras pessoas: recarrega ao abrir essas telas.
  const navigateCandidate = (view: CandidateView) => { setMode('candidate'); setCandidateView(view); window.scrollTo(0, 0); if (currentUser && (view === 'applications' || view === 'documents')) void refresh(currentUser); };
  const navigateHr = (view: HrView) => { setMode('hr'); setHrView(view); window.scrollTo(0, 0); if (currentUser && view === 'documents') void refresh(currentUser); };
  const closeNotice = () => setNotice(null);
  /** Encerra a sessão e volta ao login; `reason` aparece como aviso (ex.: token expirado). */
  const signOut = (reason?: string) => {
    session.current += 1;
    setAuthToken(null);
    loaded.current = false;
    setCurrentUser(null); setProfile(null); setJobs([]); setApplications([]); setDocuments([]); setNotifications([]);
    setLoading(true); setLoadError(null); setSelectedJobId(''); setCandidateView('jobs'); setHrView('jobs'); setMode('login');
    setNotice(reason ? { tone: 'error', text: reason } : null);
    writeSession(null);
    setRestoring(false); setRestoreError(null);
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
  };
  const exit = () => signOut();
  // Qualquer chamada autenticada que volte 401 (token vencido ou inválido) derruba a sessão.
  useEffect(() => { setUnauthorizedHandler(() => signOut('Sua sessão expirou. Entre novamente.')); return () => setUnauthorizedHandler(null); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /** Vai para a tela do hash (ou para a inicial do perfil) e corrige o hash na URL quando ele não combina. */
  const applyRoute = (perfil: LoginResponse['perfil']) => {
    const route = routeFromHash(perfil);
    setMode(route.mode);
    if (route.mode === 'candidate') setCandidateView(route.view); else setHrView(route.view);
    if (route.jobId) setSelectedJobId(route.jobId);
    const hash = hashOf(route.mode, route.mode === 'candidate' ? route.view : 'jobs', route.mode === 'hr' ? route.view : 'jobs', route.jobId);
    if (window.location.hash !== hash) window.history.replaceState(null, '', hash);
  };
  /** F5: confere o token salvo em GET /auth/me e volta para a mesma tela. 401, 403 e 404 levam ao login. */
  const restore = async () => {
    const saved = readSession();
    if (!saved) { setRestoring(false); return; }
    setRestoreError(null); setRestoring(true);
    setAuthToken(saved.token);
    try {
      const me = await authService.usuarioAtual();
      // Perfil e status só valem no login: se o perfil mudou, o token antigo não serve.
      if (me.perfil !== saved.perfil) { signOut('Sua sessão expirou. Entre novamente.'); return; }
      const account: LoginResponse = { id: me.id, nome: me.nome, email: me.email, perfil: me.perfil, token: saved.token };
      writeSession(account);
      setCurrentUser(account);
      applyRoute(account.perfil);
      setRestoring(false);
    } catch (error) {
      if (error instanceof ApiError && [401, 403, 404].includes(error.status)) signOut('Sua sessão expirou. Entre novamente.');
      else setRestoreError(messageOf(error, 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'));
    }
  };
  useEffect(() => { void restore(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // A tela atual vai para a URL e o botão voltar do navegador navega entre as telas.
  useEffect(() => {
    if (!currentUser || (mode !== 'candidate' && mode !== 'hr')) return;
    const hash = hashOf(mode, candidateView, hrView, selectedJobId);
    if (window.location.hash !== hash) window.location.hash = hash;
  }, [currentUser, mode, candidateView, hrView, selectedJobId]);
  useEffect(() => {
    if (!currentUser || (mode !== 'candidate' && mode !== 'hr')) return;
    const onHashChange = () => { if (window.location.hash !== hashOf(mode, candidateView, hrView, selectedJobId)) applyRoute(currentUser.perfil); };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [currentUser, mode, candidateView, hrView, selectedJobId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Notificações em tempo real enquanto houver sessão: o fluxo reconecta sozinho e sair (ou perder o token) o aborta.
  useEffect(() => {
    if (!currentUser) return;
    const controller = new AbortController();
    void portalService.subscribeNotifications((event) => {
      if (event.type === 'connected') {
        // Ressincroniza: pode ter chegado notificação enquanto o fluxo estava fora do ar.
        void portalService.getNotifications().then((items) => { if (!controller.signal.aborted) setNotifications(items); }).catch(() => undefined);
        return;
      }
      setNotifications((current) => current.some((item) => item.id === event.item.id) ? current : [event.item, ...current]);
      setLiveVersion((current) => current + 1);
      void refresh(currentUser);
    }, controller.signal);
    return () => controller.abort();
  }, [currentUser]); // eslint-disable-line react-hooks/exhaustive-deps

  if (restoring) return <LoadingScreen error={restoreError} onRetry={() => void restore()} onExit={exit} />;
  if (mode === 'login') return <>
    <Login onAccess={(account) => { setAuthToken(account.token); writeSession(account); setNotice(null); setCurrentUser(account); if (account.perfil === 'candidato') navigateCandidate('jobs'); else navigateHr('jobs'); }} onCreateAccount={() => setMode('signup')} />
    <Toast notice={notice} onClose={closeNotice} />
  </>;
  if (mode === 'signup') return <Signup onBackToLogin={() => setMode('login')} />;
  if (!currentUser || loading || loadError || (currentUser.perfil === 'candidato' && !profile)) return <LoadingScreen error={loadError} onRetry={() => currentUser && void refresh(currentUser)} onExit={exit} />;

  // A tela marca na hora e a API confirma depois; se a chamada falhar, as notificações voltam como não lidas ao recarregar.
  const markRead = () => { setNotifications((items) => items.map((item) => ({ ...item, read: true }))); portalService.markNotificationsRead().catch(() => undefined); };
  const markOneRead = (id: string) => { setNotifications((items) => items.map((item) => item.id === id ? { ...item, read: true } : item)); portalService.markNotificationRead(id).catch(() => undefined); };
  /** Salva o currículo; erros de validação (400 com `campos`) voltam para a tela marcar cada campo. */
  const saveResume = async (next: CandidateProfile): Promise<{ ok: true } | { ok: false; campos?: Record<string, string> }> => {
    try {
      await portalService.updateProfile(next);
      await refresh(currentUser);
      setNotice({ tone: 'ok', text: 'Currículo salvo.' });
      return { ok: true };
    } catch (error) {
      if (error instanceof ApiError && error.campos) {
        setNotice({ tone: 'error', text: 'Revise os campos destacados no currículo.' });
        return { ok: false, campos: error.campos };
      }
      setNotice({ tone: 'error', text: messageOf(error, 'Não foi possível salvar o currículo. Tente novamente.') });
      return { ok: false };
    }
  };
  const uploadResume = async (file: File) => {
    if (!profile?.id) return 'Salve o currículo antes de anexar o PDF.';
    try {
      const saved = await portalService.uploadResumeFile(profile.id, file);
      setProfile((current) => current && { ...current, arquivo: saved.arquivo });
      setNotice({ tone: 'ok', text: 'PDF enviado.' });
      return null;
    } catch (error) {
      if (error instanceof ApiError && error.status === 413) return 'O arquivo passa de 5 MB. Envie uma versão menor.';
      return messageOf(error, 'Não foi possível enviar o PDF. Tente novamente.');
    }
  };
  const downloadResume = async () => {
    if (!profile?.id) return;
    try { saveBlob(await portalService.downloadResumeFile(profile.id), profile.arquivo?.nomeOriginal ?? 'curriculo.pdf'); }
    catch (error) { setNotice({ tone: 'error', text: messageOf(error, 'Não foi possível baixar o PDF.') }); }
  };
  /** Envia um documento de contratação. Devolve a mensagem de erro para o formulário, ou null quando foi aceito. */
  const uploadDocument = async (applicationId: string, typeCode: string, file: File) => {
    try {
      await portalService.uploadDocument(applicationId, typeCode, file);
      setNotice({ tone: 'ok', text: 'Documento enviado.' });
      return null;
    } catch (error) { return messageOf(error, 'Não foi possível enviar o documento. Tente novamente.'); }
  };
  /** Marca a entrevista. O 400 de `dataHora` ("deve ser futura") volta para o campo do diálogo. */
  const scheduleInterview = async (applicationId: string, dateTime: string) => {
    try {
      await portalService.scheduleInterview(applicationId, dateTime);
      await refresh(currentUser);
      setNotice({ tone: 'ok', text: 'Entrevista marcada. O candidato foi avisado.' });
      return null;
    } catch (error) {
      if (error instanceof ApiError && error.campos?.dataHora) return { field: true, message: error.campos.dataHora };
      return { field: false, message: messageOf(error, 'Não foi possível marcar a entrevista. Tente novamente.') };
    }
  };
  const downloadDocument = async (doc: CandidateDocument) => {
    try { saveBlob(await portalService.downloadDocument(doc.id), `${doc.type}.${doc.format}`); }
    catch (error) { setNotice({ tone: 'error', text: messageOf(error, 'Não foi possível baixar o documento.') }); }
  };
  return <>
    {mode === 'candidate' && profile && <CandidateLayout user={currentUser} active={candidateView} onNavigate={navigateCandidate} onExit={exit} notifications={notifications} onReadNotifications={markRead} onReadNotification={markOneRead}>
      {candidateView === 'jobs' && <JobsPage jobs={jobs} applications={applications} resumeEmpty={!profile.id} onEditResume={() => navigateCandidate('resume')} onOpen={(id) => { setSelectedJobId(id); navigateCandidate('detail'); }} />}
      {candidateView === 'detail' && (detailJob
        ? <JobDetail job={detailJob} profile={profile} userName={currentUser.nome} applied={applications.some((item) => item.jobId === detailJob.id)} onBack={() => navigateCandidate('jobs')} onEditResume={() => navigateCandidate('resume')} onApply={async () => { if (await act(() => portalService.apply(detailJob.id))) navigateCandidate('success'); }} />
        : <main className="page"><Empty title="Vaga não encontrada" text="Ela pode ter sido encerrada. Veja as outras vagas abertas." action={<Button onClick={() => navigateCandidate('jobs')}>Ver vagas</Button>} /></main>)}
      {candidateView === 'success' && <SuccessPage job={selectedJob} onApplications={() => navigateCandidate('applications')} onJobs={() => navigateCandidate('jobs')} />}
      {candidateView === 'resume' && <ResumePage profile={profile} userName={currentUser.nome} userEmail={currentUser.email} onSave={saveResume} onUpload={uploadResume} onDownload={downloadResume} />}
      {candidateView === 'applications' && <ApplicationsPage applications={applications} onDocuments={() => navigateCandidate('documents')} onJobs={() => navigateCandidate('jobs')} />}
      {candidateView === 'documents' && <DocumentsPage applications={applications} reloadKey={liveVersion} onBack={() => navigateCandidate('applications')} onUpload={uploadDocument} onDownload={downloadDocument} />}
    </CandidateLayout>}
    {mode === 'hr' && <HrLayout userName={currentUser.nome} role={currentUser.perfil} active={hrView} onNavigate={navigateHr} onExit={exit} notifications={notifications} onReadNotifications={markRead} onReadNotification={markOneRead}>
      {hrView === 'jobs' && <HrJobsPage jobs={jobs} onCandidates={(id) => { setSelectedJobId(id); navigateHr('candidates'); }} onSaved={(input) => act(() => portalService.saveJob(input), input.id ? 'Vaga atualizada.' : 'Vaga publicada.')} onClosed={(id) => act(() => portalService.closeJob(id), 'Vaga encerrada.')} />}
      {hrView === 'candidates' && (selectedJob
        ? <HrCandidatesPage job={selectedJob} jobs={jobs} reloadKey={liveVersion} onSelectJob={setSelectedJobId} onBack={() => navigateHr('jobs')} onUpdate={(id, status) => act(() => portalService.updateApplicationStatus(id, status), 'Etapa do candidato atualizada.')} onSchedule={scheduleInterview} onDownload={downloadDocument} />
        : <section className="hr-page"><Empty title="Nenhuma vaga cadastrada" text="Crie uma vaga para começar a receber candidatos." action={<Button onClick={() => navigateHr('jobs')}>Ir para vagas</Button>} /></section>)}
      {hrView === 'documents' && <HrDocumentsPage documents={documents} onDownload={downloadDocument} />}
      {hrView === 'settings' && currentUser.perfil === 'administrador' && <SettingsPage currentUserId={String(currentUser.id)} onNotice={setNotice} />}
    </HrLayout>}
    <Toast notice={notice} onClose={closeNotice} />
  </>;
}

/* ---------- Avisos e carregamento ---------- */
function Toast({ notice, onClose }: { notice: Notice; onClose: () => void }) {
  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(onClose, notice.tone === 'error' ? 8000 : 5000);
    return () => window.clearTimeout(timer);
  }, [notice]);
  if (!notice) return null;
  return <div className="toast" data-tone={notice.tone} role={notice.tone === 'error' ? 'alert' : 'status'}><p>{notice.text}</p><button type="button" onClick={onClose} aria-label="Fechar aviso"><X /></button></div>;
}

function LoadingScreen({ error, onRetry, onExit }: { error: string | null; onRetry: () => void; onExit: () => void }) {
  return <main className="loading">
    <Brand />
    {error
      ? <><p role="alert">{error}</p><div className="actions"><Button onClick={onRetry}>Tentar novamente</Button><Button variant="outline" onClick={onExit}>Voltar ao login</Button></div></>
      : <><svg className="ring" viewBox="0 0 72 72" aria-hidden="true"><circle cx="36" cy="36" r="24" pathLength={1} /></svg><p>Preparando o portal…</p></>}
  </main>;
}

/* ---------- Entrada e cadastro ---------- */
function Glow() { return <div className="glow" aria-hidden="true"><span /><span /><span /><span /></div>; }

function AuthShell({ title, text, children }: { title: string; text: string; children: ReactNode }) {
  return <div className="auth-page">
    <Glow />
    <header className="glass-nav auth-nav"><Brand /><ThemeSwitch /></header>
    <main className="auth">
      <section className="auth-copy">
        <h1 className="display">{title}</h1>
        <p className="lede">{text}</p>
        <ul className="facts">
          <li><span><Check /></span>Um currículo para todas as candidaturas</li>
          <li><span><Check /></span>Cada etapa do processo visível para você</li>
          <li><span><Check /></span>Decisão sempre de uma pessoa do RH</li>
        </ul>
      </section>
      <section className="auth-stage">
        <div className="orb" aria-hidden="true"><span /></div>
        <div className="glass-card auth-card">{children}</div>
      </section>
    </main>
    <footer className="auth-foot">© 2026 Projeto Integrador III · TeamUp</footer>
  </div>;
}

function Login({ onAccess, onCreateAccount }: { onAccess: (account: LoginResponse) => void; onCreateAccount: () => void }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(null); setSubmitting(true);
    try {
      onAccess(await authService.login({ email, senha }));
    } catch (err) { setError(err instanceof Error ? err.message : 'Não foi possível entrar. Tente novamente.'); }
    finally { setSubmitting(false); }
  };
  return <AuthShell title="Sua próxima vaga, do jeito que faz sentido acompanhar." text="Cadastre seu currículo uma vez, candidate-se em poucos cliques e acompanhe todas as etapas do processo.">
    <h2 className="display">Entrar</h2>
    <p className="card-lede">Use o e-mail e a senha do seu cadastro. Candidatos e RH entram pelo mesmo acesso.</p>
    <form className="auth-form" onSubmit={submit}>
      {error && <FormError title="Não foi possível entrar" text={error} />}
      <Field label="E-mail"><Input value={email} onChange={(event) => setEmail(event.target.value)} type="email" inputMode="email" autoComplete="email" required aria-invalid={Boolean(error) || undefined} /></Field>
      <Field label="Senha"><PasswordInput aria-invalid={Boolean(error) || undefined} value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></Field>
      <Button type="submit" size="lg" className="w-full cta" disabled={submitting}>{submitting ? 'Entrando…' : <>Entrar<CtaArrow /></>}</Button>
    </form>
    <p className="auth-alt">Ainda não tem conta? <Button type="button" variant="link" onClick={onCreateAccount}>Criar cadastro de candidato</Button></p>
  </AuthShell>;
}

function Signup({ onBackToLogin }: { onBackToLogin: () => void }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const update = (key: keyof typeof form, value: string) => setForm((current) => ({ ...current, [key]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form.name.trim() || !form.email.trim()) { setError('Preencha nome e e-mail para continuar.'); return; }
    if (form.password.length < 8) { setError('A senha deve ter pelo menos 8 caracteres.'); return; }
    if (form.password !== form.confirmPassword) { setError('As senhas informadas não coincidem.'); return; }
    if (!accepted) { setError('É necessário aceitar os termos de uso e a política de privacidade.'); return; }
    setError(null); setSubmitting(true);
    try {
      await authService.cadastrar({ nome: form.name.trim(), email: form.email.trim(), senha: form.password });
      setSubmitted(true);
    } catch (err) { setError(err instanceof Error ? err.message : 'Não foi possível concluir o cadastro.'); }
    finally { setSubmitting(false); }
  };

  return <AuthShell title="Crie sua conta e comece a se candidatar." text="Cadastro gratuito para candidatos: monte seu currículo e acompanhe todas as suas candidaturas em um só lugar.">
    {submitted
      ? <div className="auth-done">
          <div className="done-mark"><Check /></div>
          <h2 className="display">Cadastro realizado</h2>
          <p className="card-lede">Entre com seu e-mail e senha e complete seu currículo antes de se candidatar.</p>
          <Button size="lg" className="w-full cta" onClick={onBackToLogin}>Ir para o login<CtaArrow /></Button>
        </div>
      : <>
          <h2 className="display">Criar cadastro</h2>
          <p className="card-lede">Leva menos de dois minutos. O currículo você completa depois, com calma.</p>
          <form className="auth-form" onSubmit={submit}>
            <Field label="Nome completo"><Input required autoComplete="name" value={form.name} onChange={(event) => update('name', event.target.value)} /></Field>
            <Field label="E-mail"><Input required type="email" inputMode="email" autoComplete="email" value={form.email} onChange={(event) => update('email', event.target.value)} /></Field>
            <div className="field-row">
              <Field label="Senha" hint="Mínimo de 8 caracteres"><PasswordInput required autoComplete="new-password" value={form.password} onChange={(event) => update('password', event.target.value)} /></Field>
              <Field label="Confirmar senha"><PasswordInput required autoComplete="new-password" value={form.confirmPassword} onChange={(event) => update('confirmPassword', event.target.value)} /></Field>
            </div>
            <label className="check-label"><Checkbox checked={accepted} onCheckedChange={(value) => setAccepted(value === true)} /> Li e aceito os termos de uso e a política de privacidade (LGPD).</label>
            {error && <FormError title="Não foi possível concluir o cadastro" text={error} />}
            <Button type="submit" size="lg" className="w-full cta" disabled={submitting}>{submitting ? 'Criando conta…' : <>Criar minha conta<CtaArrow /></>}</Button>
          </form>
          <p className="auth-alt">Já tem uma conta? <Button type="button" variant="link" onClick={onBackToLogin}>Entrar</Button></p>
        </>}
  </AuthShell>;
}

/* ---------- Casca do candidato ---------- */
const candidateNav: { view: CandidateView; label: string; icon: typeof Bell; match: CandidateView[] }[] = [
  { view: 'jobs', label: 'Vagas', icon: BriefcaseBusiness, match: ['jobs', 'detail', 'success'] },
  { view: 'resume', label: 'Currículo', icon: FileText, match: ['resume'] },
  { view: 'applications', label: 'Candidaturas', icon: ClipboardList, match: ['applications', 'documents'] },
];

function CandidateLayout({ active, children, onNavigate, onExit, user, notifications, onReadNotifications, onReadNotification }: { active: CandidateView; children: ReactNode; onNavigate: (view: CandidateView) => void; onExit: () => void; user: LoginResponse; notifications: NotificationItem[]; onReadNotifications: () => void; onReadNotification: (id: string) => void }) {
  const links = candidateNav.map(({ view, label, icon: Icon, match }) => <button key={view} type="button" aria-current={match.includes(active) ? 'page' : undefined} onClick={() => onNavigate(view)}><Icon />{label}</button>);
  return <div className="shell">
    <Glow />
    <header className="glass-nav app-nav">
      <Brand />
      <nav className="topnav" aria-label="Navegação principal">{links}</nav>
      <div className="topbar-actions">
        <Notifications items={notifications} onRead={onReadNotifications} onReadOne={onReadNotification} />
        <DropdownMenu>
          <DropdownMenuTrigger asChild><button type="button" className="profile-trigger" aria-label="Conta e preferências"><span><strong>{firstName(user.nome)}</strong><small>Candidato</small></span><span className="initials">{initials(user.nome)}</span></button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-60">
            <DropdownMenuLabel><strong className="block">{user.nome}</strong><span className="block font-normal text-muted-foreground">{user.email}</span></DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => onNavigate('resume')}><FileText />Meu currículo</DropdownMenuItem>
            <DropdownMenuSeparator />
            <ThemeMenu />
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={onExit}><LogOut />Sair</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
    {children}
    <nav className="tabbar" aria-label="Navegação principal">{links}</nav>
  </div>;
}

function Notifications({ items, onRead, onReadOne }: { items: NotificationItem[]; onRead: () => void; onReadOne: (id: string) => void }) {
  const unread = items.filter((item) => !item.read).length;
  return <DropdownMenu onOpenChange={(open) => { if (!open && unread > 0) onRead(); }}>
    <DropdownMenuTrigger asChild><Button variant="ghost" size="icon" className="bell" aria-label={unread > 0 ? `Notificações, ${unread} não lidas` : 'Notificações'}><Bell />{unread > 0 && <span className="bell-count">{unread}</span>}</Button></DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="notification-menu">
      <div className="menu-title">Notificações</div>
      <DropdownMenuSeparator />
      {items.length === 0 && <p className="px-2.5 py-3 text-muted-foreground">Nenhuma notificação por enquanto.</p>}
      {items.map((item) => <DropdownMenuItem key={item.id} className={`notification-item ${item.read ? '' : 'unread'}`} onSelect={(event) => { event.preventDefault(); if (!item.read) onReadOne(item.id); }}><strong>{item.title}</strong><small>{item.description}</small></DropdownMenuItem>)}
    </DropdownMenuContent>
  </DropdownMenu>;
}

/* ---------- Vagas ---------- */
function JobsPage({ jobs, applications, resumeEmpty, onOpen, onEditResume }: { jobs: Job[]; applications: Application[]; resumeEmpty: boolean; onOpen: (id: string) => void; onEditResume: () => void }) {
  const [query, setQuery] = useState(''); const [model, setModel] = useState('all');
  const open = useMemo(() => jobs.filter((job) => job.status === 'aberta'), [jobs]);
  const filtered = useMemo(() => open.filter((job) => (model === 'all' || job.workModel === model) && job.title.toLowerCase().includes(query.trim().toLowerCase())), [open, query, model]);
  const clear = () => { setQuery(''); setModel('all'); };
  return <main className="page">
    <div className="page-head"><div><h1 className="display">Vagas abertas</h1><p>{open.length === 1 ? '1 vaga aberta para candidatura.' : `${open.length} vagas abertas para candidatura.`}</p></div></div>
    {resumeEmpty && <div className="resume-nudge"><FileText /><p><strong>Seu currículo ainda está vazio.</strong> Preencha antes de se candidatar para o RH conhecer você.</p><Button onClick={onEditResume}>Completar currículo</Button></div>}
    {open.length > 0 && <div className="toolbar">
      <label className="search"><span className="sr-only">Buscar vaga por cargo</span><Search /><Input type="search" placeholder="Buscar por cargo" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
      <div className="segmented model-filter" role="group" aria-label="Modalidade">{[['all', 'Todas'], ['Presencial', 'Presencial'], ['Híbrido', 'Híbrido'], ['Remoto', 'Remoto']].map(([value, label]) => <button key={value} type="button" aria-pressed={model === value} onClick={() => setModel(value)}>{label}</button>)}</div>
    </div>}
    {filtered.length > 0
      ? <div className="lineup">{filtered.map((job) => {
          const application = applications.find((item) => item.jobId === job.id);
          return <button type="button" className="lineup-row" key={job.id} onClick={() => onOpen(job.id)}>
            <span><span className="row-title">{job.title}</span><JobMeta job={job} /></span>
            <span className="row-aside">{job.closesAt && <span className="deadline">Inscrições até {formatDate(job.closesAt)}</span>}{application && <Chip tone={appTone[application.status]}>Candidatura · {appStatus[application.status]}</Chip>}<ChevronRight /></span>
          </button>;
        })}</div>
      : open.length === 0
        ? <div className="lineup"><Empty title="Nenhuma vaga aberta no momento" text="Novas vagas aparecem aqui assim que o RH publicar. Enquanto isso, deixe seu currículo em dia." /></div>
        : <div className="lineup"><Empty title="Nenhuma vaga com esses filtros" text="Tente outro cargo ou outra modalidade." action={<Button variant="outline" onClick={clear}>Limpar filtros</Button>} /></div>}
  </main>;
}

function JobDetail({ job, profile, userName, applied, onBack, onApply, onEditResume }: { job: Job; profile: CandidateProfile; userName: string; applied: boolean; onBack: () => void; onApply: () => Promise<void>; onEditResume: () => void }) {
  const [sending, setSending] = useState(false);
  const apply = async () => { setSending(true); await onApply(); setSending(false); };
  const needsResume = !profile.id && !applied;
  const applyButton = <Button size="lg" variant={needsResume ? 'outline' : 'default'} className="w-full" disabled={applied || sending} onClick={apply}>{applied ? <><Check />Candidatura enviada</> : sending ? 'Enviando…' : 'Confirmar candidatura'}</Button>;
  const resumeButton = <Button size="lg" className="w-full" onClick={onEditResume}>Completar currículo</Button>;
  return <main className="page">
    <button type="button" className="back-link" onClick={onBack}><ChevronLeft />Vagas</button>
    <div className="detail">
      <article>
        <header className="detail-head"><h1 className="display">{job.title}</h1><JobMeta job={job} withDate /></header>
        <div className="prose">
          <h2>Sobre a vaga</h2><p>{job.description}</p>
          {job.requirements.length > 0 && <><h2>Requisitos</h2><ol>{job.requirements.map((item) => <li key={item}>{item}</li>)}</ol></>}
        </div>
      </article>
      <aside className="apply-panel" aria-label="Sua candidatura">
        <h2>Sua candidatura</h2>
        <dl className="kv"><dt>Currículo</dt><dd>{userName}</dd><dt>Atualizado em</dt><dd>{profile.updatedAt ? formatDate(profile.updatedAt) : 'Ainda não salvo'}</dd>{job.closesAt && <><dt>Inscrições até</dt><dd>{formatDate(job.closesAt)}</dd></>}</dl>
        {needsResume && <div className="note"><strong>Seu currículo ainda está vazio.</strong>Preencha antes de se candidatar para o RH conhecer você.</div>}
        <p>O RH verá seu currículo completo e poderá pedir documentos se você for aprovado.</p>
        <div className="actions">{needsResume && resumeButton}{applyButton}</div>
      </aside>
    </div>
    <div className="apply-bar">
      {needsResume && <p className="apply-bar-note"><CircleAlert />Seu currículo ainda está vazio.</p>}
      <div className="actions">{needsResume && resumeButton}{applyButton}</div>
    </div>
  </main>;
}

function SuccessPage({ job, onApplications, onJobs }: { job?: Job; onApplications: () => void; onJobs: () => void }) {
  return <main className="page narrow"><div className="success">
    <div className="success-head"><div className="done-mark"><Check /></div><h1 className="display">Candidatura enviada</h1><p>Tudo certo. Acompanhe cada etapa do processo em Candidaturas.</p></div>
    <section className="panel">
      <div className="panel-head"><div><h2>{job?.title ?? 'Sua candidatura'}</h2><p>Etapa atual</p></div><Chip>Inscrito</Chip></div>
      <Stages status="applied" />
      <h2 className="steps-title">Próximos passos</h2>
      <ol className="next-steps"><li>O RH revisa seu currículo, com apoio de triagem assistida.</li><li>Se avançar, você pode ser chamado para entrevista.</li><li>Se for aprovado, o RH pede os documentos de contratação.</li></ol>
    </section>
    <div className="flex flex-wrap gap-2"><Button size="lg" onClick={onApplications}>Ver minhas candidaturas</Button><Button size="lg" variant="ghost" onClick={onJobs}>Ver outras vagas</Button></div>
  </div></main>;
}

/* ---------- Currículo ---------- */
type SaveResult = { ok: true } | { ok: false; campos?: Record<string, string> };
type ResumeErrors = Record<string, string>;

const sexoOptions: { value: Sexo; label: string }[] = [
  { value: 'feminino', label: 'Feminino' },
  { value: 'masculino', label: 'Masculino' },
  { value: 'outro', label: 'Outro' },
  { value: 'nao_informado', label: 'Prefiro não informar' },
];
/** Chaves de erro que a tela sabe mostrar: os `campos` do backend seguem este mesmo formato. */
const resumeKeyPattern = /^(dataNascimento|sexo|cidade|uf|numeroContato|perfilLinkedin|competencias|certificacoes|resumo)$|^(experiencias|formacoes)\[\d+\]\.\w+$/;

const localToday = () => new Date().toLocaleDateString('sv-SE');
const formatBytes = (bytes: number) => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
/** O que o usuário edita, sem chaves de lista, idade e arquivo: serve para saber se há alterações não salvas. */
const resumeSignature = (profile: CandidateProfile) => JSON.stringify({
  ...profile, idade: undefined, arquivo: undefined, updatedAt: undefined,
  formacoes: profile.formacoes.map(({ uid: _uid, ...item }) => item),
  experiencias: profile.experiencias.map(({ uid: _uid, ...item }) => item),
});

/** Valida no cliente o que a API também valida, com as mesmas chaves de erro do backend. */
function validateResume(form: CandidateProfile): ResumeErrors {
  const errors: ResumeErrors = {};
  if (form.dataNascimento && form.dataNascimento >= localToday()) errors.dataNascimento = 'A data de nascimento deve ser no passado.';
  if (form.uf && form.uf.length !== 2) errors.uf = 'Use a sigla com 2 letras.';
  form.experiencias.forEach((item, index) => {
    const key = (field: string) => `experiencias[${index}].${field}`;
    if (!item.cargo.trim()) errors[key('cargo')] = 'Informe o cargo.';
    if (!item.empresa.trim()) errors[key('empresa')] = 'Informe a empresa.';
    if (!item.dataContratacao) errors[key('dataContratacao')] = 'Informe a data de contratação.';
    else if (!item.trabalhoAtual && item.dataDemissao && item.dataDemissao < item.dataContratacao) errors[key('dataDemissao')] = 'A demissão não pode ser anterior à contratação.';
  });
  form.formacoes.forEach((item, index) => {
    const key = (field: string) => `formacoes[${index}].${field}`;
    if (!item.curso.trim()) errors[key('curso')] = 'Informe o curso.';
    if (!item.instituicao.trim()) errors[key('instituicao')] = 'Informe a instituição.';
    if (!item.dataInicio) errors[key('dataInicio')] = 'Informe a data de início.';
  });
  return errors;
}

function ResumePage({ profile, userName, userEmail, onSave, onUpload, onDownload }: {
  profile: CandidateProfile; userName: string; userEmail: string;
  onSave: (profile: CandidateProfile) => Promise<SaveResult>;
  /** Devolve a mensagem de erro, ou null quando o PDF foi aceito. */
  onUpload: (file: File) => Promise<string | null>;
  onDownload: () => Promise<void>;
}) {
  const [form, setForm] = useState(profile);
  const [errors, setErrors] = useState<ResumeErrors>({});
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const signature = resumeSignature(profile);
  // Só recarrega o formulário quando o currículo salvo muda de fato (não a cada atualização de dados da página).
  useEffect(() => { setForm(profile); setErrors({}); }, [signature]); // eslint-disable-line react-hooks/exhaustive-deps
  const dirty = resumeSignature(form) !== signature;
  const pending = dirty || !profile.id;
  const failing = Object.keys(errors).length > 0;

  const clearErrors = (...keys: string[]) => setErrors((current) => {
    if (!keys.some((key) => key in current)) return current;
    const next = { ...current };
    keys.forEach((key) => delete next[key]);
    return next;
  });
  const focusFirstInvalid = () => window.setTimeout(() => formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(), 50);
  const update = <K extends keyof CandidateProfile>(key: K, value: CandidateProfile[K]) => { setForm((current) => ({ ...current, [key]: value })); clearErrors(key); };
  const patchExperience = (index: number, patch: Partial<ResumeExperience>) => {
    setForm((current) => ({ ...current, experiencias: current.experiencias.map((item, position) => position === index ? { ...item, ...patch } : item) }));
    clearErrors(...Object.keys(patch).map((field) => `experiencias[${index}].${field}`), `experiencias[${index}].dataDemissao`);
  };
  const patchEducation = (index: number, patch: Partial<ResumeEducation>) => {
    setForm((current) => ({ ...current, formacoes: current.formacoes.map((item, position) => position === index ? { ...item, ...patch } : item) }));
    clearErrors(...Object.keys(patch).map((field) => `formacoes[${index}].${field}`));
  };
  const dropErrorsOf = (list: 'experiencias' | 'formacoes') => setErrors((current) => Object.fromEntries(Object.entries(current).filter(([key]) => !key.startsWith(`${list}[`))));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const found = validateResume(form);
    setErrors(found);
    if (Object.keys(found).length) { focusFirstInvalid(); return; }
    setSaving(true);
    const result = await onSave(form);
    setSaving(false);
    if (!result.ok && result.campos) {
      const mapped: ResumeErrors = {};
      for (const [key, message] of Object.entries(result.campos)) {
        const field = key.replace(/\.periodoValido$/, '.dataDemissao');
        if (resumeKeyPattern.test(field)) mapped[field] = message;
      }
      setErrors(mapped);
      focusFirstInvalid();
    }
  };

  const chooseFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file) return;
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { setFileError('Só aceitamos arquivos PDF. Converta o currículo e envie de novo.'); return; }
    if (file.size > maxUploadBytes) { setFileError('O arquivo passa de 5 MB. Envie uma versão menor.'); return; }
    setFileError(null); setUploading(true);
    setFileError(await onUpload(file));
    setUploading(false);
  };

  const invalid = (key: string) => errors[key] ? true : undefined;
  const status = failing ? 'Corrija os campos destacados' : dirty ? 'Alterações não salvas' : 'Currículo ainda não salvo';

  return <main className="page narrow">
    <div className="page-head"><div><h1 className="display">Meu currículo</h1><p>Um currículo só, usado em todas as suas candidaturas.</p></div>{!pending && <Chip tone="green">Tudo salvo{profile.updatedAt ? ` · ${formatDate(profile.updatedAt)}` : ''}</Chip>}</div>
    <form onSubmit={submit} ref={formRef} noValidate>
      <div className="form-sheet">
        <section className="form-section"><header><h2>Contato</h2><p>Nome e e-mail vêm do seu cadastro.</p></header><div>
          <div className="field-row"><Field label="Nome completo"><Input value={userName} disabled /></Field><Field label="E-mail"><Input value={userEmail} disabled /></Field></div>
          <div className="field-row">
            <Field label="Telefone" error={errors.numeroContato}><Input type="tel" inputMode="tel" autoComplete="tel" maxLength={20} placeholder="(54) 99999-0000" value={form.numeroContato} aria-invalid={invalid('numeroContato')} onChange={(event) => update('numeroContato', event.target.value)} /></Field>
            <Field label="LinkedIn" error={errors.perfilLinkedin}><Input type="url" inputMode="url" maxLength={255} placeholder="https://linkedin.com/in/seu-perfil" value={form.perfilLinkedin} aria-invalid={invalid('perfilLinkedin')} onChange={(event) => update('perfilLinkedin', event.target.value)} /></Field>
          </div>
        </div></section>
        <section className="form-section"><header><h2>Dados pessoais</h2><p>Usados só para o processo seletivo.</p></header><div>
          <div className="field-row">
            <Field label="Data de nascimento" hint={profile.idade != null ? `${profile.idade} anos` : undefined} error={errors.dataNascimento}><Input type="date" max={localToday()} value={form.dataNascimento} aria-invalid={invalid('dataNascimento')} onChange={(event) => update('dataNascimento', event.target.value)} /></Field>
            <Field label="Sexo" error={errors.sexo}><Select value={form.sexo} onValueChange={(value) => update('sexo', value as Sexo)}><SelectTrigger className="w-full" aria-invalid={invalid('sexo')}><SelectValue placeholder="Selecione" /></SelectTrigger><SelectContent>{sexoOptions.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}</SelectContent></Select></Field>
          </div>
          <div className="field-row city-row">
            <Field label="Cidade" error={errors.cidade}><Input maxLength={100} autoComplete="address-level2" placeholder="Erechim" value={form.cidade} aria-invalid={invalid('cidade')} onChange={(event) => update('cidade', event.target.value)} /></Field>
            <Field label="UF" error={errors.uf}><Input maxLength={2} autoComplete="address-level1" placeholder="RS" value={form.uf} aria-invalid={invalid('uf')} onChange={(event) => update('uf', event.target.value.toUpperCase())} /></Field>
          </div>
        </div></section>
        <section className="form-section"><header><h2 id="cv-resumo">Resumo profissional</h2><p>Duas ou três frases sobre você e o que procura.</p></header><div><Textarea aria-labelledby="cv-resumo" placeholder="Ex.: Estudante de ADS, busco estágio em desenvolvimento web." value={form.resumo} onChange={(event) => update('resumo', event.target.value)} />{errors.resumo && <small className="field-error" role="alert">{errors.resumo}</small>}</div></section>
        <section className="form-section"><header><h2>Experiência profissional</h2><p>Opcional. Cargo, empresa, período e o que você fazia.</p></header><div>
          {form.experiencias.map((item, index) => {
            const error = (field: string) => errors[`experiencias[${index}].${field}`];
            return <div className="repeat-item" key={item.uid}>
              <div className="repeat-head"><strong>Experiência {index + 1}</strong><Button type="button" variant="ghost" size="sm" onClick={() => { setForm((current) => ({ ...current, experiencias: current.experiencias.filter((_, position) => position !== index) })); dropErrorsOf('experiencias'); }}><Trash2 />Remover<span className="sr-only"> experiência {index + 1}</span></Button></div>
              <div className="field-row">
                <Field label="Cargo" error={error('cargo')}><Input value={item.cargo} aria-invalid={invalid(`experiencias[${index}].cargo`)} onChange={(event) => patchExperience(index, { cargo: event.target.value })} /></Field>
                <Field label="Empresa" error={error('empresa')}><Input value={item.empresa} aria-invalid={invalid(`experiencias[${index}].empresa`)} onChange={(event) => patchExperience(index, { empresa: event.target.value })} /></Field>
              </div>
              <div className="field-row">
                <Field label="Contratação" error={error('dataContratacao')}><Input type="date" value={item.dataContratacao} aria-invalid={invalid(`experiencias[${index}].dataContratacao`)} onChange={(event) => patchExperience(index, { dataContratacao: event.target.value })} /></Field>
                <Field label="Demissão" error={error('dataDemissao')}><Input type="date" min={item.dataContratacao || undefined} disabled={item.trabalhoAtual} value={item.trabalhoAtual ? '' : item.dataDemissao} aria-invalid={invalid(`experiencias[${index}].dataDemissao`)} onChange={(event) => patchExperience(index, { dataDemissao: event.target.value })} /></Field>
              </div>
              <label className="check-label"><Checkbox checked={item.trabalhoAtual} onCheckedChange={(value) => patchExperience(index, value === true ? { trabalhoAtual: true, dataDemissao: '' } : { trabalhoAtual: false })} /> Trabalho aqui atualmente</label>
              <Field label="Atividades" hint="opcional"><Textarea placeholder="Ex.: Atendimento a clientes, planilhas e relatórios." value={item.descricaoAtividades} onChange={(event) => patchExperience(index, { descricaoAtividades: event.target.value })} /></Field>
            </div>;
          })}
          {!form.experiencias.length && <p className="repeat-empty">Nenhuma experiência adicionada.</p>}
          <Button type="button" variant="outline" className="repeat-add" onClick={() => setForm((current) => ({ ...current, experiencias: [...current.experiencias, { uid: crypto.randomUUID(), cargo: '', empresa: '', dataContratacao: '', dataDemissao: '', trabalhoAtual: false, descricaoAtividades: '' }] }))}><Plus />Adicionar experiência</Button>
        </div></section>
        <section className="form-section"><header><h2>Formação acadêmica</h2><p>Opcional. Curso, instituição e período.</p></header><div>
          {form.formacoes.map((item, index) => {
            const error = (field: string) => errors[`formacoes[${index}].${field}`];
            return <div className="repeat-item" key={item.uid}>
              <div className="repeat-head"><strong>Formação {index + 1}</strong><Button type="button" variant="ghost" size="sm" onClick={() => { setForm((current) => ({ ...current, formacoes: current.formacoes.filter((_, position) => position !== index) })); dropErrorsOf('formacoes'); }}><Trash2 />Remover<span className="sr-only"> formação {index + 1}</span></Button></div>
              <div className="field-row">
                <Field label="Curso" error={error('curso')}><Input value={item.curso} aria-invalid={invalid(`formacoes[${index}].curso`)} onChange={(event) => patchEducation(index, { curso: event.target.value })} /></Field>
                <Field label="Instituição" error={error('instituicao')}><Input value={item.instituicao} aria-invalid={invalid(`formacoes[${index}].instituicao`)} onChange={(event) => patchEducation(index, { instituicao: event.target.value })} /></Field>
              </div>
              <div className="field-row">
                <Field label="Início" error={error('dataInicio')}><Input type="date" value={item.dataInicio} aria-invalid={invalid(`formacoes[${index}].dataInicio`)} onChange={(event) => patchEducation(index, { dataInicio: event.target.value })} /></Field>
                <Field label="Término" hint="vazio = em andamento" error={error('dataTermino')}><Input type="date" min={item.dataInicio || undefined} value={item.dataTermino} aria-invalid={invalid(`formacoes[${index}].dataTermino`)} onChange={(event) => patchEducation(index, { dataTermino: event.target.value })} /></Field>
              </div>
            </div>;
          })}
          {!form.formacoes.length && <p className="repeat-empty">Nenhuma formação adicionada.</p>}
          <Button type="button" variant="outline" className="repeat-add" onClick={() => setForm((current) => ({ ...current, formacoes: [...current.formacoes, { uid: crypto.randomUUID(), curso: '', instituicao: '', dataInicio: '', dataTermino: '' }] }))}><Plus />Adicionar formação</Button>
        </div></section>
        <section className="form-section"><header><h2 id="cv-skills">Competências</h2><p>Enter ou vírgula para adicionar.</p></header><div><TagInput labelId="cv-skills" value={form.skills} onChange={(skills) => update('skills', skills)} />{errors.competencias && <small className="field-error" role="alert">{errors.competencias}</small>}</div></section>
        <section className="form-section"><header><h2 id="cv-cert">Certificações</h2><p>Opcional. Cursos e certificados fora da formação acadêmica.</p></header><div><Textarea aria-labelledby="cv-cert" placeholder="Ex.: AWS Cloud Practitioner (2024)." value={form.certificacoes} onChange={(event) => update('certificacoes', event.target.value)} />{errors.certificacoes && <small className="field-error" role="alert">{errors.certificacoes}</small>}</div></section>
        <section className="form-section"><header><h2 id="cv-arquivo">Currículo em PDF</h2><p>Opcional. Só PDF, até 5 MB. Enviar de novo substitui o anterior.</p></header><div>
          {profile.arquivo
            ? <div className="file-card"><span className="doc-icon"><FileText /></span><div><strong>{profile.arquivo.nomeOriginal}</strong><small>{formatBytes(profile.arquivo.tamanhoBytes)} · enviado em {formatDate(profile.arquivo.enviadoEm)}</small></div><Button type="button" variant="outline" size="sm" onClick={() => void onDownload()}><Download />Baixar</Button></div>
            : <p className="repeat-empty">Nenhum arquivo enviado.</p>}
          <input ref={fileInput} type="file" accept="application/pdf,.pdf" hidden aria-labelledby="cv-arquivo" onChange={chooseFile} />
          {profile.id
            ? <Button type="button" variant="outline" className="repeat-add" disabled={uploading} onClick={() => fileInput.current?.click()}><Upload />{uploading ? 'Enviando…' : profile.arquivo ? 'Substituir PDF' : 'Enviar PDF'}</Button>
            : <p className="repeat-empty">Salve o currículo primeiro para poder anexar o PDF.</p>}
          {fileError && <small className="field-error" role="alert">{fileError}</small>}
        </div></section>
      </div>
      {pending && <div className="save-bar" data-dirty={dirty} data-invalid={failing}><span>{status}</span><Button type="submit" disabled={saving}>{saving ? 'Salvando…' : 'Salvar currículo'}</Button></div>}
    </form>
  </main>;
}

function TagInput({ value, onChange, labelId }: { value: string[]; onChange: (value: string[]) => void; labelId: string }) {
  const [draft, setDraft] = useState('');
  const add = (raw: string) => {
    const next = raw.split(',').map((item) => item.trim()).filter((item) => item && !value.includes(item));
    if (next.length) onChange([...value, ...next]);
    setDraft('');
  };
  return <div className="tag-input">
    {value.map((tag) => <span className="tag" key={tag}>{tag}<button type="button" aria-label={`Remover ${tag}`} onClick={() => onChange(value.filter((item) => item !== tag))}><X /></button></span>)}
    <input aria-labelledby={labelId} value={draft} placeholder={value.length ? 'Adicionar outra' : 'Ex.: React, SQL, Comunicação'}
      onChange={(event) => { const next = event.target.value; if (next.includes(',')) add(next); else setDraft(next); }}
      onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); add(draft); } else if (event.key === 'Backspace' && !draft && value.length) onChange(value.slice(0, -1)); }}
      onBlur={() => { if (draft.trim()) add(draft); }} />
  </div>;
}

/* ---------- Candidaturas e documentos ---------- */
function ApplicationsPage({ applications, onDocuments, onJobs }: { applications: Application[]; onDocuments: () => void; onJobs: () => void }) {
  return <main className="page narrow">
    <div className="page-head"><div><h1 className="display">Minhas candidaturas</h1><p>{applications.length === 1 ? '1 candidatura em andamento.' : `${applications.length} candidaturas em andamento.`}</p></div><Button variant="outline" onClick={onDocuments}><FileText />Meus documentos</Button></div>
    {applications.length === 0
      ? <div className="lineup"><Empty title="Você ainda não se candidatou" text="Encontre uma vaga aberta e candidate-se com o seu currículo." action={<Button onClick={onJobs}>Ver vagas abertas</Button>} /></div>
      : <div className="stack">{applications.map((app) => <section className="panel application" data-status={app.status} key={app.id}>
          <div className="panel-head"><div><h2>{app.jobTitle}</h2><p>Enviada em {formatDate(app.submittedAt)}{app.jobStatus !== 'aberta' && ` · Vaga ${jobStatus[app.jobStatus].toLowerCase()}`}</p></div><Chip tone={appTone[app.status]}>{appStatus[app.status]}</Chip></div>
          <Stages status={app.status} />
          <div className="panel-foot">
            <p className="next-step">{app.status === 'interview' && app.interviewAt ? `Entrevista marcada para ${formatDateTime(app.interviewAt)} (horário de Brasília).` : nextStep[app.status]}</p>
            {(app.status === 'approved' || app.status === 'hired') && <Button onClick={onDocuments}><Upload />Enviar documentos</Button>}
            {(app.status === 'rejected' || app.status === 'cancelled') && <Button variant="outline" onClick={onJobs}>Ver outras vagas</Button>}
          </div>
        </section>)}</div>}
  </main>;
}

function Stages({ status }: { status: ApplicationStatus }) {
  const current = stepFor(status); const out = status === 'rejected' || status === 'cancelled';
  return <ol className="stages" aria-label="Etapas da candidatura">{stageNames.map((name, index) => {
    const step = index + 1;
    const state = out && step === current ? 'out' : step < current ? 'done' : step === current ? 'current' : '';
    return <li key={name} className={state} aria-current={step === current ? 'step' : undefined}><span className="node">{state === 'done' || (state === 'current' && (status === 'approved' || status === 'hired')) ? <Check /> : state === 'out' ? null : step}</span><span>{state === 'out' ? appStatus[status] : name}</span></li>;
  })}</ol>;
}

/** Valida o arquivo de um documento no cliente: o backend aceita PDF e DOCX de até 5 MB. */
function checkDocumentFile(file: File): string | null {
  if (!/\.(pdf|docx)$/i.test(file.name)) return 'Só aceitamos arquivos PDF ou DOCX.';
  if (file.size > maxUploadBytes) return 'O arquivo passa de 5 MB. Envie uma versão menor.';
  return null;
}

type UploadDocument = (applicationId: string, typeCode: string, file: File) => Promise<string | null>;

function DocumentsPage({ applications, reloadKey, onBack, onUpload, onDownload }: {
  applications: Application[]; reloadKey: number; onBack: () => void;
  /** Devolve a mensagem de erro, ou null quando o documento foi aceito. */
  onUpload: UploadDocument;
  onDownload: (doc: CandidateDocument) => Promise<void>;
}) {
  // O backend só aceita documentos em candidaturas aprovadas ou contratadas.
  const eligible = applications.filter((app) => app.status === 'approved' || app.status === 'hired');
  // Sobe a cada envio aceito: os quadros recarregam e o item passa de pendente para enviado.
  const [version, setVersion] = useState(0);
  const upload: UploadDocument = async (applicationId, typeCode, file) => {
    const problem = await onUpload(applicationId, typeCode, file);
    if (!problem) setVersion((current) => current + 1);
    return problem;
  };
  return <main className="page narrow">
    <button type="button" className="back-link" onClick={onBack}><ChevronLeft />Candidaturas</button>
    <div className="page-head"><div><h1 className="display">Documentos</h1><p>Envie os documentos de contratação das vagas em que você foi aprovado.</p></div></div>
    {eligible.length === 0
      ? <div className="lineup"><Empty title="Documentos ainda não liberados" text="Os documentos são liberados quando você for aprovado em uma vaga." /></div>
      : <>
          <DocumentForm applications={eligible} onUpload={upload} />
          <div className="stack">{eligible.map((app) => <DocumentBoardView key={app.id} applicationId={app.id} reloadKey={`${version}.${reloadKey}`} onDownload={onDownload} onReplace={(typeCode, file) => upload(app.id, typeCode, file)} />)}</div>
        </>}
  </main>;
}

function DocumentForm({ applications, onUpload }: { applications: Application[]; onUpload: UploadDocument }) {
  const [types, setTypes] = useState<DocumentType[]>([]);
  const [applicationId, setApplicationId] = useState(applications[0].id);
  const [typeCode, setTypeCode] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  useEffect(() => { portalService.getDocumentTypes().then(setTypes).catch((problem) => setError(messageOf(problem, 'Não foi possível carregar os tipos de documento.'))); }, []);
  // A candidatura escolhida pode sair da lista (ex.: o RH mudou a etapa): volta para a primeira.
  const selected = applications.some((app) => app.id === applicationId) ? applicationId : applications[0].id;
  const choose = (event: ChangeEvent<HTMLInputElement>) => {
    const picked = event.target.files?.[0]; event.target.value = '';
    if (!picked) return;
    const problem = checkDocumentFile(picked);
    setFile(problem ? null : picked); setError(problem);
  };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!typeCode) { setError('Escolha o tipo do documento.'); return; }
    if (!file) { setError('Escolha o arquivo do documento.'); return; }
    setError(null); setSending(true);
    const problem = await onUpload(selected, typeCode, file);
    setSending(false);
    if (problem) setError(problem); else { setTypeCode(''); setFile(null); }
  };
  return <section className="panel doc-form">
    <div className="panel-head"><div><h2>Enviar documento</h2><p>PDF ou DOCX, até 5 MB. Enviar de novo o mesmo tipo substitui o arquivo anterior.</p></div></div>
    <form className="dialog-form" onSubmit={submit} noValidate>
      <div className="field-row">
        {applications.length > 1 && <Field label="Vaga"><Select value={selected} onValueChange={setApplicationId}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent>{applications.map((app) => <SelectItem key={app.id} value={app.id}>{app.jobTitle}</SelectItem>)}</SelectContent></Select></Field>}
        <Field label="Tipo do documento"><Select value={typeCode} onValueChange={(value) => { setTypeCode(value); setError(null); }}><SelectTrigger className="w-full"><SelectValue placeholder="Selecione o tipo" /></SelectTrigger><SelectContent>{types.map((item) => <SelectItem key={item.code} value={item.code}>{item.name}{item.required ? '' : ' (condicional)'}</SelectItem>)}</SelectContent></Select></Field>
      </div>
      <div className="file-pick">
        <input ref={fileInput} type="file" accept=".pdf,.docx" hidden aria-label="Arquivo do documento" onChange={choose} />
        <Button type="button" variant="outline" onClick={() => fileInput.current?.click()}><Upload />{file ? 'Trocar arquivo' : 'Escolher arquivo'}</Button>
        <span>{file ? `${file.name} · ${formatBytes(file.size)}` : 'Nenhum arquivo escolhido'}</span>
      </div>
      {error && <FormError title="Não foi possível enviar" text={error} />}
      <div className="doc-form-actions"><Button type="submit" disabled={sending}>{sending ? 'Enviando…' : 'Enviar documento'}</Button></div>
    </form>
  </section>;
}

/**
 * Quadro de documentos de uma candidatura: enviados, pendentes e quantos obrigatórios já foram.
 * O candidato e o RH veem o mesmo quadro; só o candidato recebe `onReplace` e pode trocar arquivos.
 */
function DocumentBoardView({ applicationId, reloadKey, onDownload, onReplace }: {
  applicationId: string; reloadKey: string | number;
  onDownload: (doc: CandidateDocument) => Promise<void>;
  onReplace?: (typeCode: string, file: File) => Promise<string | null>;
}) {
  const [board, setBoard] = useState<DocumentBoard | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let current = true;
    portalService.getDocumentBoard(applicationId)
      .then((next) => { if (current) { setBoard(next); setError(null); } })
      .catch((problem) => { if (current) setError(messageOf(problem, 'Não foi possível carregar os documentos.')); });
    return () => { current = false; };
  }, [applicationId, reloadKey]);
  if (error && !board) return <div className="lineup"><Empty title="Não foi possível carregar os documentos" text={error} /></div>;
  if (!board) return <div className="lineup"><p className="empty muted">Carregando documentos…</p></div>;
  return <section className="lineup doc-group" aria-label={`Documentos de ${board.jobTitle}`}>
    <div className="doc-group-head">
      <strong>{board.jobTitle}</strong>
      <Chip tone={board.sentRequired >= board.totalRequired ? 'green' : 'yellow'}>{board.sentRequired} de {board.totalRequired} obrigatórios enviados</Chip>
    </div>
    <h3 className="doc-subhead">Enviados <span>{board.sent.length}</span></h3>
    {board.sent.length > 0
      ? board.sent.map((doc) => <DocumentRow key={doc.id} doc={doc} onDownload={onDownload} onReplace={onReplace} />)
      : <p className="doc-none">Nenhum documento enviado ainda.</p>}
    <h3 className="doc-subhead">Pendentes <span>{board.pending.length}</span></h3>
    {board.pending.length > 0
      ? board.pending.map((item) => <div className="doc-row" key={item.code}>
          <span className="doc-icon"><FileText /></span>
          <div><strong>{item.name}</strong>{item.condition && <small>{item.condition}</small>}</div>
          <div className="doc-side">{item.condition && <Chip tone="yellow">Condicional</Chip>}</div>
        </div>)
      : <p className="doc-none">Nenhum documento pendente.</p>}
  </section>;
}

function DocumentRow({ doc, onDownload, onReplace }: {
  doc: CandidateDocument; onDownload: (doc: CandidateDocument) => Promise<void>;
  /** Só aparece em envios com tipo da lista: trocar o arquivo é reenviar o mesmo tipo. */
  onReplace?: (typeCode: string, file: File) => Promise<string | null>;
}) {
  const [busy, setBusy] = useState<'download' | 'replace' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const replace = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file || !onReplace || !doc.typeCode) return;
    const problem = checkDocumentFile(file);
    if (problem) { setError(problem); return; }
    setError(null); setBusy('replace');
    setError(await onReplace(doc.typeCode, file));
    setBusy(null);
  };
  return <div className="doc-row">
    <span className="doc-icon"><FileText /></span>
    <div><strong>{doc.type}</strong><small>{doc.jobTitle} · {doc.format.toUpperCase()} · {formatBytes(doc.sizeBytes)} · enviado em {formatDate(doc.sentAt)}</small>{error && <small className="field-error" role="alert">{error}</small>}</div>
    <div className="doc-side">
      {doc.typeCode === null && <Chip tone="dashed">tipo antigo</Chip>}
      {onReplace && doc.typeCode && <>
        <input ref={fileInput} type="file" accept=".pdf,.docx" hidden aria-label={`Novo arquivo de ${doc.type}`} onChange={replace} />
        <Button variant="outline" disabled={busy !== null} onClick={() => fileInput.current?.click()}><Upload />{busy === 'replace' ? 'Enviando…' : 'Substituir'}<span className="sr-only"> {doc.type}</span></Button>
      </>}
      <Button variant="outline" disabled={busy !== null} onClick={async () => { setBusy('download'); await onDownload(doc); setBusy(null); }}><Download />{busy === 'download' ? 'Baixando…' : 'Baixar'}<span className="sr-only"> {doc.type}</span></Button>
    </div>
  </div>;
}

/** Documentos do RH e do administrador: uma seção por candidato, com as vagas e os arquivos dele. */
function HrDocumentsPage({ documents, onDownload }: { documents: CandidateDocument[]; onDownload: (doc: CandidateDocument) => Promise<void> }) {
  const groups = useMemo(() => {
    const byCandidate = new Map<string, { id: string; name: string; jobs: string[]; documents: CandidateDocument[] }>();
    for (const doc of documents) {
      const group = byCandidate.get(doc.candidateId) ?? { id: doc.candidateId, name: doc.candidateName, jobs: [], documents: [] };
      if (!group.jobs.includes(doc.jobTitle)) group.jobs.push(doc.jobTitle);
      group.documents.push(doc);
      byCandidate.set(doc.candidateId, group);
    }
    return [...byCandidate.values()].sort((a, b) => a.name.localeCompare(b.name));
  }, [documents]);
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Documentos de contratação</h1><p>Arquivos enviados pelos candidatos aprovados, separados por candidato.</p></div></div>
    {groups.length === 0
      ? <div className="lineup"><Empty title="Nenhum documento enviado" text="Os documentos enviados pelos candidatos aprovados aparecem aqui." /></div>
      : <div className="stack">{groups.map((group) => <section className="lineup doc-group" key={group.id} aria-label={`Documentos de ${group.name}`}>
          <div className="doc-group-head">
            <div className="person"><span className="initials">{initials(group.name)}</span><span><strong>{group.name}</strong><small>{group.jobs.join(' · ')}</small></span></div>
            <Chip>{group.documents.length === 1 ? '1 documento' : `${group.documents.length} documentos`}</Chip>
          </div>
          {group.documents.map((doc) => <DocumentRow key={doc.id} doc={doc} onDownload={onDownload} />)}
        </section>)}</div>}
  </section>;
}

/* ---------- Painel do RH ---------- */
const hrNav: { view: HrView; label: string; icon: typeof Bell }[] = [
  { view: 'jobs', label: 'Vagas', icon: BriefcaseBusiness },
  { view: 'candidates', label: 'Candidatos', icon: UsersRound },
  { view: 'documents', label: 'Documentos', icon: FileText },
];
const settingsNav = { view: 'settings' as const, label: 'Configurações', icon: Settings };

function HrLayout({ active, children, onNavigate, onExit, userName, role, notifications, onReadNotifications, onReadNotification }: { active: HrView; children: ReactNode; onNavigate: (view: HrView) => void; onExit: () => void; userName: string; role: LoginResponse['perfil']; notifications: NotificationItem[]; onReadNotifications: () => void; onReadNotification: (id: string) => void }) {
  // Esconder a aba é só conveniência: quem barra o acesso às Configurações é o backend (403).
  const roleLabel = role === 'administrador' ? 'Administrador' : 'Recursos Humanos';
  const links = (role === 'administrador' ? [...hrNav, settingsNav] : hrNav).map(({ view, label, icon: Icon }) => <button key={view} type="button" aria-current={active === view ? 'page' : undefined} onClick={() => onNavigate(view)}><Icon />{label}</button>);
  return <div className="shell">
    <Glow />
    <header className="glass-nav app-nav">
      <Brand tag="RH" />
      <nav className="topnav" aria-label="Navegação do RH">{links}</nav>
      <div className="topbar-actions">
        <Notifications items={notifications} onRead={onReadNotifications} onReadOne={onReadNotification} />
        <DropdownMenu>
          <DropdownMenuTrigger asChild><button type="button" className="profile-trigger" aria-label="Conta e preferências"><span><strong>{firstName(userName)}</strong><small>{roleLabel}</small></span><span className="initials">{initials(userName)}</span></button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-56">
            <DropdownMenuLabel><strong className="block">{userName}</strong><span className="block font-normal text-muted-foreground">{roleLabel}</span></DropdownMenuLabel>
            <DropdownMenuSeparator />
            <ThemeMenu />
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={onExit}><LogOut />Sair</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
    <main className="hr-main">{children}</main>
    <nav className="tabbar" aria-label="Navegação do RH">{links}</nav>
  </div>;
}

function HrJobsPage({ jobs, onCandidates, onSaved, onClosed }: { jobs: Job[]; onCandidates: (id: string) => void; onSaved: (input: JobInput) => Promise<boolean>; onClosed: (id: string) => Promise<boolean> }) {
  const [tab, setTab] = useState<Job['status'] | 'all'>('aberta'); const [query, setQuery] = useState('');
  const [editor, setEditor] = useState<Job | null | undefined>(undefined);
  const [closing, setClosing] = useState<Job | null>(null);
  const count = (status: Job['status']) => jobs.filter((job) => job.status === status).length;
  const filtered = jobs.filter((job) => (tab === 'all' || job.status === tab) && job.title.toLowerCase().includes(query.trim().toLowerCase()));
  const tabs: [typeof tab, string, number][] = [['aberta', 'Abertas', count('aberta')], ['rascunho', 'Rascunhos', count('rascunho')], ['encerrada', 'Encerradas', count('encerrada')], ['all', 'Todas', jobs.length]];
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Vagas</h1><p>Crie, edite e encerre as vagas publicadas no portal.</p></div><Button size="lg" onClick={() => setEditor(null)}><Plus />Nova vaga</Button></div>
    <div className="toolbar">
      <div className="segmented status-filter" role="group" aria-label="Filtrar por status">{tabs.map(([value, label, total]) => <button key={value} type="button" aria-pressed={tab === value} onClick={() => setTab(value)}>{label}<span className="count">{total}</span></button>)}</div>
      <label className="search"><span className="sr-only">Buscar vaga</span><Search /><Input type="search" placeholder="Buscar vaga" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
    </div>
    <div className="lineup">
      {filtered.length > 0 && <table className="sheet-table">
        <thead><tr><th>Vaga</th><th>Local</th><th>Publicada em</th><th>Status</th><th><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{filtered.map((job) => <tr key={job.id}>
          <td><button type="button" className="row-link" onClick={() => onCandidates(job.id)}><strong>{job.title}</strong><small>{job.workModel} · {job.contract}</small><small className="mobile-meta">{job.city !== job.workModel ? `${job.city} · ` : ''}Publicada em {formatDate(job.publishedAt)}</small></button></td>
          <td data-label="Local">{job.city !== job.workModel ? job.city : '—'}</td>
          <td data-label="Publicada em" className="tabular">{formatDate(job.publishedAt)}</td>
          <td><Chip tone={jobTone[job.status]}>{jobStatus[job.status]}</Chip></td>
          <td className="actions-cell"><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Ações da vaga ${job.title}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="min-w-48">
            <DropdownMenuItem onClick={() => onCandidates(job.id)}><UsersRound />Ver candidatos</DropdownMenuItem>
            <DropdownMenuItem onClick={() => setEditor(job)}><Pencil />Editar vaga</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" disabled={job.status === 'encerrada'} onClick={() => setClosing(job)}><Ban />Encerrar vaga</DropdownMenuItem>
          </DropdownMenuContent></DropdownMenu></td>
        </tr>)}</tbody>
      </table>}
      {filtered.length === 0 && (jobs.length === 0
        ? <Empty title="Nenhuma vaga cadastrada" text="Publique a primeira vaga para os candidatos começarem a se inscrever." action={<Button onClick={() => setEditor(null)}><Plus />Nova vaga</Button>} />
        : <Empty title="Nenhuma vaga neste filtro" text="Troque o status ou a busca para ver outras vagas." />)}
    </div>
    {editor !== undefined && <JobDialog job={editor ?? undefined} onClose={() => setEditor(undefined)} onSave={onSaved} />}
    {closing && <ConfirmDialog title="Encerrar vaga?" text={`“${closing.title}” deixa de aparecer para os candidatos e fica no histórico como encerrada.`} confirm="Encerrar vaga" busyLabel="Encerrando…" onCancel={() => setClosing(null)} onConfirm={async () => { if (await onClosed(closing.id)) setClosing(null); }} />}
  </section>;
}

function ConfirmDialog({ title, text, confirm, busyLabel, onCancel, onConfirm }: { title: string; text: string; confirm: string; busyLabel: string; onCancel: () => void; onConfirm: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  return <Dialog open onOpenChange={(open) => !open && onCancel()}><DialogContent className="job-dialog confirm-dialog">
    <DialogHeader><DialogTitle>{title}</DialogTitle><DialogDescription>{text}</DialogDescription></DialogHeader>
    <DialogFooter><Button variant="outline" onClick={onCancel}>Cancelar</Button><Button variant="destructive" disabled={busy} onClick={async () => { setBusy(true); await onConfirm(); setBusy(false); }}><Ban />{busy ? busyLabel : confirm}</Button></DialogFooter>
  </DialogContent></Dialog>;
}

function JobDialog({ job, onClose, onSave }: { job?: Job; onClose: () => void; onSave: (input: JobInput) => Promise<boolean> }) {
  const [form, setForm] = useState({ title: job?.title ?? '', city: job?.city ?? 'Erechim, RS', workModel: job?.workModel ?? 'Híbrido', contract: job?.contract ?? 'CLT', closesAt: job?.closesAt ?? '', description: job?.description ?? '', requirements: job?.requirements.join('\n') ?? '' });
  const [saving, setSaving] = useState(false);
  const update = (key: keyof typeof form, value: string) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true);
    const ok = await onSave({ id: job?.id, status: job?.status, title: form.title, city: form.city, workModel: form.workModel as Job['workModel'], contract: form.contract as Job['contract'], closesAt: form.closesAt || undefined, description: form.description, requirements: lineList(form.requirements) });
    setSaving(false);
    if (ok) onClose();
  };
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{job ? 'Editar vaga' : 'Nova vaga'}</DialogTitle><DialogDescription>{job ? 'As alterações aparecem no portal assim que você salvar.' : 'A vaga é publicada como aberta assim que você salvar.'}</DialogDescription></DialogHeader>
    <form onSubmit={submit} className="dialog-form">
      <Field label="Título da vaga"><Input required value={form.title} onChange={(event) => update('title', event.target.value)} /></Field>
      <div className="field-row">
        <Field label="Cidade"><Input required value={form.city} onChange={(event) => update('city', event.target.value)} /></Field>
        <Field label="Inscrições até" hint="Opcional"><Input type="date" value={form.closesAt} onChange={(event) => update('closesAt', event.target.value)} /></Field>
        <Field label="Modalidade"><Select value={form.workModel} onValueChange={(value) => update('workModel', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="Presencial">Presencial</SelectItem><SelectItem value="Híbrido">Híbrido</SelectItem><SelectItem value="Remoto">Remoto</SelectItem></SelectContent></Select></Field>
        <Field label="Contrato"><Select value={form.contract} onValueChange={(value) => update('contract', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="CLT">CLT</SelectItem><SelectItem value="Estágio">Estágio</SelectItem><SelectItem value="PJ">PJ</SelectItem><SelectItem value="Temporário">Temporário</SelectItem></SelectContent></Select></Field>
      </div>
      <Field label="Descrição"><Textarea required value={form.description} onChange={(event) => update('description', event.target.value)} /></Field>
      <Field label="Requisitos" hint="Um por linha"><Textarea required value={form.requirements} onChange={(event) => update('requirements', event.target.value)} /></Field>
      <DialogFooter><Button type="button" variant="outline" onClick={onClose}>Cancelar</Button><Button type="submit" disabled={saving}>{saving ? 'Salvando…' : job ? 'Salvar alterações' : 'Publicar vaga'}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}

type ScheduleProblem = { field: boolean; message: string } | null;

function HrCandidatesPage({ job, jobs, reloadKey, onSelectJob, onBack, onUpdate, onSchedule, onDownload }: {
  job: Job; jobs: Job[];
  /** Sobe quando chega uma notificação: a lista de inscritos e os quadros abertos recarregam. */
  reloadKey: number;
  onSelectJob: (id: string) => void; onBack: () => void;
  onUpdate: (id: string, status: ApplicationStatus) => Promise<boolean>;
  onSchedule: (applicationId: string, dateTime: string) => Promise<ScheduleProblem>;
  onDownload: (doc: CandidateDocument) => Promise<void>;
}) {
  const [candidates, setCandidates] = useState<Candidate[] | null>(null); const [loadError, setLoadError] = useState<string | null>(null);
  const [query, setQuery] = useState(''); const [status, setStatus] = useState('all');
  const [rejecting, setRejecting] = useState<Candidate | null>(null);
  const [viewing, setViewing] = useState<Candidate | null>(null);
  const [scheduling, setScheduling] = useState<Candidate | null>(null);
  const [documentsOf, setDocumentsOf] = useState<Candidate | null>(null);
  const load = () => portalService.getCandidates(job.id).then((items) => { setCandidates(items); setLoadError(null); }).catch((error) => setLoadError(messageOf(error, 'Não foi possível carregar os inscritos.')));
  useEffect(() => { setCandidates(null); setLoadError(null); void load(); }, [job.id]);
  useEffect(() => { if (reloadKey > 0) void load(); }, [reloadKey]); // eslint-disable-line react-hooks/exhaustive-deps
  const retry = () => { setLoadError(null); void load(); };
  const update = async (id: string, next: ApplicationStatus) => { if (await onUpdate(id, next)) void load(); };
  const filtered = (candidates ?? []).filter((candidate) => candidate.name.toLowerCase().includes(query.trim().toLowerCase()) && (status === 'all' || candidate.status === status));
  return <section className="hr-page">
    <button type="button" className="back-link" onClick={onBack}><ChevronLeft />Vagas</button>
    <div className="page-head">
      <div><h1 className="display">Candidatos</h1><p>{candidates === null ? (loadError ? 'Inscritos indisponíveis.' : 'Carregando inscritos…') : `${candidates.length} ${candidates.length === 1 ? 'inscrito' : 'inscritos'} · publicada em ${formatDate(job.publishedAt)}`}</p></div>
      <Select value={job.id} onValueChange={onSelectJob}><SelectTrigger className="job-picker" aria-label="Vaga"><SelectValue /></SelectTrigger><SelectContent>{jobs.map((item) => <SelectItem key={item.id} value={item.id}>{item.title}</SelectItem>)}</SelectContent></Select>
    </div>
    <div className="toolbar">
      <label className="search"><span className="sr-only">Buscar candidato</span><Search /><Input type="search" placeholder="Buscar candidato" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
      <Select value={status} onValueChange={setStatus}><SelectTrigger className="min-w-48" aria-label="Etapa"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">Todas as etapas</SelectItem>{Object.entries(appStatus).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select>
    </div>
    <div className="lineup">
      {filtered.length > 0 && <table className="sheet-table plain">
        <thead><tr><th>Candidato</th><th>Inscrição</th><th>Etapa</th><th><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{filtered.map((candidate) => <tr key={candidate.applicationId}>
          <td className="lead-cell"><div className="person"><span className="initials">{initials(candidate.name)}</span><span><strong>{candidate.name}</strong><small>{candidate.email}</small></span></div></td>
          <td data-label="Inscrição" className="tabular">{formatDate(candidate.submittedAt)}</td>
          <td><Chip tone={appTone[candidate.status]}>{appStatus[candidate.status]}</Chip>{candidate.status === 'interview' && candidate.interviewAt && <small>Entrevista em {formatDateTime(candidate.interviewAt)}</small>}</td>
          <td className="actions-cell"><CandidateMenu candidate={candidate} onUpdate={update} onReject={setRejecting} onResume={setViewing} onSchedule={setScheduling} onDocuments={setDocumentsOf} /></td>
        </tr>)}</tbody>
      </table>}
      {candidates !== null && filtered.length === 0 && (candidates.length === 0
        ? <Empty title="Nenhum inscrito ainda" text="Quando alguém se candidatar a esta vaga, aparece aqui." />
        : <Empty title="Nenhum candidato neste filtro" text="Troque a etapa ou a busca." />)}
      {candidates === null && loadError && <Empty title="Não foi possível carregar os inscritos" text={loadError} action={<Button variant="outline" onClick={retry}>Tentar novamente</Button>} />}
      {candidates === null && !loadError && <p className="empty muted">Carregando inscritos…</p>}
    </div>
    {viewing && <ResumeDialog candidate={viewing} onClose={() => setViewing(null)} />}
    {documentsOf && <CandidateDocumentsDialog candidate={documentsOf} reloadKey={reloadKey} onDownload={onDownload} onClose={() => setDocumentsOf(null)} />}
    {scheduling && <InterviewDialog candidate={scheduling} onClose={() => setScheduling(null)} onSave={async (dateTime) => { const problem = await onSchedule(scheduling.applicationId, dateTime); if (!problem) void load(); return problem; }} />}
    {rejecting && <ConfirmDialog title="Não selecionar candidato?" text={`${rejecting.name} verá a candidatura como “Não selecionado” nesta vaga.`} confirm="Não selecionar" busyLabel="Salvando…" onCancel={() => setRejecting(null)} onConfirm={async () => { await update(rejecting.applicationId, 'rejected'); setRejecting(null); }} />}
  </section>;
}

function CandidateMenu({ candidate, onUpdate, onReject, onResume, onSchedule, onDocuments }: { candidate: Candidate; onUpdate: (id: string, status: ApplicationStatus) => void; onReject: (candidate: Candidate) => void; onResume: (candidate: Candidate) => void; onSchedule: (candidate: Candidate) => void; onDocuments: (candidate: Candidate) => void }) {
  return <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Ações para ${candidate.name}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="min-w-56">
    <DropdownMenuItem onClick={() => onResume(candidate)}><FileText />Ver currículo</DropdownMenuItem>
    <DropdownMenuItem onClick={() => onDocuments(candidate)}><Inbox />Ver documentos</DropdownMenuItem>
    <DropdownMenuSeparator />
    <DropdownMenuItem disabled={candidate.status === 'reviewing'} onClick={() => onUpdate(candidate.applicationId, 'reviewing')}><ClipboardList />Mover para análise</DropdownMenuItem>
    <DropdownMenuItem onClick={() => onSchedule(candidate)}><CalendarDays />{candidate.status === 'interview' ? 'Reagendar entrevista' : 'Chamar para entrevista'}</DropdownMenuItem>
    <DropdownMenuItem disabled={candidate.status === 'approved'} onClick={() => onUpdate(candidate.applicationId, 'approved')}><Check />Aprovar candidato</DropdownMenuItem>
    <DropdownMenuSeparator />
    <DropdownMenuItem disabled={candidate.status === 'rejected'} variant="destructive" onClick={() => onReject(candidate)}><Ban />Não selecionar candidato</DropdownMenuItem>
  </DropdownMenuContent></DropdownMenu>;
}

/** Agora, no horário de Brasília, no formato de um input datetime-local (para o `min`). */
function nowInBrasilia() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date()).replace(' ', 'T');
}

/** Marca a entrevista: o backend leva o candidato para a etapa Entrevista e o avisa com a data e a hora. */
function InterviewDialog({ candidate, onClose, onSave }: { candidate: Candidate; onClose: () => void; onSave: (dateTime: string) => Promise<ScheduleProblem> }) {
  const [value, setValue] = useState('');
  const [problem, setProblem] = useState<ScheduleProblem>(null);
  const [saving, setSaving] = useState(false);
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!value) { setProblem({ field: true, message: 'Informe a data e a hora da entrevista.' }); return; }
    setSaving(true);
    const result = await onSave(value);
    setSaving(false);
    if (result) setProblem(result); else onClose();
  };
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog confirm-dialog">
    <DialogHeader><DialogTitle>{candidate.status === 'interview' ? 'Reagendar entrevista' : 'Chamar para entrevista'}</DialogTitle><DialogDescription>{candidate.name} recebe uma notificação com a data e a hora escolhidas.</DialogDescription></DialogHeader>
    <form onSubmit={submit} className="dialog-form" noValidate>
      {problem && !problem.field && <FormError title="Não foi possível marcar" text={problem.message} />}
      <Field label="Data e hora" hint="Horário de Brasília" error={problem?.field ? problem.message : undefined}><Input type="datetime-local" min={nowInBrasilia()} value={value} aria-invalid={problem?.field || undefined} onChange={(event) => { setValue(event.target.value); setProblem(null); }} /></Field>
      <DialogFooter><Button type="button" variant="outline" onClick={onClose}>Cancelar</Button><Button type="submit" disabled={saving}>{saving ? 'Marcando…' : 'Marcar entrevista'}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}

/** O RH abre um candidato da vaga e vê o mesmo quadro de documentos que o candidato vê. */
function CandidateDocumentsDialog({ candidate, reloadKey, onDownload, onClose }: { candidate: Candidate; reloadKey: number; onDownload: (doc: CandidateDocument) => Promise<void>; onClose: () => void }) {
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{candidate.name}</DialogTitle><DialogDescription>{candidate.email} · documentos de contratação</DialogDescription></DialogHeader>
    <DocumentBoardView applicationId={candidate.applicationId} reloadKey={reloadKey} onDownload={onDownload} />
    <DialogFooter><Button variant="outline" onClick={onClose}>Fechar</Button></DialogFooter>
  </DialogContent></Dialog>;
}

/** Currículo do inscrito, somente leitura. O backend só libera para o RH da vaga e para o administrador. */
function ResumeDialog({ candidate, onClose }: { candidate: Candidate; onClose: () => void }) {
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  useEffect(() => { portalService.getProfile(candidate.id).then(setProfile).catch((problem) => setError(messageOf(problem, 'Não foi possível carregar o currículo.'))); }, [candidate.id]);
  const download = async () => {
    if (!profile?.id) return;
    setDownloading(true);
    try { saveBlob(await portalService.downloadResumeFile(profile.id), profile.arquivo?.nomeOriginal ?? 'curriculo.pdf'); }
    catch (problem) { setError(messageOf(problem, 'Não foi possível baixar o PDF.')); }
    setDownloading(false);
  };
  const period = (start: string, end: string, ongoing: string) => `${formatDate(start)} a ${end ? formatDate(end) : ongoing}`;
  const facts: [string, string][] = profile ? ([
    ['Telefone', profile.numeroContato],
    ['LinkedIn', profile.perfilLinkedin],
    ['Idade', profile.idade != null ? `${profile.idade} anos` : ''],
    ['Sexo', sexoOptions.find((option) => option.value === profile.sexo)?.label ?? ''],
    ['Cidade', [profile.cidade, profile.uf].filter(Boolean).join(', ')],
  ] as [string, string][]).filter(([, value]) => value) : [];
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{candidate.name}</DialogTitle><DialogDescription>{candidate.email} · currículo somente para leitura</DialogDescription></DialogHeader>
    {error && <FormError title="Não foi possível abrir o currículo" text={error} />}
    {!error && !profile && <p className="empty muted">Carregando currículo…</p>}
    {profile && !profile.id && <Empty title="Currículo não encontrado" text="Esta pessoa ainda não preencheu o currículo." />}
    {profile?.id && <div className="resume-view">
      {facts.length > 0 && <section><h3>Contato e dados pessoais</h3><dl className="resume-facts">{facts.map(([label, value]) => <Fragment key={label}><dt>{label}</dt><dd>{value}</dd></Fragment>)}</dl></section>}
      {profile.resumo && <section><h3>Resumo profissional</h3><p>{profile.resumo}</p></section>}
      {profile.experiencias.length > 0 && <section><h3>Experiência profissional</h3>{profile.experiencias.map((item) => <div className="resume-item" key={item.uid}><strong>{item.cargo}</strong><small>{item.empresa} · {period(item.dataContratacao, item.trabalhoAtual ? '' : item.dataDemissao, 'atual')}</small>{item.descricaoAtividades && <p>{item.descricaoAtividades}</p>}</div>)}</section>}
      {profile.formacoes.length > 0 && <section><h3>Formação acadêmica</h3>{profile.formacoes.map((item) => <div className="resume-item" key={item.uid}><strong>{item.curso}</strong><small>{item.instituicao} · {period(item.dataInicio, item.dataTermino, 'em andamento')}</small></div>)}</section>}
      {profile.skills.length > 0 && <section><h3>Competências</h3><div className="tags">{profile.skills.map((skill) => <span className="tag static" key={skill}>{skill}</span>)}</div></section>}
      {profile.certificacoes && <section><h3>Certificações</h3><p>{profile.certificacoes}</p></section>}
      <section><h3>Currículo em PDF</h3>
        {profile.arquivo
          ? <div className="file-card"><span className="doc-icon"><FileText /></span><div><strong>{profile.arquivo.nomeOriginal}</strong><small>{formatBytes(profile.arquivo.tamanhoBytes)} · enviado em {formatDate(profile.arquivo.enviadoEm)}</small></div><Button type="button" variant="outline" size="sm" disabled={downloading} onClick={() => void download()}><Download />{downloading ? 'Baixando…' : 'Baixar'}</Button></div>
          : <p className="repeat-empty">Nenhum PDF anexado.</p>}
      </section>
    </div>}
    <DialogFooter><Button variant="outline" onClick={onClose}>Fechar</Button></DialogFooter>
  </DialogContent></Dialog>;
}

/* ---------- Configurações (administrador) ---------- */
function SettingsPage({ currentUserId, onNotice }: { currentUserId: string; onNotice: (notice: Notice) => void }) {
  const [users, setUsers] = useState<StaffUser[] | null>(null); const [loadError, setLoadError] = useState<string | null>(null);
  const [editor, setEditor] = useState<StaffUser | null | undefined>(undefined);
  const [deleting, setDeleting] = useState<StaffUser | null>(null);
  const load = () => portalService.getUsers().then((items) => { setUsers(items); setLoadError(null); }).catch((error) => setLoadError(messageOf(error, 'Não foi possível carregar os usuários.')));
  useEffect(() => { void load(); }, []);
  /** Salva no backend e recarrega a lista. Devolve a mensagem de erro para o formulário, ou null. */
  const save = async (input: StaffUserInput, id?: string) => {
    try {
      if (id) await portalService.updateUser(id, input); else await portalService.createUser(input);
      onNotice({ tone: 'ok', text: id ? 'Usuário atualizado.' : 'Usuário criado.' });
      await load();
      return null;
    } catch (error) { return messageOf(error, 'Não foi possível salvar o usuário. Tente novamente.'); }
  };
  const toggleBlock = async (user: StaffUser) => {
    const blocking = user.status !== 'bloqueado';
    try {
      await portalService.updateUser(user.id, { name: user.name, email: user.email, role: user.role, status: blocking ? 'bloqueado' : 'ativo' });
      onNotice({ tone: 'ok', text: blocking ? `${user.name} foi bloqueado.` : `${user.name} foi desbloqueado.` });
      await load();
    } catch (error) { onNotice({ tone: 'error', text: messageOf(error, 'Não foi possível alterar o usuário. Tente novamente.') }); }
  };
  const remove = async (user: StaffUser) => {
    try {
      await portalService.deleteUser(user.id);
      onNotice({ tone: 'ok', text: `${user.name} foi excluído.` });
      await load();
    } catch (error) { onNotice({ tone: 'error', text: messageOf(error, 'Não foi possível excluir o usuário. Tente novamente.') }); }
    setDeleting(null);
  };
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Configurações</h1><p>Quem acessa o painel do RH e o que cada pessoa pode fazer.</p></div><Button size="lg" onClick={() => setEditor(null)}><Plus />Novo usuário</Button></div>
    <p className="info-note"><Info /><span><strong>Perfil e status valem no próximo login.</strong> O acesso de cada pessoa dura até 8 horas, então uma mudança só aparece para ela quando entrar de novo.</span></p>
    <div className="lineup">
      {users && users.length > 0 && <table className="sheet-table plain">
        <thead><tr><th>Usuário</th><th>Perfil</th><th>Status</th><th><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{users.map((user) => {
          const self = user.id === currentUserId;
          return <tr key={user.id}>
            <td className="lead-cell"><div className="person"><span className="initials">{initials(user.name)}</span><span><strong>{user.name}{self && ' (você)'}</strong><small>{user.email}</small></span></div></td>
            <td data-label="Perfil">{staffRoles[user.role]}</td>
            <td><Chip tone={userTone[user.status]}>{userStatus[user.status]}</Chip></td>
            <td className="actions-cell"><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Ações para ${user.name}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="min-w-52">
              <DropdownMenuItem onClick={() => setEditor(user)}><Pencil />Editar usuário</DropdownMenuItem>
              <DropdownMenuItem disabled={self} onClick={() => void toggleBlock(user)}><Ban />{user.status === 'bloqueado' ? 'Desbloquear' : 'Bloquear'}</DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" disabled={self} onClick={() => setDeleting(user)}><Trash2 />Excluir usuário</DropdownMenuItem>
            </DropdownMenuContent></DropdownMenu></td>
          </tr>;
        })}</tbody>
      </table>}
      {users && users.length === 0 && <Empty title="Nenhum usuário do RH" text="Crie o primeiro usuário para dar acesso ao painel." action={<Button onClick={() => setEditor(null)}><Plus />Novo usuário</Button>} />}
      {!users && loadError && <Empty title="Não foi possível carregar os usuários" text={loadError} action={<Button variant="outline" onClick={() => { setLoadError(null); void load(); }}>Tentar novamente</Button>} />}
      {!users && !loadError && <p className="empty muted">Carregando usuários…</p>}
    </div>
    {editor !== undefined && <UserDialog user={editor ?? undefined} self={editor?.id === currentUserId} onClose={() => setEditor(undefined)} onSave={save} />}
    {deleting && <ConfirmDialog title="Excluir usuário?" text={`${deleting.name} perde o acesso e o cadastro é removido. Quem já tem vagas ou candidaturas não pode ser excluído: nesse caso, bloqueie o acesso.`} confirm="Excluir usuário" busyLabel="Excluindo…" onCancel={() => setDeleting(null)} onConfirm={() => remove(deleting)} />}
  </section>;
}

function UserDialog({ user, self, onClose, onSave }: { user?: StaffUser; self: boolean; onClose: () => void; onSave: (input: StaffUserInput, id?: string) => Promise<string | null> }) {
  const [form, setForm] = useState({ name: user?.name ?? '', email: user?.email ?? '', password: '', role: user?.role ?? 'rh', status: user?.status ?? 'ativo' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const update = (key: keyof typeof form, value: string) => { setForm((current) => ({ ...current, [key]: value })); setError(null); };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!user && form.password.length < 6) { setError('A senha deve ter no mínimo 6 caracteres.'); return; }
    if (user && form.password && form.password.length < 6) { setError('A nova senha deve ter no mínimo 6 caracteres.'); return; }
    setSaving(true);
    const problem = await onSave({ name: form.name, email: form.email, role: form.role as StaffUser['role'], status: form.status as StatusUsuario, password: form.password || undefined }, user?.id);
    setSaving(false);
    if (problem) setError(problem); else onClose();
  };
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{user ? 'Editar usuário' : 'Novo usuário'}</DialogTitle><DialogDescription>{user ? 'Mudanças de perfil e status valem no próximo login da pessoa.' : 'A pessoa entra com este e-mail e a senha definida aqui.'}</DialogDescription></DialogHeader>
    <form onSubmit={submit} className="dialog-form">
      {error && <FormError title="Não foi possível salvar" text={error} />}
      <div className="field-row">
        <Field label="Nome completo"><Input required maxLength={150} autoComplete="off" value={form.name} onChange={(event) => update('name', event.target.value)} /></Field>
        <Field label="E-mail"><Input required type="email" inputMode="email" maxLength={150} autoComplete="off" value={form.email} onChange={(event) => update('email', event.target.value)} /></Field>
      </div>
      <Field label={user ? 'Nova senha' : 'Senha'} hint={user ? 'Deixe em branco para manter a atual' : 'Mínimo de 6 caracteres'}><PasswordInput autoComplete="new-password" required={!user} value={form.password} onChange={(event) => update('password', event.target.value)} /></Field>
      <div className="field-row">
        <Field label="Perfil" hint={self ? 'Você não altera o próprio' : undefined}><Select value={form.role} disabled={self} onValueChange={(value) => update('role', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="rh">RH</SelectItem><SelectItem value="administrador">Administrador</SelectItem></SelectContent></Select></Field>
        {user && <Field label="Status" hint={self ? 'Você não altera o próprio' : undefined}><Select value={form.status} disabled={self} onValueChange={(value) => update('status', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent>{Object.entries(userStatus).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select></Field>}
      </div>
      <DialogFooter><Button type="button" variant="outline" onClick={onClose}>Cancelar</Button><Button type="submit" disabled={saving}>{saving ? 'Salvando…' : user ? 'Salvar alterações' : 'Criar usuário'}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}

/* ---------- Peças compartilhadas ---------- */
function Brand({ tag }: { tag?: string }) {
  return <span className="brand"><span className="brand-orb" aria-hidden="true" /><span className="brand-word">TeamUp</span>{tag && <span className="brand-tag">{tag}</span>}</span>;
}
function CtaArrow() { return <span className="cta-arrow" aria-hidden="true"><ChevronRight /></span>; }

function ThemeSwitch() {
  const pref = useThemePref();
  const options: [ThemePref, string, typeof Sun][] = [['system', 'Seguir o sistema', Monitor], ['light', 'Tema claro', Sun], ['dark', 'Tema escuro', Moon]];
  return <div className="segmented theme-switch" role="radiogroup" aria-label="Tema">{options.map(([value, label, Icon]) => <button key={value} type="button" role="radio" aria-checked={pref === value} aria-label={label} title={label} onClick={() => setThemePref(value)}><Icon /></button>)}</div>;
}

function ThemeMenu() {
  const pref = useThemePref();
  return <>
    <DropdownMenuLabel className="font-normal text-muted-foreground">Tema</DropdownMenuLabel>
    <DropdownMenuRadioGroup value={pref} onValueChange={(value) => setThemePref(value as ThemePref)}>
      <DropdownMenuRadioItem value="system"><Monitor />Seguir o sistema</DropdownMenuRadioItem>
      <DropdownMenuRadioItem value="light"><Sun />Claro</DropdownMenuRadioItem>
      <DropdownMenuRadioItem value="dark"><Moon />Escuro</DropdownMenuRadioItem>
    </DropdownMenuRadioGroup>
  </>;
}

function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: ReactNode }) { return <label className="field"><span>{label}{hint && <span className="hint"> · {hint}</span>}</span>{children}{error && <small className="field-error" role="alert">{error}</small>}</label>; }
function PasswordInput(props: ComponentProps<'input'>) {
  const [visible, setVisible] = useState(false);
  return <div className="password"><Input {...props} type={visible ? 'text' : 'password'} /><button type="button" onClick={() => setVisible((current) => !current)} aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={visible}>{visible ? <EyeOff /> : <Eye />}</button></div>;
}
function FormError({ title, text }: { title: string; text: string }) { return <div className="form-error" role="alert"><CircleAlert /><div><strong>{title}</strong>{text}</div></div>; }
function Chip({ tone, children }: { tone?: Tone; children: ReactNode }) { return <span className="chip" data-tone={tone}>{children}</span>; }
function JobMeta({ job, withDate = false }: { job: Job; withDate?: boolean }) { return <span className="meta">{job.city !== job.workModel && <span><MapPin />{job.city}</span>}<span><BriefcaseBusiness />{job.workModel} · {job.contract}</span>{withDate && <span><CalendarDays />Publicada em {formatDate(job.publishedAt)}</span>}</span>; }
function Empty({ title, text, action }: { title: string; text: string; action?: ReactNode }) {
  return <div className="empty"><span className="empty-icon"><Inbox /></span><h2>{title}</h2><p>{text}</p>{action}</div>;
}

function stepFor(status: ApplicationStatus) { return ({ applied: 1, reviewing: 2, interview: 3, approved: 4, rejected: 2, hired: 4, cancelled: 1 })[status]; }
/** Entrega um arquivo baixado como blob (a API exige o token, então não dá para usar um link direto). */
function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = filename;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function initials(name: string) { return name.split(' ').filter(Boolean).slice(0, 2).map((item) => item[0]).join('').toUpperCase(); }
function firstName(name: string) { return name.split(' ')[0]; }
function lineList(value: string) { return value.split('\n').map((item) => item.trim()).filter(Boolean); }
function messageOf(error: unknown, fallback: string) { return error instanceof Error && error.message ? error.message : fallback; }

export default App;
