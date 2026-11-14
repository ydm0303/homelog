// 图片等静态文件统一走 /api/files，避免各页面各自拼接导致 dev/生产路径不一致
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')

export function resolveFileUrl(path?: string | null): string {
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  // 后端已经返回完整相对 API 路径时不再重复加前缀
  if (path.startsWith('/api/')) return path
  return `${API_BASE}/files${path.startsWith('/') ? path : `/${path}`}`
}
