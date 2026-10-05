import { useMemo, useState } from 'react';
import { Check, ChevronLeft, ChevronRight, CircleAlert, FileText, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { formatDate } from '@/lib/utils';
import type { Application, CandidateProfile, Job } from '@/types/domain';
import { appStatus, appTone } from '@/lib/status';
import { Stages } from '@/pages/candidate/applications';
import { Chip, JobMeta, Empty } from '@/components/common';
import { requirementGroups } from '@/lib/helpers';

export function JobsPage({ jobs, applications, resumeEmpty, onOpen, onEditResume }: { jobs: Job[]; applications: Application[]; resumeEmpty: boolean; onOpen: (id: string) => void; onEditResume: () => void }) {
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
        ? <div className="lineup"><Empty title="Nenhuma vaga aberta no momento" text="Novas vagas aparecem aqui assim que o RH publicar. Enquanto isso, deixe seu currículo em dia para se candidatar com rapidez." /></div>
        : <div className="lineup"><Empty title="Nenhuma vaga com esses filtros" text="Tente outro cargo ou outra modalidade." action={<Button variant="outline" onClick={clear}>Limpar filtros</Button>} /></div>}
  </main>;
}

export function JobDetail({ job, profile, userName, applied, onBack, onApply, onEditResume }: { job: Job; profile: CandidateProfile; userName: string; applied: boolean; onBack: () => void; onApply: () => Promise<void>; onEditResume: () => void }) {
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
          {requirementGroups(job).length > 0 && <><h2>Requisitos</h2>{requirementGroups(job).map(([title, items]) => <section key={title}><h3>{title}</h3><ul>{items.map((item) => <li key={item}>{item}</li>)}</ul></section>)}</>}
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

export function SuccessPage({ job, onApplications, onJobs }: { job?: Job; onApplications: () => void; onJobs: () => void }) {
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
