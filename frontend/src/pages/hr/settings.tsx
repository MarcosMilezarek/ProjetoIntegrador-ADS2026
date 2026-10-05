import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Ban, Info, MoreHorizontal, Pencil, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { portalService } from '@/services/portal-service';
import type { StaffUser, StaffUserInput } from '@/types/domain';
import type { StatusUsuario } from '@/types/auth';
import { ConfirmDialog, Field, PasswordInput, FormError, Chip, Empty } from '@/components/common';
import type { Notice } from '@/components/common';
import { userStatus, userTone, staffRoles } from '@/lib/status';
import { initials, messageOf } from '@/lib/helpers';

export function SettingsPage({ currentUserId, onNotice }: { currentUserId: string; onNotice: (notice: Notice) => void }) {
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
