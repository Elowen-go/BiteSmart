/** 后端 JSON 数组字符串列（tags / fitScenes / cautions / dietPreference 等）→ string[]，脏数据兜底为空数组 */
export const parseJsonList = (raw?: string | string[] | null): string[] => {
  if (Array.isArray(raw)) return raw.map(x => String(x))
  if (!raw) return []
  try {
    const v: unknown = JSON.parse(raw)
    return Array.isArray(v) ? v.map(x => String(x)) : []
  } catch {
    return []
  }
}
