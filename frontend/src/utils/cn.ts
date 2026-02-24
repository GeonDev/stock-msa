import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(value: any) {
  if (value === null || value === undefined || typeof value !== 'number' || isNaN(value)) return '₩0';
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
  }).format(value);
}

export function formatPercent(value: any) {
  if (value === undefined || value === null || typeof value !== 'number' || isNaN(value)) return '0.00%';
  return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
}
