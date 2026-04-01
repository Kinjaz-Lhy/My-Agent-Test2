import axios from 'axios'
import { getToken } from '@/utils/auth'

const http = axios.create({ baseURL: '/api/v1/admin' })

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Query conversation logs with filters */
export async function queryLogs(params) {
  const { data } = await http.get('/logs', { params })
  return data
}

/** Get hot topics statistics */
export async function getHotTopics(params) {
  const { data } = await http.get('/hot-topics', { params })
  return data
}

/** Get operation metrics dashboard data */
export async function getMetrics(params) {
  const { data } = await http.get('/metrics', { params })
  return data
}

/** Add a new knowledge entry */
export async function addKnowledge(entry) {
  const { data } = await http.post('/knowledge', entry)
  return data
}

/** Update an existing knowledge entry */
export async function updateKnowledge(entryId, entry) {
  const { data } = await http.put(`/knowledge/${entryId}`, entry)
  return data
}

/** Query knowledge entries by category */
export async function queryKnowledge(params) {
  const { data } = await http.get('/knowledge', { params })
  return data
}

/** Get knowledge categories */
export async function getKnowledgeCategories() {
  const { data } = await http.get('/knowledge/categories')
  return data
}

/** Add a knowledge category */
export async function addKnowledgeCategory(category) {
  const { data } = await http.post('/knowledge/categories', category)
  return data
}

/** Update a knowledge category */
export async function updateKnowledgeCategory(categoryId, category) {
  const { data } = await http.put(`/knowledge/categories/${categoryId}`, category)
  return data
}

/** Delete a knowledge category */
export async function deleteKnowledgeCategory(categoryId) {
  const { data } = await http.delete(`/knowledge/categories/${categoryId}`)
  return data
}

/** Review (approve/reject) a knowledge entry */
export async function reviewKnowledge(entryId, approved) {
  const { data } = await http.put(`/knowledge/${entryId}/review`, { approved })
  return data
}

/** Delete a knowledge entry */
export async function deleteKnowledge(entryId) {
  const { data } = await http.delete(`/knowledge/${entryId}`)
  return data
}

/** Get auto-reply rules */
export async function getAutoReplyRules() {
  const { data } = await http.get('/auto-reply-rules')
  return data
}

/** Create or update an auto-reply rule */
export async function saveAutoReplyRule(rule) {
  const { data } = await http.post('/auto-reply-rules', rule)
  return data
}
