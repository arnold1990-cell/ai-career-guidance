import type { InputHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export const Input = ({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) => (
  <input
    className={cn('w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary-500', className)}
    {...props}
  />
);
