import { Fragment, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Ban, Sparkles, CalendarDays, Check, ChevronLeft, ClipboardList, Download, FileText, Inbox, MoreHorizontal, Search, UserCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { portalService } from '@/services/portal-service';
import { formatDate, formatDateTime } from '@/lib/utils';
import type { AnaliseCandidatura, ApplicationStatus, Candidate, CandidateDocument, CandidateProfile, DocumentType, Job } from '@/types/domain';
import { appStatus, appTone } from '@/lib/status';
import { sexoOptions } from '@/pages/candidate/resume';
import { formatBytes, nowInBrasilia, saveBlob, initials, messageOf } from '@/lib/helpers';
import { DocumentBoardView } from '@/components/documents';
import type { ReviewDocument } from '@/components/documents';
import { ConfirmDialog, Field, FormError, Chip, Empty } from '@/components/common';

type ScheduleProblem = { field: boolean; message: string } | null;

/**
 * Por que o botão Contratar está desabilitado, ou null quando pode contratar: a candidatura está aprovada e todo tipo
 * obrigatório tem documento aprovado nela. Só orienta a tela; quem decide é o servidor (409).
 */
function hireBlock(candidate: Candidate, types: DocumentType[] | null, documents: CandidateDocument[]): string | null {
  if (candidate.status !== 'approved') return 'Disponível depois que o candidato for aprovado.';
  if (!types) return 'Conferindo os documentos obrigatórios.';
  const missing = types.filter((type) => type.required && !documents.some((doc) => doc.applicationId === candidate.applicationId && doc.typeCode === type.code && doc.status === 'aprovado')).length;
  if (missing === 0) return null;
  return missing === 1 ? 'Falta 1 documento obrigatório aprovado.' : `Faltam ${missing} documentos obrigatórios aprovados.`;
}

export function HrCandidatesPage({ job, jobs, documents, reloadKey, onSelectJob, onBack, onUpdate, onSchedule, onDownload, onReview, onHire }: {
  job: Job; jobs: Job[];
  /** Documentos das vagas do RH (GET /documentos): servem à regra do botão Contratar. */
  documents: CandidateDocument[];
  /** Sobe quando chega uma notificação: a lista de inscritos e os quadros abertos recarregam. */
  reloadKey: number;
  onSelectJob: (id: string) => void; onBack: () => void;
  onUpdate: (id: string, status: ApplicationStatus) => Promise<boolean>;
  onSchedule: (applicationId: string, dateTime: string) => Promise<ScheduleProblem>;
  onDownload: (doc: CandidateDocument) => Promise<void>;
  onReview: ReviewDocument;
  onHire: (applicationId: string) => Promise<boolean>;
}) {
  const [candidates, setCandidates] = useState<Candidate[] | null>(null); const [loadError, setLoadError] = useState<string | null>(null);
  const [query, setQuery] = useState(''); const [status, setStatus] = useState('all');
  const [rejecting, setRejecting] = useState<Candidate | null>(null);
  const [hiring, setHiring] = useState<Candidate | null>(null);
  const [viewing, setViewing] = useState<Candidate | null>(null);
  const [scheduling, setScheduling] = useState<Candidate | null>(null);
  const [documentsOf, setDocumentsOf] = useState<Candidate | null>(null);
  const [analysisOf, setAnalysisOf] = useState<Candidate | null>(null);
  // Tipos de documento exigidos: sem eles (ainda ou por falha) o Contratar fica desabilitado.
  const [types, setTypes] = useState<DocumentType[] | null>(null);
  useEffect(() => { portalService.getDocumentTypes().then(setTypes).catch(() => undefined); }, []);
  // Cada carregamento leva um número: só o mais recente grava, para uma resposta atrasada (outra vaga, outra notificação) não sobrescrever a atual.
  const latestLoad = useRef(0);
  const load = () => {
    const mine = ++latestLoad.current;
    return portalService.getCandidates(job.id)
      .then((items) => { if (mine === latestLoad.current) { setCandidates(items); setLoadError(null); } })
      .catch((error) => { if (mine === latestLoad.current) setLoadError(messageOf(error, 'Não foi possível carregar os inscritos.')); });
  };
  useEffect(() => { setCandidates(null); setLoadError(null); void load(); }, [job.id]);
  useEffect(() => { if (reloadKey > 0) void load(); }, [reloadKey]); // eslint-disable-line react-hooks/exhaustive-deps
  const retry = () => { setLoadError(null); void load(); };
  const update = async (id: string, next: ApplicationStatus) => { if (await onUpdate(id, next)) void load(); };
  const filtered = (candidates ?? []).filter((candidate) => candidate.name.toLowerCase().includes(query.trim().toLowerCase()) && (status === 'all' || candidate.status === status));
  return <section className="hr-page">
    <button type="button" className="back-link" onClick={onBack}><ChevronLeft />Vagas</button>
    <div className="page-head">
      <div><h1 className="display">Candidatos</h1><p>{candidates === null ? (loadError ? 'Inscritos indisponíveis.' : 'Carregando inscritos…') : `${candidates.length} ${candidates.length === 1 ? 'inscrito' : 'inscritos'} · publicada em ${formatDate(job.publishedAt)}`}</p></div>
      <Select value={job.id} onValueChange={onSelectJob}><SelectTrigger className="job-picker" aria-label="Vaga"><SelectValue /></SelectTrigger><SelectContent>{jobs.filter((item) => item.status !== 'encerrada' || item.id === job.id).map((item) => <SelectItem key={item.id} value={item.id}>{item.title}</SelectItem>)}</SelectContent></Select>
    </div>
    <div className="toolbar">
      <label className="search"><span className="sr-only">Buscar candidato</span><Search /><Input type="search" placeholder="Buscar candidato" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
      <Select value={status} onValueChange={setStatus}><SelectTrigger className="min-w-48" aria-label="Etapa"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">Todas as etapas</SelectItem>{Object.entries(appStatus).filter(([value]) => value !== 'hired').map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select>
    </div>
    <div className="lineup">
      {filtered.length > 0 && <table className="sheet-table plain">
        <thead><tr><th>Candidato</th><th>Inscrição</th><th>Aderência</th><th>Etapa</th><th><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{filtered.map((candidate) => <tr key={candidate.applicationId}>
          <td className="lead-cell"><div className="person"><span className="initials">{initials(candidate.name)}</span><span><strong>{candidate.name}</strong><small>{candidate.email}</small></span></div></td>
          <td data-label="Inscrição" className="tabular">{formatDate(candidate.submittedAt)}</td>
          <td data-label="Aderência">{adherenceLabel(candidate.analise)}</td>
          <td><Chip tone={appTone[candidate.status]}>{appStatus[candidate.status]}</Chip>{candidate.status === 'interview' && candidate.interviewAt && <small>Entrevista em {formatDateTime(candidate.interviewAt)}</small>}</td>
          <td className="actions-cell"><CandidateMenu candidate={candidate} hireBlock={hireBlock(candidate, types, documents)} onUpdate={update} onReject={setRejecting} onHire={setHiring} onResume={setViewing} onSchedule={setScheduling} onDocuments={setDocumentsOf} onAnalysis={setAnalysisOf} /></td>
        </tr>)}</tbody>
      </table>}
      {candidates !== null && filtered.length === 0 && (candidates.length === 0
        ? <Empty title="Nenhum inscrito ainda" text="Quando alguém se candidatar a esta vaga, aparece aqui." />
        : <Empty title="Nenhum candidato neste filtro" text="Troque a etapa ou a busca." />)}
      {candidates === null && loadError && <Empty title="Não foi possível carregar os inscritos" text={loadError} action={<Button variant="outline" onClick={retry}>Tentar novamente</Button>} />}
      {candidates === null && !loadError && <p className="empty muted">Carregando inscritos…</p>}
    </div>
    {viewing && <ResumeDialog candidate={viewing} onClose={() => setViewing(null)} />}
    {documentsOf && <CandidateDocumentsDialog candidate={documentsOf} reloadKey={reloadKey} onDownload={onDownload} onReview={onReview} onClose={() => setDocumentsOf(null)} />}
    {analysisOf && <AnalysisDialog candidate={analysisOf} onClose={() => setAnalysisOf(null)} />}
    {scheduling && <InterviewDialog candidate={scheduling} onClose={() => setScheduling(null)} onSave={async (dateTime) => { const problem = await onSchedule(scheduling.applicationId, dateTime); if (!problem) void load(); return problem; }} />}
    {hiring && <ConfirmDialog title="Contratar candidato?" text={`${hiring.name} passa a constar em Funcionários e sai da lista de candidatos desta vaga.`} confirm="Contratar" busyLabel="Contratando…" destructive={false} onCancel={() => setHiring(null)} onConfirm={async () => { const hired = hiring; if (await onHire(hired.applicationId)) setCandidates((items) => items && items.filter((item) => item.applicationId !== hired.applicationId)); setHiring(null); }} />}
    {rejecting && <ConfirmDialog title="Não selecionar candidato?" text={`${rejecting.name} verá a candidatura como “Não selecionado” nesta vaga.`} confirm="Não selecionar" busyLabel="Salvando…" onCancel={() => setRejecting(null)} onConfirm={async () => { await update(rejecting.applicationId, 'rejected'); setRejecting(null); }} />}
  </section>;
}

function CandidateMenu({ candidate, hireBlock, onUpdate, onReject, onHire, onResume, onSchedule, onDocuments, onAnalysis }: { candidate: Candidate; hireBlock: string | null; onUpdate: (id: string, status: ApplicationStatus) => void; onReject: (candidate: Candidate) => void; onHire: (candidate: Candidate) => void; onResume: (candidate: Candidate) => void; onSchedule: (candidate: Candidate) => void; onDocuments: (candidate: Candidate) => void; onAnalysis: (candidate: Candidate) => void }) {
  return <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Ações para ${candidate.name}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="min-w-56">
    <DropdownMenuItem onClick={() => onResume(candidate)}><FileText />Ver currículo</DropdownMenuItem>
    <DropdownMenuItem onClick={() => onAnalysis(candidate)}><Sparkles />Ver análise</DropdownMenuItem>
    <DropdownMenuItem onClick={() => onDocuments(candidate)}><Inbox />Ver documentos</DropdownMenuItem>
    <DropdownMenuSeparator />
    <DropdownMenuItem disabled={candidate.status === 'reviewing'} onClick={() => onUpdate(candidate.applicationId, 'reviewing')}><ClipboardList />Mover para análise</DropdownMenuItem>
    <DropdownMenuItem onClick={() => onSchedule(candidate)}><CalendarDays />{candidate.status === 'interview' ? 'Reagendar entrevista' : 'Chamar para entrevista'}</DropdownMenuItem>
    <DropdownMenuItem disabled={candidate.status === 'approved'} onClick={() => onUpdate(candidate.applicationId, 'approved')}><Check />Aprovar candidato</DropdownMenuItem>
    <DropdownMenuItem className="has-hint" disabled={hireBlock !== null} onClick={() => onHire(candidate)}><UserCheck /><span><span className="hint-title">Contratar candidato</span>{hireBlock && <small>{hireBlock}</small>}</span></DropdownMenuItem>
    <DropdownMenuSeparator />
    <DropdownMenuItem disabled={candidate.status === 'rejected'} variant="destructive" onClick={() => onReject(candidate)}><Ban />Não selecionar candidato</DropdownMenuItem>
  </DropdownMenuContent></DropdownMenu>;
}

/** Aderência à vaga: alta (80+) verde, média (50 a 79) amarelo, baixa vermelho. O número sempre aparece; sem ele, o estado da análise. */
function adherenceLabel(analysis: AnaliseCandidatura | null) {
  if (analysis?.status === 'CONCLUIDA' && analysis.aderencia !== null) return <Chip tone={analysis.aderencia >= 80 ? 'green' : analysis.aderencia >= 50 ? 'yellow' : 'red'}>{analysis.aderencia}%</Chip>;
  return <small>{analysis?.status === 'PENDENTE' ? 'Analisando' : 'Análise indisponível'}</small>;
}

/** Triagem do currículo por IA: só uma recomendação, quem decide é o RH. Sem tempo real: PENDENTE some ao recarregar a lista. */
function AnalysisDialog({ candidate, onClose }: { candidate: Candidate; onClose: () => void }) {
  const analysis = candidate.analise;
  const sections = [{ title: 'Pontos positivos', tone: 'positive', items: analysis?.pontosPositivos ?? [] }, { title: 'Pontos negativos', tone: 'negative', items: analysis?.pontosNegativos ?? [] }];
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>Análise de {candidate.name}</DialogTitle><DialogDescription>A análise é só uma recomendação. Quem decide é o RH.</DialogDescription></DialogHeader>
    {analysis?.status === 'CONCLUIDA' && analysis.aderencia !== null && <p className="analysis-score">Aderência à vaga: <strong>{analysis.aderencia}%</strong></p>}
    {analysis?.status === 'CONCLUIDA'
      ? sections.map((section) => <div key={section.title} className={`analysis-section ${section.tone}`}><h3>{section.title}</h3>{section.items.length > 0 ? <ul>{section.items.map((item, index) => <li key={index}>{item}</li>)}</ul> : <p className="muted">Nenhum ponto registrado.</p>}</div>)
      : <p className="muted">{analysis?.status === 'PENDENTE' ? 'A análise ainda está sendo preparada. Ela aparece ao recarregar a lista de inscritos.' : 'Análise indisponível.'}</p>}
    <DialogFooter><Button variant="outline" onClick={onClose}>Fechar</Button></DialogFooter>
  </DialogContent></Dialog>;
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
function CandidateDocumentsDialog({ candidate, reloadKey, onDownload, onReview, onClose }: { candidate: Candidate; reloadKey: number; onDownload: (doc: CandidateDocument) => Promise<void>; onReview: ReviewDocument; onClose: () => void }) {
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{candidate.name}</DialogTitle><DialogDescription>{candidate.email} · documentos de contratação</DialogDescription></DialogHeader>
    <DocumentBoardView applicationId={candidate.applicationId} reloadKey={reloadKey} onDownload={onDownload} onReview={onReview} />
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
