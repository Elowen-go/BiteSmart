import { chatStream, getChatHistory, getChatSessions, type AiConversation } from '../../api/ai'
import { addToCart } from '../../api/cart'
import { getDishes } from '../../api/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'
import { buildAiDishCards, toAiDishCard, type AiDishCard } from '../../utils/ai-dish'

const SUGGESTIONS = ['减脂晚餐怎么吃？', '今天蛋白质够吗？', '帮我配一周餐单']
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

Page({
  data: {
    question: '',
    loading: false,
    streamingAnswer: false,
    sessionId: '',
    messages: [] as DisplayConversation[],
    availableDishes: [] as AiDishCard[],
    sessions: [] as AiSessionSummary[],
    historyOpen: false,
    suggestions: SUGGESTIONS,
    toView: '',
    menuTop: 0,
    menuH: 32
  },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    const sessionId = wx.getStorageSync('aiSessionId') || ''
    this.setData({ menuTop, menuH, sessionId, question: options && options.question ? decodeURIComponent(options.question) : '' })
    getDishes()
      .then((result) => {
        const records = Array.isArray(result) ? result : result.records
        this.setData({ availableDishes: (records || []).map(toAiDishCard).filter((item): item is AiDishCard => item !== null) })
      })
      .catch(() => {})
      .finally(() => this.loadSessions(sessionId))
  },
  loadSession(sessionId: string) {
    if (!sessionId) return Promise.resolve()
    return getChatHistory(sessionId)
      .then((messages) => this.setData({ sessionId, messages: messages.map((message) => toDisplayMessage(message, this.data.availableDishes)), toView: messages.length ? `m${messages.length - 1}` : '' }))
      .catch(() => {})
  },
  loadSessions(preferredSessionId: string) {
    getChatSessions().then((records) => {
      const sessions: AiSessionSummary[] = []
      const seen: Record<string, boolean> = {}
      records.forEach((record) => {
        const sessionId = String(record.sessionId || '')
        if (!sessionId || seen[sessionId]) return
        seen[sessionId] = true
        sessions.push({
          sessionId,
          title: String(record.question || '新的健康对话').slice(0, 24),
          time: String(record.createTime || '').replace('T', ' ').slice(0, 16)
        })
      })
      this.setData({ sessions })
      const target = preferredSessionId || (sessions[0] && sessions[0].sessionId) || ''
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
  back() { wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) { this.setData({ question: event.detail.value }) },
  send(questionOverride?: string | WechatMiniprogram.TouchEvent | WechatMiniprogram.Input) {
    const override = typeof questionOverride === 'string' ? questionOverride : ''
    const question = (override || this.data.question).trim()
    if (!question || this.data.loading || sending) return
    sending = true
    const pendingIndex = this.data.messages.length
    const appended: DisplayConversation[] = [...this.data.messages, { question, answer: '', formattedAnswer: '', dishCards: [] }]
    this.setData({ loading: true, streamingAnswer: false, messages: appended, toView: `m${appended.length - 1}` })
    chatStream(question, this.data.sessionId || undefined, (token) => {
      const messages = [...this.data.messages]
      const pending = messages[pendingIndex]
      if (!pending) return
      pending.answer = `${pending.answer || ''}${token}`
      pending.formattedAnswer = formatAiAnswer(pending.answer)
      this.setData({ messages, streamingAnswer: true, toView: `m${pendingIndex}` })
    }).then((result) => {
      if (result.sessionId) { wx.setStorageSync('aiSessionId', result.sessionId); this.setData({ sessionId: result.sessionId }) }
      const messages = [...this.data.messages]
      messages[pendingIndex] = toDisplayMessage({ ...messages[pendingIndex], ...result }, this.data.availableDishes)
      this.setData({ messages, question: '', toView: `m${messages.length - 1}` })
    }).catch((error: unknown) => {
      console.error('AI stream failed', error)
      const messages = [...this.data.messages]
      if (messages[pendingIndex] && !messages[pendingIndex].answer) messages.splice(pendingIndex, 1)
      this.setData({ messages, toView: messages.length ? `m${messages.length - 1}` : '' })
      const message = error instanceof Error && error.message ? error.message : 'AI 暂时没有回应'
      wx.showToast({ title: message.slice(0, 28), icon: 'none' })
    }).finally(() => this.setData({ loading: false, streamingAnswer: false }))
      .finally(() => { sending = false })
  },
  useSuggestion(event: WechatMiniprogram.CustomEvent) {
    const question = String(event.currentTarget.dataset.question || '')
    this.setData({ question })
    this.send(question)
  },
  addRecommendedDish(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    addToCart(10, id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '加入购物车失败', icon: 'none' }))
  }
})
