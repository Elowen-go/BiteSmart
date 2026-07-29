import { request } from '../utils/request'
import { getToken } from '../utils/auth'
import { API_ORIGIN } from '../utils/request'

export interface AiConversation { id?: number; sessionId?: string; question?: string; answer?: string; createTime?: string }
export const recommend = (data: Record<string, unknown> = {}): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: '/ai/recommend', method: 'POST', data })
export const chat = (question: string, sessionId?: string): Promise<AiConversation> => request<AiConversation>({ url: '/ai/chat', method: 'POST', data: { question, sessionId }, contentType: 'form' })

/** Receives the backend SSE response chunk by chunk on WeChat. */
export const chatStream = (
  question: string,
  sessionId: string | undefined,
  onToken: (token: string) => void
): Promise<AiConversation> => new Promise((resolve, reject) => {
  const token = getToken()
  if (!token) {
    reject(new Error('请先登录'))
    return
  }

  const effectiveSessionId = sessionId || `mini-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  const query = [`question=${encodeURIComponent(question)}`, `sessionId=${encodeURIComponent(effectiveSessionId)}`]
  let buffer = ''
  let answer = ''
  let settled = false
  let receivedChunk = false
  let requestTask: WechatMiniprogram.RequestTask | null = null
  let timeoutTimer: ReturnType<typeof setTimeout> | null = null
  type Decoder = { decode(data?: ArrayBuffer, options?: { stream?: boolean }): string }
  type RuntimeGlobal = { TextDecoder?: new (label?: string) => Decoder }
  let decoder: Decoder | null = null
  try {
    // Older WeChat runtimes may not define globalThis, so never access it directly.
    const runtime = typeof globalThis !== 'undefined' ? globalThis as unknown as RuntimeGlobal : null
    if (runtime && runtime.TextDecoder) decoder = new runtime.TextDecoder('utf-8')
  } catch (_) {
    decoder = null
  }
  let pendingUtf8Bytes: number[] = []

  const consumeEvent = (eventText: string) => {
    let eventName = 'message'
    const dataLines: string[] = []
    eventText.split(/\r?\n/).forEach((line) => {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) {
        const data = line.slice(5)
        dataLines.push(data.startsWith(' ') ? data.slice(1) : data)
      }
    })
    const data = dataLines.join('\n')
    if (eventName === 'token') {
      answer += data
      onToken(data)
    }
    if (eventName === 'done') finish()
    if (eventName === 'error') throw new Error(data || 'AI 流式请求失败')
  }

  const decodeWithoutTextDecoder = (bytes: Uint8Array, final: boolean): string => {
    const values = pendingUtf8Bytes.concat(Array.prototype.slice.call(bytes) as number[])
    let completeLength = values.length
    if (!final) {
      let continuationCount = 0
      for (let i = values.length - 1; i >= 0 && (values[i] & 0xc0) === 0x80; i -= 1) continuationCount += 1
      const leadIndex = values.length - continuationCount - 1
      if (leadIndex >= 0) {
        const lead = values[leadIndex]
        const expected = lead >= 0xf0 ? 4 : lead >= 0xe0 ? 3 : lead >= 0xc0 ? 2 : 1
        if (continuationCount + 1 < expected) completeLength = leadIndex
      }
    }
    pendingUtf8Bytes = values.slice(completeLength)
    let encoded = ''
    for (let i = 0; i < completeLength; i += 1) {
      const hex = values[i].toString(16)
      encoded += `%${hex.length === 1 ? `0${hex}` : hex}`
    }
    try {
      return encoded ? decodeURIComponent(encoded) : ''
    } catch (_) {
      return ''
    }
  }

  const consumeText = (text: string) => {
    buffer += text
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    events.filter(Boolean).forEach(consumeEvent)
  }

  const consumeChunk = (chunk: ArrayBuffer) => {
    receivedChunk = true
    const bytes = new Uint8Array(chunk)
    const text = decoder ? decoder.decode(bytes.buffer, { stream: true }) : decodeWithoutTextDecoder(bytes, false)
    consumeText(text)
  }

  const responseMessage = (data: unknown): string => {
    if (typeof data === 'string') return data.slice(0, 80)
    if (data && typeof data === 'object') {
      const body = data as Record<string, unknown>
      return String(body.message || body.errMsg || body.error || '')
    }
    return ''
  }

  const finish = (error?: Error) => {
    if (settled) return
    if (error) {
      settled = true
      if (timeoutTimer) clearTimeout(timeoutTimer)
      reject(error)
      return
    }
    try {
      if (decoder) buffer += decoder.decode(undefined, { stream: false })
      else buffer += decodeWithoutTextDecoder(new Uint8Array(0), true)
      if (buffer.trim()) consumeEvent(buffer)
      if (!answer.trim()) throw new Error('AI 接口未返回内容')
      settled = true
      if (timeoutTimer) clearTimeout(timeoutTimer)
      resolve({ question, answer, sessionId: effectiveSessionId })
    } catch (finishError) {
      settled = true
      if (timeoutTimer) clearTimeout(timeoutTimer)
      reject(finishError instanceof Error ? finishError : new Error('AI 流式解析失败'))
    }
  }

  requestTask = wx.request({
    url: `${API_ORIGIN}/api/ai/chat/stream?${query.join('&')}`,
    method: 'POST',
    enableChunked: true,
    timeout: 180000,
    header: {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`
    },
    success: (response) => {
      try {
        if (response.statusCode >= 400) {
          const message = responseMessage(response.data)
          finish(new Error(`AI 请求失败（${response.statusCode}）${message ? `：${message}` : ''}`))
          return
        }
        // Some WeChat developer tools deliver the body only in success().
        if (!receivedChunk) {
          if (response.data instanceof ArrayBuffer) consumeChunk(response.data)
          else if (typeof response.data === 'string') consumeText(response.data)
        }
        finish()
      } catch (error) {
        finish(error instanceof Error ? error : new Error('AI 流式解析失败'))
      }
    },
    fail: (error) => {
      const message = error && typeof error === 'object' && 'errMsg' in error
        ? String((error as { errMsg?: unknown }).errMsg || '')
        : ''
      finish(new Error(message || 'AI 流式请求失败'))
    },
    complete: () => {
      // Some WeChat runtimes may omit the final SSE callback but still complete the request.
      if (!settled && answer.trim()) finish()
    }
  } as WechatMiniprogram.RequestOption)

  const chunkTask = requestTask as WechatMiniprogram.RequestTask & {
    onChunkReceived?: (callback: (response: { data: ArrayBuffer }) => void) => void
  }
  if (chunkTask.onChunkReceived) {
    chunkTask.onChunkReceived((response) => {
      try {
        consumeChunk(response.data)
      } catch (error) {
        if (requestTask) requestTask.abort()
        finish(error instanceof Error ? error : new Error('AI 流式解析失败'))
      }
    })
  }

  timeoutTimer = setTimeout(() => {
    if (requestTask) requestTask.abort()
    finish(new Error('AI 回复超时，请稍后重试'))
  }, 180000)
})
export const getChatHistory = (sessionId: string): Promise<AiConversation[]> => request<AiConversation[]>({ url: '/ai/chat/history', data: { sessionId } })
export const getChatSessions = (): Promise<AiConversation[]> => request<AiConversation[]>({ url: '/ai/chat/sessions' })
