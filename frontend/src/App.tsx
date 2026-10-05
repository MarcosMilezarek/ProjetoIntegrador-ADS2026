import { useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { portalService } from '@/services/portal-service';
import { ApiError, setAuthToken, setUnauthorizedHandler } from '@/lib/api-client';
import type { Application, CandidateDocument, CandidateProfile, Job, NotificationItem } from '@/types/domain';
import { authService } from '@/types/auth-service';
import type { LoginResponse } from '@/types/auth';
import { hashOf, routeFromHash } from '@/lib/routes';
import type { CandidateView, HrView } from '@/lib/routes';
import { Toast, LoadingScreen, Empty } from '@/components/common';
import type { Notice } from '@/components/common';
import { HrJobsPage } from '@/pages/hr/jobs';
import type { JobInput } from '@/pages/hr/jobs';
import { readSession, writeSession } from '@/lib/session';
import { Login, Signup } from '@/pages/auth';
import { CandidateLayout } from '@/pages/candidate/layout';
import { JobsPage, JobDetail, SuccessPage } from '@/pages/candidate/jobs';
import { ResumePage } from '@/pages/candidate/resume';
import { ApplicationsPage } from '@/pages/candidate/applications';
import { DocumentsPage } from '@/pages/candidate/documents';
import { HrDocumentsPage } from '@/pages/hr/documents';
import { HrLayout } from '@/pages/hr/layout';
import { HrCandidatesPage } from '@/pages/hr/candidates';
import { HrAgendaPage } from '@/pages/hr/agenda';
import { HrEmployeesPage } from '@/pages/hr/employees';
import { SettingsPage } from '@/pages/hr/settings';
import { saveBlob, messageOf } from '@/lib/helpers';

type AppMode = 'login' | 'signup' | 'candidate' | 'hr';

const sessionExpired = 'Sua sessão terminou por segurança. Entre novamente para continuar.';

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
      const text = messageOf(error, 'Não conseguimos carregar os dados agora. Tente novamente em instantes.');
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
      setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos concluir a ação agora. Tente novamente em instantes.') });
      return false;
    }
  };
  const selectedJob = jobs.find((job) => job.id === selectedJobId) ?? jobs.find((job) => job.status !== 'encerrada') ?? jobs[0];
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
  useEffect(() => { setUnauthorizedHandler(() => signOut(sessionExpired)); return () => setUnauthorizedHandler(null); }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
      if (me.perfil !== saved.perfil) { signOut(sessionExpired); return; }
      const account: LoginResponse = { id: me.id, nome: me.nome, email: me.email, perfil: me.perfil, token: saved.token };
      writeSession(account);
      setCurrentUser(account);
      applyRoute(account.perfil);
      setRestoring(false);
    } catch (error) {
      if (error instanceof ApiError && [401, 403, 404].includes(error.status)) signOut(sessionExpired);
      else setRestoreError(messageOf(error, 'Não conseguimos conectar ao servidor. Verifique sua conexão com a internet e tente novamente em instantes.'));
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
      setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos salvar seu currículo agora. O que você preencheu continua na tela: tente salvar de novo em instantes.') });
      return { ok: false };
    }
  };
  /** Salva a vaga; erros de validação (400 com `campos`) voltam para o formulário marcar cada campo. */
  const saveJob = async (input: JobInput): Promise<{ ok: true } | { ok: false; campos?: Record<string, string> }> => {
    if (!currentUser) return { ok: false };
    try {
      await portalService.saveJob(input);
      await refresh(currentUser);
      setNotice({ tone: 'ok', text: input.id ? 'Vaga atualizada.' : 'Vaga publicada.' });
      return { ok: true };
    } catch (error) {
      if (error instanceof ApiError && error.campos) {
        setNotice({ tone: 'error', text: 'Revise os campos destacados na vaga.' });
        return { ok: false, campos: error.campos };
      }
      setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos salvar a vaga agora. O que você preencheu continua na tela: tente salvar de novo em instantes.') });
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
      return messageOf(error, 'Não conseguimos enviar o PDF agora. Tente novamente em instantes.');
    }
  };
  const downloadResume = async () => {
    if (!profile?.id) return;
    try { saveBlob(await portalService.downloadResumeFile(profile.id), profile.arquivo?.nomeOriginal ?? 'curriculo.pdf'); }
    catch (error) { setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos baixar o PDF agora. Tente novamente em instantes.') }); }
  };
  /** Envia um documento de contratação. Devolve a mensagem de erro para o formulário, ou null quando foi aceito. */
  const uploadDocument = async (applicationId: string, typeCode: string, file: File) => {
    try {
      await portalService.uploadDocument(applicationId, typeCode, file);
      setNotice({ tone: 'ok', text: 'Documento enviado.' });
      return null;
    } catch (error) { return messageOf(error, 'Não conseguimos enviar o documento agora. Tente novamente em instantes.'); }
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
    catch (error) { setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos baixar o documento agora. Tente novamente em instantes.') }); }
  };
  /** Candidato confirma a presença na entrevista; a candidatura na tela passa a ser a da resposta. */
  const confirmPresence = async (applicationId: string) => {
    try {
      const updated = await portalService.confirmPresence(applicationId);
      setApplications((items) => items.map((item) => item.id === updated.id ? updated : item));
      setNotice({ tone: 'ok', text: 'Presença confirmada.' });
    } catch (error) { setNotice({ tone: 'error', text: messageOf(error, 'Não conseguimos confirmar sua presença agora. Tente novamente em instantes.') }); }
  };
  // O RH revisa um documento e contrata. Os dois recarregam os dados; o servidor valida as regras e a mensagem do 409 aparece no aviso.
  const reviewDocument = (doc: CandidateDocument, approve: boolean) => act(() => approve ? portalService.approveDocument(doc.id) : portalService.refuseDocument(doc.id), approve ? 'Documento aprovado.' : 'Documento recusado.');
  const hire = (applicationId: string) => act(() => portalService.hire(applicationId), 'Candidato contratado. Ele agora aparece em Funcionários.');
  return <>
    {mode === 'candidate' && profile && <CandidateLayout user={currentUser} active={candidateView} onNavigate={navigateCandidate} onExit={exit} notifications={notifications} onReadNotifications={markRead} onReadNotification={markOneRead}>
      {candidateView === 'jobs' && <JobsPage jobs={jobs} applications={applications} resumeEmpty={!profile.id} onEditResume={() => navigateCandidate('resume')} onOpen={(id) => { setSelectedJobId(id); navigateCandidate('detail'); }} />}
      {candidateView === 'detail' && (detailJob
        ? <JobDetail job={detailJob} profile={profile} userName={currentUser.nome} applied={applications.some((item) => item.jobId === detailJob.id)} onBack={() => navigateCandidate('jobs')} onEditResume={() => navigateCandidate('resume')} onApply={async () => { if (await act(() => portalService.apply(detailJob.id))) navigateCandidate('success'); }} />
        : <main className="page"><Empty title="Esta vaga não está mais disponível" text="Ela pode ter sido encerrada, mas há outras oportunidades abertas esperando por você." action={<Button onClick={() => navigateCandidate('jobs')}>Ver vagas</Button>} /></main>)}
      {candidateView === 'success' && <SuccessPage job={selectedJob} onApplications={() => navigateCandidate('applications')} onJobs={() => navigateCandidate('jobs')} />}
      {candidateView === 'resume' && <ResumePage profile={profile} userName={currentUser.nome} userEmail={currentUser.email} onSave={saveResume} onUpload={uploadResume} onDownload={downloadResume} />}
      {candidateView === 'applications' && <ApplicationsPage applications={applications} onDocuments={() => navigateCandidate('documents')} onJobs={() => navigateCandidate('jobs')} onConfirmPresence={confirmPresence} />}
      {candidateView === 'documents' && <DocumentsPage applications={applications} reloadKey={liveVersion} onBack={() => navigateCandidate('applications')} onUpload={uploadDocument} onDownload={downloadDocument} />}
    </CandidateLayout>}
    {mode === 'hr' && <HrLayout userName={currentUser.nome} role={currentUser.perfil} active={hrView} onNavigate={navigateHr} onExit={exit} notifications={notifications} onReadNotifications={markRead} onReadNotification={markOneRead}>
      {hrView === 'jobs' && <HrJobsPage jobs={jobs} onCandidates={(id) => { setSelectedJobId(id); navigateHr('candidates'); }} onSaved={saveJob} onClosed={(id) => act(() => portalService.closeJob(id), 'Vaga encerrada.')} />}
      {hrView === 'candidates' && (selectedJob
        ? <HrCandidatesPage job={selectedJob} jobs={jobs} documents={documents} reloadKey={liveVersion} onSelectJob={setSelectedJobId} onBack={() => navigateHr('jobs')} onUpdate={(id, status) => act(() => portalService.updateApplicationStatus(id, status), 'Etapa do candidato atualizada.')} onSchedule={scheduleInterview} onDownload={downloadDocument} onReview={reviewDocument} onHire={hire} />
        : <section className="hr-page"><Empty title="Nenhuma vaga cadastrada" text="Crie uma vaga para começar a receber candidatos." action={<Button onClick={() => navigateHr('jobs')}>Ir para vagas</Button>} /></section>)}
      {hrView === 'agenda' && <HrAgendaPage reloadKey={liveVersion} />}
      {hrView === 'documents' && <HrDocumentsPage documents={documents} onDownload={downloadDocument} onReview={reviewDocument} />}
      {hrView === 'employees' && <HrEmployeesPage onNotice={setNotice} onDownload={downloadDocument} />}
      {hrView === 'settings' && currentUser.perfil === 'administrador' && <SettingsPage currentUserId={String(currentUser.id)} onNotice={setNotice} />}
    </HrLayout>}
    <Toast notice={notice} onClose={closeNotice} />
  </>;
}


export default App;
