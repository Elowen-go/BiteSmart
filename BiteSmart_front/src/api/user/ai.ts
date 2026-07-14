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
  return request.post('/ai/chat', { params: { question, sessionId } })
}

export const getChatHistory = (sessionId: string): Promise<any> => {
  return request.get('/ai/chat/history', { params: { sessionId } })
}

export const aiRecommend = (data: AiRecommendRequest = {}): Promise<any> => {
  return request.post('/ai/recommend', data)
}
