import { useEffect, useState } from 'react';
import { Ban, Eye, MoreHorizontal } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { portalService } from '@/services/portal-service';
import type { CandidateDocument, Employee, EmployeeProfile } from '@/types/domain';
import { ConfirmDialog, FormError, Chip, Empty } from '@/components/common';
import type { Notice } from '@/components/common';
import { userStatus, userTone } from '@/lib/status';
import { DocumentRow } from '@/components/documents';
import { messageOf } from '@/lib/helpers';

/** Candidatos contratados. Inativar não apaga: o registro continua na lista, só troca o status. */
export function HrEmployeesPage({ onNotice, onDownload }: { onNotice: (notice: Notice) => void; onDownload: (doc: CandidateDocument) => Promise<void> }) {
  const [employees, setEmployees] = useState<Employee[] | null>(null); const [loadError, setLoadError] = useState<string | null>(null);
  const [viewing, setViewing] = useState<Employee | null>(null);
  const [deactivating, setDeactivating] = useState<Employee | null>(null);
  const load = () => portalService.getEmployees().then((items) => { setEmployees(items); setLoadError(null); }).catch((error) => setLoadError(messageOf(error, 'Não foi possível carregar os funcionários.')));
  useEffect(() => { void load(); }, []);
  /** Troca só o funcionário inativado na lista, sem recarregá-la. */
  const deactivate = async (employee: Employee) => {
    try {
      const updated = await portalService.deactivateEmployee(employee.id);
      setEmployees((items) => items && items.map((item) => item.id === updated.id ? updated : item));
      onNotice({ tone: 'ok', text: `O cadastro de ${employee.candidateName} foi inativado.` });
    } catch (error) { onNotice({ tone: 'error', text: messageOf(error, 'Não foi possível inativar o funcionário. Tente novamente.') }); }
    setDeactivating(null);
  };
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Funcionários</h1><p>Candidatos contratados, com os dados da contratação e os documentos.</p></div></div>
    <div className="lineup">
      {employees && employees.length > 0 && <table className="sheet-table">
        <thead><tr><th>Funcionário</th><th>Vaga</th><th>Contratação</th><th>Status</th><th><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{employees.map((employee) => <tr key={employee.id}>
          <td><button type="button" className="row-link" onClick={() => setViewing(employee)}><strong>{employee.candidateName}</strong><small>{employee.candidateEmail}</small><small className="mobile-meta">{employee.jobTitle} · Contratação em {employee.hiredAt}</small></button></td>
          <td data-label="Vaga">{employee.jobTitle}</td>
          <td data-label="Contratação" className="tabular">{employee.hiredAt}</td>
          <td><Chip tone={userTone[employee.status]}>{userStatus[employee.status]}</Chip></td>
          <td className="actions-cell"><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Ações para ${employee.candidateName}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="min-w-48">
            <DropdownMenuItem onClick={() => setViewing(employee)}><Eye />Ver perfil</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" disabled={employee.status === 'inativo'} onClick={() => setDeactivating(employee)}><Ban />Inativar</DropdownMenuItem>
          </DropdownMenuContent></DropdownMenu></td>
        </tr>)}</tbody>
      </table>}
      {employees && employees.length === 0 && <Empty title="Nenhum funcionário ainda" text="As contratações feitas na tela de candidatos aparecem aqui." />}
      {!employees && loadError && <Empty title="Não foi possível carregar os funcionários" text={loadError} action={<Button variant="outline" onClick={() => { setLoadError(null); void load(); }}>Tentar novamente</Button>} />}
      {!employees && !loadError && <p className="empty muted">Carregando funcionários…</p>}
    </div>
    {viewing && <EmployeeDialog employee={viewing} onDownload={onDownload} onClose={() => setViewing(null)} />}
    {deactivating && <ConfirmDialog title="Inativar funcionário?" text={`O cadastro de ${deactivating.candidateName} passa a constar como inativo. Nada é apagado: o registro e os documentos continuam na lista.`} confirm="Inativar" busyLabel="Inativando…" onCancel={() => setDeactivating(null)} onConfirm={() => deactivate(deactivating)} />}
  </section>;
}

/** Perfil do funcionário: dados da contratação e documentos com selo de status e download. */
function EmployeeDialog({ employee, onDownload, onClose }: { employee: Employee; onDownload: (doc: CandidateDocument) => Promise<void>; onClose: () => void }) {
  const [profile, setProfile] = useState<EmployeeProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { portalService.getEmployee(employee.id).then(setProfile).catch((problem) => setError(messageOf(problem, 'Não foi possível carregar o perfil.'))); }, [employee.id]);
  const data = profile?.employee ?? employee;
  return <Dialog open onOpenChange={(open) => !open && onClose()}><DialogContent className="job-dialog">
    <DialogHeader><DialogTitle>{data.candidateName}</DialogTitle><DialogDescription>{data.candidateEmail} · dados da contratação</DialogDescription></DialogHeader>
    <div className="resume-view">
      <section><h3>Contratação</h3><dl className="resume-facts"><dt>Vaga</dt><dd>{data.jobTitle}</dd><dt>Data</dt><dd>{data.hiredAt}</dd><dt>Status</dt><dd><Chip tone={userTone[data.status]}>{userStatus[data.status]}</Chip></dd></dl></section>
      <section><h3>Documentos</h3>
        {error && <FormError title="Não foi possível abrir os documentos" text={error} />}
        {!error && !profile && <p className="empty muted">Carregando documentos…</p>}
        {profile && (profile.documents.length > 0
          ? <div className="lineup">{profile.documents.map((doc) => <DocumentRow key={doc.id} doc={doc} onDownload={onDownload} />)}</div>
          : <p className="repeat-empty">Nenhum documento enviado.</p>)}
      </section>
    </div>
    <DialogFooter><Button variant="outline" onClick={onClose}>Fechar</Button></DialogFooter>
  </DialogContent></Dialog>;
}
