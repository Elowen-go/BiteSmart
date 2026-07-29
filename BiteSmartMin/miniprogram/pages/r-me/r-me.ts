import { getSafeArea } from '../../utils/safe-area'
import { clearAuth, getUserInfo, setUserInfo } from '../../utils/auth'
import {
  getDriverTasks,
  getDriverSettlements,
  getDriverReviewStats,
  updateDriverStatus
} from '../../api/delivery'
import { getCurrentUser, updateCurrentUser } from '../../api/user'
import { resolveFileUrl, uploadFile } from '../../api/file'

/** 同 r-tasks：后端无 GET 在线状态端点，当前状态本地持久化 */
const RIDER_ONLINE_KEY = 'bitesmart_rider_online'

Page({
  data: {
    padTop: 44,
    avatar: '',
    active: 'me',
    nickname: '配送骑手',
    initial: '骑',
    avatarUploading: false,
    online: false,
    rating: '—',
    reviewCount: 0,
    totalCount: 0,
    lastOrderId: '' as number | string
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    const user = getUserInfo()
    if (user) {
      const nickname = user.nickname || user.username || '配送骑手'
      this.setData({ nickname, initial: nickname.slice(0, 1), avatar: resolveFileUrl(user.avatar) })
    }
    let online = false
    try {
      online = !!wx.getStorageSync(RIDER_ONLINE_KEY)
    } catch (_) { /* ignore */ }
    this.setData({ online })
    this.loadAll()
    this.refreshAccount()
  },

  onShow() {
    this.refreshAccount()
    this.loadAll()
  },

  refreshAccount() {
    getCurrentUser()
      .then((user) => {
        setUserInfo(user)
        const nickname = user.nickname || user.username || this.data.nickname
        this.setData({ nickname, initial: nickname.slice(0, 1), avatar: resolveFileUrl(user.avatar) })
      })
      .catch(() => {})
  },

  onAvatarError() {
    this.setData({ avatar: '' })
  },

  chooseAvatar() {
    if (this.data.avatarUploading) return
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const filePath = result.tempFilePaths && result.tempFilePaths[0]
        if (!filePath) return
        this.setData({ avatarUploading: true })
        uploadFile(filePath, 'avatar')
          .then((url) => updateCurrentUser({ avatar: url }))
          .then((user) => {
            setUserInfo(user)
            this.setData({ avatar: resolveFileUrl(user.avatar) })
            wx.showToast({ title: '头像已更换', icon: 'success' })
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '头像上传失败', icon: 'none' }))
          .finally(() => this.setData({ avatarUploading: false }))
      }
    })
  },

  loadAll() {
    getDriverTasks()
      .then((tasks) => {
        const latest = (tasks || []).find((task) => task.orderId)
        this.setData({ lastOrderId: (latest && latest.orderId) || '' })
      })
      .catch(() => {})

    getDriverReviewStats()
      .then((s) => this.setData({
        rating: s && s.averageRating != null ? String(s.averageRating) : '—',
        reviewCount: Number((s && s.totalReviews) || 0)
      }))
      .catch((error: Error) => console.warn('[r-me] 评分加载失败：', error && error.message))

    // 累计单量：以已生成结算记录（已送达）的条数计
    getDriverSettlements()
      .then((rows) => this.setData({ totalCount: (rows || []).length }))
      .catch((error: Error) => console.warn('[r-me] 单量加载失败：', error && error.message))
  },

  /** 上线/下线：同步 POST /driver/status，失败回滚（与 r-tasks 同一本地状态） */
  toggleOnline() {
    const next = !this.data.online
    updateDriverStatus(next ? 10 : 30)
      .then(() => {
        try {
          wx.setStorageSync(RIDER_ONLINE_KEY, next)
        } catch (_) { /* ignore */ }
        this.setData({ online: next })
        wx.showToast({ title: next ? '已上线，开始接单' : '已下线', icon: 'none' })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '状态更新失败', icon: 'none' }))
  },

  goReviews() {
    wx.navigateTo({ url: '/pages/r-reviews/r-reviews' })
  },

  goProfile() {
    wx.navigateTo({ url: '/pages/r-profile/r-profile' })
  },

  goComplaint() {
    if (!this.data.lastOrderId) {
      wx.showToast({ title: '暂无可关联的配送订单', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/r-feedback/r-feedback?orderId=${this.data.lastOrderId}` })
  },

  vehicle() {
    this.goProfile()
  },

  logout() {
    wx.showModal({
      title: '退出骑手端？',
      content: '退出后需要重新登录',
      confirmText: '退出',
      confirmColor: '#C0563F',
      success: (res) => {
        if (!res.confirm) return
        clearAuth()
        wx.reLaunch({ url: '/pages/login/login' })
      }
    })
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
