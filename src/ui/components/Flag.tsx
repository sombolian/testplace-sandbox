interface FlagProps {
  iso2: string
  className?: string
  squared?: boolean
}

export function Flag({ iso2, className = '', squared = false }: FlagProps) {
  const code = iso2.toLowerCase()
  return (
    <span
      className={`fi fi-${code} ${squared ? 'fis aspect-square' : ''} ${className}`}
      aria-hidden
    />
  )
}
