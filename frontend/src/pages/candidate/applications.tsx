import { useState } from 'react';
import { Check, FileText, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatDate, formatDateTime } from '@/lib/utils';
import type { Application, ApplicationStatus } from '@/types/domain';
import { appStatus, appTone, jobStatus } from '@/lib/status';
import { Chip, Empty } from '@/components/common';

const stageNames = ['Inscrito', 'Em análise', 'Entrevista', 'Aprovado'];

const nextStep: Record<ApplicationStatus, string> = {
  applied: 'Próximo passo: o RH vai revisar seu currículo.',
  reviewing: 'Próximo passo: o RH está avaliando seu currículo.',
  interview: 'Próximo passo: aguarde o contato do RH para a entrevista.',
  approved: 'Você foi aprovado. Envie os documentos de contratação para seguir com a admissão.',
  rejected: 'Agradecemos o seu interesse. Desta vez você não seguiu nesta vaga, mas seu currículo continua salvo e você pode se candidatar às próximas oportunidades.',
  hired: 'Você foi contratado. Bem-vindo à equipe!',
  cancelled: 'Esta candidatura foi cancelada, mas seu currículo continua salvo. Quando quiser, veja as outras vagas abertas.',
};

export function ApplicationsPage({ applications, onDocuments, onJobs, onConfirmPresence }: { applications: Application[]; onDocuments: () => void; onJobs: () => void; onConfirmPresence: (applicationId: string) => Promise<void> }) {
  const [confirming, setConfirming] = useState<string | null>(null);
  const confirmPresence = async (applicationId: string) => { setConfirming(applicationId); await onConfirmPresence(applicationId); setConfirming(null); };
  return <main className="page narrow">
    <div className="page-head"><div><h1 className="display">Minhas candidaturas</h1><p>{applications.length === 1 ? '1 candidatura em andamento.' : `${applications.length} candidaturas em andamento.`}</p></div><Button variant="outline" onClick={onDocuments}><FileText />Meus documentos</Button></div>
    {applications.length === 0
      ? <div className="lineup"><Empty title="Você ainda não tem candidaturas" text="Quando encontrar uma vaga que combine com você, é só se candidatar com o seu currículo." action={<Button onClick={onJobs}>Ver vagas abertas</Button>} /></div>
      : <div className="stack">{applications.map((app) => {
          const scheduled = app.status === 'interview' && app.interviewAt;
          return <section className="panel application" data-status={app.status} key={app.id}>
            <div className="panel-head"><div><h2>{app.jobTitle}</h2><p>Enviada em {formatDate(app.submittedAt)}{app.jobStatus !== 'aberta' && ` · Vaga ${jobStatus[app.jobStatus].toLowerCase()}`}</p></div><Chip tone={appTone[app.status]}>{appStatus[app.status]}</Chip></div>
            <Stages status={app.status} />
            <div className="panel-foot">
              <p className="next-step">{scheduled ? `Entrevista marcada para ${formatDateTime(app.interviewAt)} (horário de Brasília).` : nextStep[app.status]}</p>
              {scheduled && app.presence === 'pendente' && <Button disabled={confirming === app.id} onClick={() => void confirmPresence(app.id)}><Check />{confirming === app.id ? 'Confirmando…' : 'Confirmar presença'}</Button>}
              {scheduled && app.presence === 'confirmado' && <Chip tone="green">Presença confirmada{app.presenceConfirmedAt ? ` em ${formatDateTime(app.presenceConfirmedAt)}` : ''}</Chip>}
              {(app.status === 'approved' || app.status === 'hired') && <Button onClick={onDocuments}><Upload />Enviar documentos</Button>}
              {(app.status === 'rejected' || app.status === 'cancelled') && <Button variant="outline" onClick={onJobs}>Ver outras vagas</Button>}
            </div>
          </section>;
        })}</div>}
  </main>;
}

export function Stages({ status }: { status: ApplicationStatus }) {
  const current = stepFor(status); const out = status === 'rejected' || status === 'cancelled';
  return <ol className="stages" aria-label="Etapas da candidatura">{stageNames.map((name, index) => {
    const step = index + 1;
    const state = out && step === current ? 'out' : step < current ? 'done' : step === current ? 'current' : '';
    return <li key={name} className={state} aria-current={step === current ? 'step' : undefined}><span className="node">{state === 'done' || (state === 'current' && (status === 'approved' || status === 'hired')) ? <Check /> : state === 'out' ? null : step}</span><span>{state === 'out' ? appStatus[status] : name}</span></li>;
  })}</ol>;
}

function stepFor(status: ApplicationStatus) { return ({ applied: 1, reviewing: 2, interview: 3, approved: 4, rejected: 2, hired: 4, cancelled: 1 })[status]; }
