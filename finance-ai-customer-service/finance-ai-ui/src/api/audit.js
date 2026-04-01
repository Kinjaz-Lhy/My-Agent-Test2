import axios from 'axios'
import { getToken } from '@/utils/auth'

const http = axios.create({ baseURL: '/api/v1/audit' })

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Query audit logs with filters */
export async function queryAuditLogs(params) {
  const { data } = await http.get('/logs', { params })
  return data
}
