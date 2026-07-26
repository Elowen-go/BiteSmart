import { getNotices, type Notice } from '../../api/notice'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface NoticeView { id: number | string; title: string; body: string; date: string }
const NOTICE_SEEN_KEY = 'bitesmart.notice.seenAt'

const parseNoticeTime = (value?: string): number => {
  if (!value) return 0
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const time = Date.parse(normalized)
  return Number.isFinite(time) ? time : 0
}

Page({
  data: { notices: [] as NoticeView[], openId: '' as number | string, loading: true, menuTop: 0, menuH: 32 },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    getNotices()
      .then((notices) => {
        const latestAt = notices.reduce((max, item) => {
          const at = parseNoticeTime(String(item.publishTime || item.createTime || ''))
          return at > max ? at : max
        }, 0)
        if (latestAt) {
          wx.setStorageSync(NOTICE_SEEN_KEY, latestAt)
        }
        this.setData({
          notices: notices.map((n: Notice, i: number) => ({
            id: n.id != null ? String(n.id) : String(i + 1),
            title: n.title || '系统公告',
            body: n.content || '',
            date: String(n.publishTime || n.createTime || '').slice(0, 10)
          }))
        })
      })
      .catch(() => {})
      .finally(() => this.setData({ loading: false }))
  },
  back() { wx.navigateBack() },
  toggle(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id || '')
    if (!id) return
    this.setData({ openId: this.data.openId === id ? '' : id })
  }
})
