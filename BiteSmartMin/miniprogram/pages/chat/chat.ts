import { chat, getChatHistory, type AiConversation } from '../../api/ai'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

const SUGGESTIONS = ['减脂晚餐怎么吃？', '今天蛋白质够吗？', '帮我配一周餐单']

Page({
  data: {
    question: '',
    loading: false,
    sessionId: '',
    messages: [] as AiConversation[],
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
    if (sessionId) {
      getChatHistory(sessionId)
        .then((messages) => this.setData({ messages, toView: messages.length ? `m${messages.length - 1}` : '' }))
        .catch(() => {})
    }
  },
  back() { wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) { this.setData({ question: event.detail.value }) },
  send(questionOverride?: string | WechatMiniprogram.TouchEvent | WechatMiniprogram.Input) {
    const override = typeof questionOverride === 'string' ? questionOverride : ''
    const question = (override || this.data.question).trim()
    if (!question || this.data.loading) return
    const appended = [...this.data.messages, { question }]
    this.setData({ loading: true, messages: appended, toView: `m${appended.length - 1}` })
    chat(question, this.data.sessionId || undefined).then((result) => {
      if (result.sessionId) { wx.setStorageSync('aiSessionId', result.sessionId); this.setData({ sessionId: result.sessionId }) }
      const messages = [...this.data.messages, result]
      this.setData({ messages, question: '', toView: `m${messages.length - 1}` })
    }).catch(() => wx.showToast({ title: 'AI 暂时没有回应', icon: 'none' })).finally(() => this.setData({ loading: false }))
  },
  useSuggestion(event: WechatMiniprogram.CustomEvent) {
    const question = String(event.currentTarget.dataset.question || '')
    this.setData({ question })
    this.send(question)
  }
})
