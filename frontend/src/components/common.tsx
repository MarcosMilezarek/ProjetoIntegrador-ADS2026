import { useEffect, useState } from 'react';
import type { ComponentProps, ReactNode } from 'react';
import { Ban, Bell, BriefcaseBusiness, CalendarDays, ChevronRight, CircleAlert, Eye, EyeOff, Inbox, MapPin, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { formatDate } from '@/lib/utils';
import type { Job, NotificationItem } from '@/types/domain';
import type { Tone } from '@/lib/status';

export type Notice = { tone: 'ok' | 'error'; text: string } | null;

export function Toast({ notice, onClose }: { notice: Notice; onClose: () => void }) {
  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(onClose, notice.tone === 'error' ? 8000 : 5000);
    return () => window.clearTimeout(timer);
  }, [notice]);
  if (!notice) return null;
  return <div className="toast" data-tone={notice.tone} role={notice.tone === 'error' ? 'alert' : 'status'}><p>{notice.text}</p><button type="button" onClick={onClose} aria-label="Fechar aviso"><X /></button></div>;
}

export function LoadingScreen({ error, onRetry, onExit }: { error: string | null; onRetry: () => void; onExit: () => void }) {
  return <main className="loading">
    <Brand />
    {error
      ? <><p role="alert">{error}</p><div className="actions"><Button onClick={onRetry}>Tentar novamente</Button><Button variant="outline" onClick={onExit}>Voltar ao login</Button></div></>
      : <><svg className="ring" viewBox="0 0 72 72" aria-hidden="true"><circle cx="36" cy="36" r="24" pathLength={1} /></svg><p>Preparando o portal…</p></>}
  </main>;
}

export function Glow() { return <div className="glow" aria-hidden="true"><span /><span /><span /><span /></div>; }

export function Notifications({ items, onRead, onReadOne }: { items: NotificationItem[]; onRead: () => void; onReadOne: (id: string) => void }) {
  const unread = items.filter((item) => !item.read).length;
  return <DropdownMenu onOpenChange={(open) => { if (!open && unread > 0) onRead(); }}>
    <DropdownMenuTrigger asChild><Button variant="ghost" size="icon" className="bell" aria-label={unread > 0 ? `Notificações, ${unread} não lidas` : 'Notificações'}><Bell />{unread > 0 && <span className="bell-count">{unread}</span>}</Button></DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="notification-menu">
      <div className="menu-title">Notificações</div>
      <DropdownMenuSeparator />
      {items.length === 0 && <p className="px-2.5 py-3 text-muted-foreground">Nenhuma notificação por enquanto.</p>}
      {items.map((item) => <DropdownMenuItem key={item.id} className={`notification-item ${item.read ? '' : 'unread'}`} data-type={item.type} onSelect={(event) => { event.preventDefault(); if (!item.read) onReadOne(item.id); }}><strong>{item.title}</strong><small>{item.description}</small></DropdownMenuItem>)}
    </DropdownMenuContent>
  </DropdownMenu>;
}

export function ConfirmDialog({ title, text, confirm, busyLabel, destructive = true, onCancel, onConfirm }: { title: string; text: string; confirm: string; busyLabel: string; destructive?: boolean; onCancel: () => void; onConfirm: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  return <Dialog open onOpenChange={(open) => !open && onCancel()}><DialogContent className="job-dialog confirm-dialog">
    <DialogHeader><DialogTitle>{title}</DialogTitle><DialogDescription>{text}</DialogDescription></DialogHeader>
    <DialogFooter><Button variant="outline" onClick={onCancel}>Cancelar</Button><Button variant={destructive ? 'destructive' : 'default'} disabled={busy} onClick={async () => { setBusy(true); await onConfirm(); setBusy(false); }}>{destructive && <Ban />}{busy ? busyLabel : confirm}</Button></DialogFooter>
  </DialogContent></Dialog>;
}

export function Brand({ tag }: { tag?: string }) {
  return <span className="brand"><span className="brand-orb" aria-hidden="true" /><span className="brand-word">TeamUp</span>{tag && <span className="brand-tag">{tag}</span>}</span>;
}

export function CtaArrow() { return <span className="cta-arrow" aria-hidden="true"><ChevronRight /></span>; }

export function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: ReactNode }) { return <label className="field"><span>{label}{hint && <span className="hint"> · {hint}</span>}</span>{children}{error && <small className="field-error" role="alert">{error}</small>}</label>; }

export function PasswordInput(props: ComponentProps<'input'>) {
  const [visible, setVisible] = useState(false);
  return <div className="password"><Input {...props} type={visible ? 'text' : 'password'} /><button type="button" onClick={() => setVisible((current) => !current)} aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={visible}>{visible ? <EyeOff /> : <Eye />}</button></div>;
}

export function FormError({ title, text }: { title: string; text: string }) { return <div className="form-error" role="alert"><CircleAlert /><div><strong>{title}</strong>{text}</div></div>; }

export function Chip({ tone, children }: { tone?: Tone; children: ReactNode }) { return <span className="chip" data-tone={tone}>{children}</span>; }

export function JobMeta({ job, withDate = false }: { job: Job; withDate?: boolean }) { return <span className="meta">{job.city !== job.workModel && <span><MapPin />{job.city}</span>}<span><BriefcaseBusiness />{job.workModel} · {job.contract}</span>{withDate && <span><CalendarDays />Publicada em {formatDate(job.publishedAt)}</span>}</span>; }

export function Empty({ title, text, action }: { title: string; text: string; action?: ReactNode }) {
  return <div className="empty"><span className="empty-icon"><Inbox /></span><h2>{title}</h2><p>{text}</p>{action}</div>;
}
