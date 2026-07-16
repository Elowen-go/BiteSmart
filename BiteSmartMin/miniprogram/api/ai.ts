import { request } from '../utils/request'

export interface AiConversation { id?: number; sessionId?: string; question?: string; answer?: string; createTime?: string }
export const recommend = (data: Record<string, unknown> = {}): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: '/ai/recommend', method: 'POST', data })
export const chat = (question: string, sessionId?: string): Promise<AiConversation> => request<AiConversation>({ url: '/ai/chat', method: 'POST', data: { question, sessionId }, contentType: 'form' })
export const getChatHistory = (sessionId: string): Promise<AiConversation[]> => request<AiConversation[]>({ url: '/ai/chat/history', data: { sessionId } })
