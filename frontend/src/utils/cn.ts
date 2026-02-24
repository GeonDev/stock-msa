import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(value: number) {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
  }).format(value);
}

export function formatPercent(value: number | undefined | null) {
  if (value === undefined || value === null) return '0.00%';
  return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
}
