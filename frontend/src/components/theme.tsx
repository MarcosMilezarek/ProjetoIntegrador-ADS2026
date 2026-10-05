import { useSyncExternalStore } from 'react';
import { Monitor, Moon, Sun } from 'lucide-react';
import { DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem } from '@/components/ui/dropdown-menu';

type ThemePref = 'system' | 'light' | 'dark';

const themeKey = 'teamup-theme';

const themeListeners = new Set<() => void>();

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');

let themePref: ThemePref = (() => { try { const saved = localStorage.getItem(themeKey); return saved === 'light' || saved === 'dark' ? saved : 'system'; } catch { return 'system'; } })();

function applyTheme() {
  const dark = themePref === 'dark' || (themePref === 'system' && darkQuery.matches);
  document.documentElement.classList.toggle('dark', dark);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#06100b' : '#ffffff');
}

function setThemePref(next: ThemePref) {
  themePref = next;
  try { if (next === 'system') localStorage.removeItem(themeKey); else localStorage.setItem(themeKey, next); } catch { /* preferência só não fica salva */ }
  applyTheme();
  themeListeners.forEach((listener) => listener());
}

darkQuery.addEventListener('change', () => { if (themePref === 'system') applyTheme(); });

applyTheme();

function useThemePref() { return useSyncExternalStore((listener) => { themeListeners.add(listener); return () => { themeListeners.delete(listener); }; }, () => themePref); }

export function ThemeSwitch() {
  const pref = useThemePref();
  const options: [ThemePref, string, typeof Sun][] = [['system', 'Seguir o sistema', Monitor], ['light', 'Tema claro', Sun], ['dark', 'Tema escuro', Moon]];
  return <div className="segmented theme-switch" role="radiogroup" aria-label="Tema">{options.map(([value, label, Icon]) => <button key={value} type="button" role="radio" aria-checked={pref === value} aria-label={label} title={label} onClick={() => setThemePref(value)}><Icon /></button>)}</div>;
}

export function ThemeMenu() {
  const pref = useThemePref();
  return <>
    <DropdownMenuLabel className="font-normal text-muted-foreground">Tema</DropdownMenuLabel>
    <DropdownMenuRadioGroup value={pref} onValueChange={(value) => setThemePref(value as ThemePref)}>
      <DropdownMenuRadioItem value="system"><Monitor />Seguir o sistema</DropdownMenuRadioItem>
      <DropdownMenuRadioItem value="light"><Sun />Claro</DropdownMenuRadioItem>
      <DropdownMenuRadioItem value="dark"><Moon />Escuro</DropdownMenuRadioItem>
    </DropdownMenuRadioGroup>
  </>;
}
