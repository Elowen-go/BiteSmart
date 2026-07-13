export const resolveFileUrl = (url?: string | null): string => {
  if (!url) return ''
  return url.startsWith('/uploads/') ? `/api/files/download${url}` : url
}
