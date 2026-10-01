import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Formata uma data ISO ("yyyy-MM-dd" ou "yyyy-MM-ddTHH:mm:ss") para "dd/MM/yyyy". */
export function formatDate(iso?: string): string {
  if (!iso) return '';
  const [date] = iso.split('T');
  const [y, m, d] = date.split('-');
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo', dateStyle: 'short', timeStyle: 'short' });

/** Mostra um instante ISO (ex.: "2026-10-20T18:00:00Z") no horário de Brasília. */
export function formatDateTime(iso?: string): string {
  if (!iso) return '';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : dateTimeFormat.format(date);
}
