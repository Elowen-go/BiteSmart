import { chat, getChatHistory, recommend, type AiConversation } from '../../api/ai'
import { addToCart } from '../../api/cart'
import { MOCK_DISHES, uimg } from '../../mock/catalog'
import { getUserInfo } from '../../utils/auth'
import { API_ORIGIN } from '../../utils/request'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

const QUESTIONS = ['减脂晚餐怎么吃？', '今天蛋白质够吗？', '推荐一份低卡早餐']
const normalizeAvatar = (value: unknown): string => {
  const avatar = String(value || '').trim()
  if (!avatar) return ''
  if (/^(https?:\/\/|data:image\/)/i.test(avatar)) return avatar
  if (avatar.startsWith('/uploads/')) {
    return `${API_ORIGIN}/api/files/download/${avatar.slice('/uploads/'.length)}`
  }
  if (avatar.startsWith('/api/')) return `${API_ORIGIN}${avatar}`
  if (avatar.startsWith('/')) return `${API_ORIGIN}${avatar}`
  return `${API_ORIGIN}/${avatar.replace(/^\/+/, '')}`
}
const INITIAL_RECOMMENDATIONS = MOCK_DISHES.slice(0, 3).map((dish) => ({
  id: dish.id as number | string,
  name: dish.name,
  calories: dish.kcal,
  protein: dish.protein,
  price: dish.price,
  image: uimg(dish.img, 500)
}))

Page({
  data: {
    loading: false,
    avatar: '',
    initial: '王',
    question: '',
    sessionId: '',
    messages: [] as AiConversation[],
    toView: '',
    image: uimg(MOCK_DISHES[0].img, 900),
    recommendation: {
      // 推荐菜品 id 为雪花字符串，原样透传禁止 Number() 强转
      id: MOCK_DISHES[0].id as number | string,
      name: MOCK_DISHES[0].name,
      calories: MOCK_DISHES[0].kcal,
      protein: MOCK_DISHES[0].protein,
      price: MOCK_DISHES[0].price,
      image: ''
    },
    recommendations: INITIAL_RECOMMENDATIONS,
    suggestions: QUESTIONS,
    padTop: 0
  },
  onLoad() {
    if (!requireUser()) return
    const user = getUserInfo()
    const nickname = (user && (user.nickname || user.username)) || '王硕'
    const { padTop } = getSafeArea()
    const sessionId = wx.getStorageSync('aiSessionId') || ''
    this.setData({
      padTop,
      sessionId,
      avatar: normalizeAvatar(user && user.avatar),
      initial: nickname.slice(0, 1)
    })
    if (sessionId) {
      getChatHistory(sessionId)
        .then((messages) => this.setData({ messages, toView: messages.length ? `m${messages.length - 1}` : '' }))
        .catch(() => {})
    }
    recommend().then((result) => {
      const item = (result.dish || result.recommendation || result) as Record<string, unknown>
      const recommendation = {
          id: (item.id || item.dishId || this.data.recommendation.id) as number | string,
          name: String(item.name || item.dishName || this.data.recommendation.name),
          calories: Number(item.calories || this.data.recommendation.calories),
          protein: Number(item.protein || this.data.recommendation.protein),
          price: Number(item.price || this.data.recommendation.price),
          image: String(item.image || this.data.image)
      }
      this.setData({
        recommendation,
        recommendations: [recommendation, ...this.data.recommendations.slice(1)]
      })
    }).catch(() => {})
  },
  onInput(event: WechatMiniprogram.Input) {
    this.setData({ question: event.detail.value })
  },
  onAvatarError() {
    this.setData({ avatar: '' })
  },
  send(questionOverride?: string | WechatMiniprogram.TouchEvent | WechatMiniprogram.Input) {
    const override = typeof questionOverride === 'string' ? questionOverride : ''
    const question = (override || this.data.question).trim()
    if (!question || this.data.loading) return
    const appended = [...this.data.messages, { question }]
    this.setData({ loading: true, messages: appended, question: '', toView: `m${appended.length - 1}` })
    chat(question, this.data.sessionId || undefined)
      .then((result) => {
        if (result.sessionId) {
          wx.setStorageSync('aiSessionId', result.sessionId)
          this.setData({ sessionId: result.sessionId })
        }
        const messages = [...this.data.messages, result]
        this.setData({ messages, toView: `m${messages.length - 1}` })
      })
      .catch(() => wx.showToast({ title: 'AI 暂时没有回应', icon: 'none' }))
      .finally(() => this.setData({ loading: false }))
  },
  useSuggestion(event: WechatMiniprogram.CustomEvent) {
    const question = String(event.currentTarget.dataset.question || '')
    this.send(question)
  },
  openChat(event?: WechatMiniprogram.CustomEvent) {
    const question = event && event.currentTarget.dataset.question
    wx.navigateTo({ url: `/pages/chat/chat${question ? `?question=${encodeURIComponent(String(question))}` : ''}` })
  },
  addRecommendation(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id || this.data.recommendation.id
    addToCart(10, id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },
  openRecommendation(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id || this.data.recommendation.id
    wx.navigateTo({ url: `/pages/food-detail/food-detail?id=${id}` })
  },
  goFood() { wx.switchTab({ url: '/pages/food/food' }) },
  goAssessment() { wx.navigateTo({ url: '/pages/assessment/assessment' }) }
})
