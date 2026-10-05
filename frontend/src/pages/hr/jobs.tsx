import { useState } from 'react';
import type { FormEvent } from 'react';
import { Ban, MoreHorizontal, Pencil, Plus, Search, UsersRound } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { formatDate } from '@/lib/utils';
import type { Job, NewJobInput } from '@/types/domain';
import { jobStatus, jobTone } from '@/lib/status';
import { ConfirmDialog, Field, Chip, Empty } from '@/components/common';
import { lineList } from '@/lib/helpers';

export type JobInput = NewJobInput & { id?: string; status?: Job['status'] };

export function HrJobsPage({ jobs, onCandidates, onSaved, onClosed }: { jobs: Job[]; onCandidates: (id: string) => void; onSaved: (input: JobInput) => Promise<{ ok: true } | { ok: false; campos?: Record<string, string> }>; onClosed: (id: string) => Promise<boolean> }) {
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

function JobDialog({ job, onClose, onSave }: { job?: Job; onClose: () => void; onSave: (input: JobInput) => Promise<{ ok: true } | { ok: false; campos?: Record<string, string> }> }) {
  const [form, setForm] = useState({ title: job?.title ?? '', city: job?.city ?? 'Erechim, RS', workModel: job?.workModel ?? 'Híbrido', contract: job?.contract ?? 'CLT', closesAt: job?.closesAt ?? '', description: job?.description ?? '', required: job?.requirements.required.join('\n') ?? '', desirable: job?.requirements.desirable.join('\n') ?? '', differential: job?.requirements.differential.join('\n') ?? '' });
  const [saving, setSaving] = useState(false);
  const [campos, setCampos] = useState<Record<string, string>>({});
  const update = (key: keyof typeof form, value: string) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true);
    const result = await onSave({ id: job?.id, status: job?.status, title: form.title, city: form.city, workModel: form.workModel as Job['workModel'], contract: form.contract as Job['contract'], closesAt: form.closesAt || undefined, description: form.description, requirements: { required: lineList(form.required), desirable: lineList(form.desirable), differential: lineList(form.differential) } });
    setSaving(false);
    if (result.ok) onClose(); else setCampos(result.campos ?? {});
  };
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{job ? 'Editar vaga' : 'Nova vaga'}</DialogTitle><DialogDescription>{job ? 'As alterações aparecem no portal assim que você salvar.' : 'A vaga é publicada como aberta assim que você salvar.'}</DialogDescription></DialogHeader>
    <form onSubmit={submit} className="dialog-form">
      <Field label="Título da vaga" error={campos.titulo}><Input required value={form.title} onChange={(event) => update('title', event.target.value)} /></Field>
      <div className="field-row">
        <Field label="Cidade" error={campos.local}><Input required value={form.city} onChange={(event) => update('city', event.target.value)} /></Field>
        <Field label="Inscrições até" hint="Opcional" error={campos.prazo}><Input type="date" value={form.closesAt} onChange={(event) => update('closesAt', event.target.value)} /></Field>
        <Field label="Modalidade"><Select value={form.workModel} onValueChange={(value) => update('workModel', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="Presencial">Presencial</SelectItem><SelectItem value="Híbrido">Híbrido</SelectItem><SelectItem value="Remoto">Remoto</SelectItem></SelectContent></Select></Field>
        <Field label="Contrato"><Select value={form.contract} onValueChange={(value) => update('contract', value)}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="CLT">CLT</SelectItem><SelectItem value="Estágio">Estágio</SelectItem><SelectItem value="PJ">PJ</SelectItem><SelectItem value="Temporário">Temporário</SelectItem></SelectContent></Select></Field>
      </div>
      <Field label="Descrição" error={campos.descricao}><Textarea required value={form.description} onChange={(event) => update('description', event.target.value)} /></Field>
      <div className="note"><strong>A classificação dos requisitos importa.</strong>Ela influencia a triagem por IA, e os requisitos obrigatórios pesam mais.</div>
      <Field label="Requisitos obrigatórios" hint="Um requisito por linha" error={campos.requisitosObrigatorios}><Textarea value={form.required} onChange={(event) => update('required', event.target.value)} aria-describedby="req-obrigatorios" /><small id="req-obrigatorios">Indispensável para a função, como experiência, formação e competências exigidas.</small></Field>
      <Field label="Requisitos desejáveis" hint="Um requisito por linha" error={campos.requisitosDesejaveis}><Textarea value={form.desirable} onChange={(event) => update('desirable', event.target.value)} aria-describedby="req-desejaveis" /><small id="req-desejaveis">Bom ter, dá para aprender na prática.</small></Field>
      <Field label="Requisitos diferenciais" hint="Um requisito por linha" error={campos.requisitosDiferenciais}><Textarea value={form.differential} onChange={(event) => update('differential', event.target.value)} aria-describedby="req-diferenciais" /><small id="req-diferenciais">Não é necessário, mas soma se o candidato tiver.</small></Field>
      <DialogFooter><Button type="button" variant="outline" onClick={onClose}>Cancelar</Button><Button type="submit" disabled={saving}>{saving ? 'Salvando…' : job ? 'Salvar alterações' : 'Publicar vaga'}</Button></DialogFooter>
    </form>
  </DialogContent></Dialog>;
}
