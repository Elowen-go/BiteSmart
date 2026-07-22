/** 后端 JSON 数组字符串列（tags / prefs / avoid / focusParts / fitScenes / cautions）→ string[]，脏数据兜底为空数组 */
export const parseJsonList = (raw?: string | string[] | null): string[] => {
  if (Array.isArray(raw)) return raw.map((x) => String(x))
  if (!raw) return []
  try {
    const v: unknown = JSON.parse(raw)
    return Array.isArray(v) ? v.map((x) => String(x)) : []
  } catch (_) {
    return []
  }
}

/** LocalDateTime 序列化串（ISO 'yyyy-MM-ddTHH:mm:ss'）→ 'yyyy-MM-dd HH:mm' 展示 */
export const fmtDateTime = (raw?: string | null): string =>
  raw ? String(raw).replace('T', ' ').slice(0, 16) : ''
