import { type ReactNode } from 'react'

interface StatProps {
  label: string
  value: ReactNode
  hint?: string
  className?: string
}

export function Stat({ label, value, hint, className = '' }: StatProps) {
  return (
    <div className={`flex flex-col gap-0.5 ${className}`}>
      <div className="text-[10px] uppercase tracking-wider text-slate-400">{label}</div>
      <div className="text-sm text-slate-100 font-medium leading-tight">{value}</div>
      {hint && <div className="text-[11px] text-slate-500">{hint}</div>}
    </div>
  )
}
