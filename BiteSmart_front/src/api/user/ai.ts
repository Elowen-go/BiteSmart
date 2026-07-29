import request from '../../utils/request'

export interface AiConversation {
  id: number
  userId: number
  sessionId: string
  question: string
  answer: string
  createTime: string
}

export interface AiRecommendRequest {
  mealType?: 'all' | 'breakfast' | 'lunch' | 'dinner'
  dietaryRestrictions?: string
}

export const aiChat = (question: string, sessionId?: string): Promise<any> => {
  return request.post('/ai/chat', null, { params: { question, sessionId } })
}

/** Reads the server-sent events emitted by the streaming AI endpoint. */
export const aiChatStream = async (
  question: string,
  sessionId: string | undefined,
  onToken: (token: string) => void
): Promise<void> => {
  const params = new URLSearchParams({ question })
  if (sessionId) params.set('sessionId', sessionId)

  const response = await fetch(`/api/ai/chat/stream?${params.toString()}`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${localStorage.getItem('token') || ''}`
    }
  })
  if (!response.ok || !response.body) {
    throw new Error('AI 流式接口请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const consumeEvent = (rawEvent: string) => {
    let eventName = 'message'
    const dataLines: string[] = []
    for (const line of rawEvent.split(/\r?\n/)) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) {
        const data = line.slice(5)
        dataLines.push(data.startsWith(' ') ? data.slice(1) : data)
      }
    }
    if (eventName === 'token') onToken(dataLines.join('\n'))
    if (eventName === 'error') throw new Error(dataLines.join('\n'))
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    events.filter(Boolean).forEach(consumeEvent)
    if (done) break
  }
  if (buffer.trim()) consumeEvent(buffer)
}

export const getChatHistory = (sessionId: string): Promise<any> => {
  return request.get('/ai/chat/history', { params: { sessionId } })
}

export const aiRecommend = (data: AiRecommendRequest = {}): Promise<any> => {
  return request.post('/ai/recommend', data)
}
