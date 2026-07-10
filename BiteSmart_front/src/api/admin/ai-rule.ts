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
  return request.get('/admin/ai-rules')
}

export const addAiRule = (data: Partial<AiRecommendRule>): Promise<any> => {
  return request.post('/admin/ai-rules', data)
}

export const updateAiRule = (id: number, data: Partial<AiRecommendRule>): Promise<any> => {
  return request.put(`/admin/ai-rules/${id}`, data)
}

export const deleteAiRule = (id: number): Promise<any> => {
  return request.delete(`/admin/ai-rules/${id}`)
}