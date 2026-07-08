import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export interface AgentDTO {
  id: number
  name: string
  description: string
  avatar: string
  status: string
  creator: string
  createdAt: string
  updatedAt: string
}

export function getAgentList(params: any) {
  return request.get('/agents', { params })
}

export function getAgentDetail(id: number) {
  return request.get(`/agents/${id}`)
}

export function createAgent(data: Partial<AgentDTO>) {
  return request.post('/agents', data)
}

export function updateAgent(id: number, data: Partial<AgentDTO>) {
  return request.put(`/agents/${id}`, data)
}

export function deleteAgent(id: number) {
  return request.delete(`/agents/${id}`)
}

export default request