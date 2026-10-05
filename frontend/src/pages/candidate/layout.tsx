import type { ReactNode } from 'react';
import { Bell, BriefcaseBusiness, ClipboardList, FileText, LogOut } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import type { NotificationItem } from '@/types/domain';
import type { LoginResponse } from '@/types/auth';
import type { CandidateView } from '@/lib/routes';
import { Glow, Notifications, Brand } from '@/components/common';
import { ThemeMenu } from '@/components/theme';
import { initials, firstName } from '@/lib/helpers';

const candidateNav: { view: CandidateView; label: string; icon: typeof Bell; match: CandidateView[] }[] = [
  { view: 'jobs', label: 'Vagas', icon: BriefcaseBusiness, match: ['jobs', 'detail', 'success'] },
  { view: 'resume', label: 'Currículo', icon: FileText, match: ['resume'] },
  { view: 'applications', label: 'Candidaturas', icon: ClipboardList, match: ['applications', 'documents'] },
];

export function CandidateLayout({ active, children, onNavigate, onExit, user, notifications, onReadNotifications, onReadNotification }: { active: CandidateView; children: ReactNode; onNavigate: (view: CandidateView) => void; onExit: () => void; user: LoginResponse; notifications: NotificationItem[]; onReadNotifications: () => void; onReadNotification: (id: string) => void }) {
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
