import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { Bell, BriefcaseBusiness, CalendarDays, FileText, LogOut, Settings, UserCheck, UsersRound } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import type { NotificationItem } from '@/types/domain';
import type { LoginResponse } from '@/types/auth';
import type { HrView } from '@/lib/routes';
import { Glow, Notifications, Brand } from '@/components/common';
import { ThemeMenu } from '@/components/theme';
import { initials, firstName } from '@/lib/helpers';

const hrNav: { view: HrView; label: string; icon: typeof Bell }[] = [
  { view: 'jobs', label: 'Vagas', icon: BriefcaseBusiness },
  { view: 'candidates', label: 'Candidatos', icon: UsersRound },
  { view: 'agenda', label: 'Agenda', icon: CalendarDays },
  { view: 'documents', label: 'Documentos', icon: FileText },
  { view: 'employees', label: 'Funcionários', icon: UserCheck },
];

const settingsNav = { view: 'settings' as const, label: 'Configurações', icon: Settings };

export function HrLayout({ active, children, onNavigate, onExit, userName, role, notifications, onReadNotifications, onReadNotification }: { active: HrView; children: ReactNode; onNavigate: (view: HrView) => void; onExit: () => void; userName: string; role: LoginResponse['perfil']; notifications: NotificationItem[]; onReadNotifications: () => void; onReadNotification: (id: string) => void }) {
  // Esconder a aba é só conveniência: quem barra o acesso às Configurações é o backend (403).
  const roleLabel = role === 'administrador' ? 'Administrador' : 'Recursos Humanos';
  const links = (role === 'administrador' ? [...hrNav, settingsNav] : hrNav).map(({ view, label, icon: Icon }) => <button key={view} type="button" aria-current={active === view ? 'page' : undefined} onClick={() => onNavigate(view)}><Icon />{label}</button>);
  // Em telas estreitas o menu (no topo ou na barra de baixo) rola quando as abas não cabem: a aba atual não pode ficar escondida.
  useEffect(() => { document.querySelectorAll('.topnav [aria-current="page"], .tabbar [aria-current="page"]').forEach((tab) => tab.scrollIntoView({ block: 'nearest', inline: 'nearest' })); }, [active]);
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
