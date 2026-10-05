import type { Job } from '@/types/domain';

export const maxUploadBytes = 5 * 1024 * 1024;

export const localToday = () => new Date().toLocaleDateString('sv-SE');

export const formatBytes = (bytes: number) => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;

/** Agora, no horário de Brasília, no formato de um input datetime-local (para o `min`). */
export function nowInBrasilia() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date()).replace(' ', 'T');
}

/** Entrega um arquivo baixado como blob (a API exige o token, então não dá para usar um link direto). */
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = filename;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function initials(name: string) { return name.split(' ').filter(Boolean).slice(0, 2).map((item) => item[0]).join('').toUpperCase(); }

export function firstName(name: string) { return name.split(' ')[0]; }

export function lineList(value: string) { return value.split('\n').map((item) => item.trim()).filter(Boolean); }

/** Grupos de requisitos que têm itens, na ordem de peso. */
export function requirementGroups({ requirements }: Job): [string, string[]][] {
  const groups: [string, string[]][] = [['Obrigatórios', requirements.required], ['Desejáveis', requirements.desirable], ['Diferenciais', requirements.differential]];
  return groups.filter(([, items]) => items.length > 0);
}

export function messageOf(error: unknown, fallback: string) { return error instanceof Error && error.message ? error.message : fallback; }
