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
