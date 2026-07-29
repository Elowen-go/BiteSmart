import { chatStream, getChatHistory, getChatSessions, recommend, type AiConversation } from '../../api/ai'
import { addToCart } from '../../api/cart'
import { getDishes } from '../../api/catalog'
import { MOCK_DISHES, uimg } from '../../mock/catalog'
import { getUserInfo } from '../../utils/auth'
import { API_ORIGIN } from '../../utils/request'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'
import { buildAiDishCards, toAiDishCard, type AiDishCard } from '../../utils/ai-dish'

const QUESTIONS = ['减脂晚餐怎么吃？', '今天蛋白质够吗？', '推荐一份低卡早餐']
let sending = false
type AiSessionSummary = { sessionId: string; title: string; time: string }
type DisplayConversation = AiConversation & { formattedAnswer?: string; dishCards?: AiDishCard[] }
const formatAiAnswer = (answer: string): string => String(answer || '')
  .replace(/\r\n/g, '\n')
  .replace(/\*\*([^*]+)\*\*/g, '$1')
  .replace(/__([^_]+)__/g, '$1')
  .replace(/^#{1,3}\s+/gm, '')
  .replace(/^[-*]\s+/gm, '• ')
  .replace(/^\s*(\d+)[.)]\s*/gm, '$1. ')
  .replace(/\n{3,}/g, '\n\n')
  .trim()
const toDisplayMessage = (message: AiConversation, dishes: AiDishCard[] = []): DisplayConversation => ({
  ...message,
  formattedAnswer: formatAiAnswer(message.answer || ''),
  dishCards: buildAiDishCards(message.question || '', dishes)
})
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
    streamingAnswer: false,
    avatar: '',
    initial: '王',
    question: '',
    sessionId: '',
    messages: [] as DisplayConversation[],
    sessions: [] as AiSessionSummary[],
    availableDishes: [] as AiDishCard[],
    historyOpen: false,
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
    getDishes()
      .then((result) => {
        const records = Array.isArray(result) ? result : result.records
        this.setData({ availableDishes: (records || []).map(toAiDishCard).filter((item): item is AiDishCard => item !== null) })
      })
      .catch(() => {})
      .finally(() => this.loadSessions(sessionId))
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
  loadSession(sessionId: string) {
    if (!sessionId) return Promise.resolve()
    return getChatHistory(sessionId)
      .then((messages) => this.setData({ sessionId, messages: messages.map((message) => toDisplayMessage(message, this.data.availableDishes)), toView: messages.length ? `m${messages.length - 1}` : '' }))
      .catch(() => {})
  },
  loadSessions(preferredSessionId: string) {
    getChatSessions().then((records) => {
      const grouped: AiSessionSummary[] = []
      const seen: Record<string, boolean> = {}
      records.forEach((record) => {
        const id = String(record.sessionId || '')
        if (!id || seen[id]) return
        seen[id] = true
        grouped.push({
          sessionId: id,
          title: String(record.question || '新的健康对话').slice(0, 24),
          time: String(record.createTime || '').replace('T', ' ').slice(0, 16)
        })
      })
      this.setData({ sessions: grouped })
      const target = preferredSessionId || (grouped[0] && grouped[0].sessionId) || ''
      if (target) {
        wx.setStorageSync('aiSessionId', target)
        this.loadSession(target)
      }
    }).catch(() => {
      if (preferredSessionId) this.loadSession(preferredSessionId)
    })
  },
  startNewChat() {
    if (sending) return
    wx.removeStorageSync('aiSessionId')
    this.setData({ sessionId: '', messages: [], historyOpen: false, toView: '' })
  },
  openHistory() { this.setData({ historyOpen: true }) },
  closeHistory() { this.setData({ historyOpen: false }) },
  noop() {},
  selectHistory(event: WechatMiniprogram.CustomEvent) {
    const sessionId = String(event.currentTarget.dataset.sessionId || '')
    if (!sessionId) return
    wx.setStorageSync('aiSessionId', sessionId)
    this.setData({ historyOpen: false })
    this.loadSession(sessionId)
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
    if (!question || this.data.loading || sending) return
    sending = true
    const pendingIndex = this.data.messages.length
    const appended: DisplayConversation[] = [...this.data.messages, { question, answer: '', formattedAnswer: '', dishCards: [] }]
    this.setData({ loading: true, streamingAnswer: false, messages: appended, question: '', toView: `m${appended.length - 1}` })
    chatStream(question, this.data.sessionId || undefined, (token) => {
      const messages = [...this.data.messages]
      const pending = messages[pendingIndex]
      if (!pending) return
      pending.answer = `${pending.answer || ''}${token}`
      pending.formattedAnswer = formatAiAnswer(pending.answer)
      this.setData({ messages, streamingAnswer: true, toView: `m${pendingIndex}` })
    })
      .then((result) => {
        if (result.sessionId) {
          wx.setStorageSync('aiSessionId', result.sessionId)
          this.setData({ sessionId: result.sessionId })
        }
        const messages = [...this.data.messages]
        messages[pendingIndex] = toDisplayMessage({ ...messages[pendingIndex], ...result }, this.data.availableDishes)
        this.setData({ messages, toView: `m${messages.length - 1}` })
      })
      .catch((error: unknown) => {
        console.error('AI stream failed', error)
        const messages = [...this.data.messages]
        if (messages[pendingIndex] && !messages[pendingIndex].answer) messages.splice(pendingIndex, 1)
        this.setData({ messages, toView: messages.length ? `m${messages.length - 1}` : '' })
        const message = error instanceof Error && error.message ? error.message : 'AI 暂时没有回应'
        wx.showToast({ title: message.slice(0, 28), icon: 'none' })
      })
      .finally(() => this.setData({ loading: false, streamingAnswer: false }))
      .finally(() => { sending = false })
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
  addRecommendedDish(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    addToCart(10, id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '加入购物车失败', icon: 'none' }))
  },
  openRecommendation(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id || this.data.recommendation.id
    wx.navigateTo({ url: `/pages/food-detail/food-detail?id=${id}` })
  },
  goFood() { wx.switchTab({ url: '/pages/food/food' }) },
  goAssessment() { wx.navigateTo({ url: '/pages/assessment/assessment' }) }
})
