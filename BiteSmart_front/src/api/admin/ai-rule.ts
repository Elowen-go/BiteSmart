import request from '../../utils/request'

export interface AiRecommendRule {
  id: number
  ruleName: string
  description: string
  ruleType: string
  ruleConfig: string
  enabled: boolean
  priority: number
  createTime: string
  updateTime: string
}

export const listAiRules = (): Promise<any> => {
  return request.get('/api/admin/ai-rules')
}

export const addAiRule = (data: Partial<AiRecommendRule>): Promise<any> => {
  return request.post('/api/admin/ai-rules', data)
}

export const updateAiRule = (id: number, data: Partial<AiRecommendRule>): Promise<any> => {
  return request.put(`/api/admin/ai-rules/${id}`, data)
}

export const deleteAiRule = (id: number): Promise<any> => {
  return request.delete(`/api/admin/ai-rules/${id}`)
}
