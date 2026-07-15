export const resolveFileUrl = (url?: string | null): string => {
  if (!url) return ''
  const value = String(url).trim()
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) return value
  if (value.startsWith('/api/files/download')) return value
  if (value.startsWith('/uploads/')) return `/api/files/download${value}`
  if (value.startsWith('uploads/')) return `/api/files/download/${value}`
  if (value.startsWith('/')) return value
  return `/api/files/download/${value}`
}
