export function formatNumber(n: number | null | undefined): string {
  if (n == null) return '—'
  if (n >= 1e9) return `${(n / 1e9).toFixed(2)} B`
  if (n >= 1e6) return `${(n / 1e6).toFixed(2)} M`
  if (n >= 1e3) return `${(n / 1e3).toFixed(1)} K`
  return n.toLocaleString()
}

export function formatArea(km2: number | null | undefined): string {
  if (km2 == null) return '—'
  return `${km2.toLocaleString(undefined, { maximumFractionDigits: 0 })} km²`
}

export function formatList(items: string[], max = 4): string {
  if (!items.length) return '—'
  if (items.length <= max) return items.join(', ')
  return `${items.slice(0, max).join(', ')} +${items.length - max}`
}
