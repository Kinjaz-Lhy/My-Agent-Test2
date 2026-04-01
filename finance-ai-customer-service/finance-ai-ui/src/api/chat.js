import axios from 'axios'
import { getToken } from '@/utils/auth'
import { streamChat as sseStreamChat } from '@/utils/sse'

const http = axios.create({ baseURL: '/api/v1/chat' })

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Start SSE streaming chat. Returns AbortController. */
export function streamChat(sessionId, message, callbacks) {
  return sseStreamChat(sessionId, message, callbacks)
}

/** Get session list for current user */
export async function getSessions() {
  const { data } = await http.get('/sessions')
  return data
}

/** Get session detail by ID */
export async function getSession(sessionId) {
  const { data } = await http.get(`/sessions/${sessionId}`)
  return data
}

/** Close a session */
export async function closeSession(sessionId) {
  const { data } = await http.post(`/sessions/${sessionId}/close`)
  return data
}

/** Submit satisfaction feedback */
export async function submitFeedback(sessionId, score, comment) {
  const { data } = await http.post(`/sessions/${sessionId}/feedback`, { score, comment })
  return data
}

/** Rename a session */
export async function renameSession(sessionId, title) {
  await http.post(`/sessions/${sessionId}/rename`, { title })
}

/** Pin/unpin a session */
export async function pinSession(sessionId, pinned) {
  await http.post(`/sessions/${sessionId}/pin`, { pinned })
}

/** Delete a session */
export async function deleteSession(sessionId) {
  await http.post(`/sessions/${sessionId}/delete`)
}
