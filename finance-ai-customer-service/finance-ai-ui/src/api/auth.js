import axios from 'axios'
import { getToken, setToken, setRefreshToken, getRefreshToken } from '@/utils/auth'

const http = axios.create({ baseURL: '/api/v1/auth' })

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Redirect to SSO login page */
export function ssoLogin(redirectUrl) {
  const params = redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''
  window.location.href = `/api/v1/auth/sso/login${params}`
}

/** Handle SSO callback — exchange code for tokens */
export async function ssoCallback(code) {
  const { data } = await http.post('/sso/callback', { code })
  if (data.token) {
    setToken(data.token)
    if (data.refreshToken) setRefreshToken(data.refreshToken)
  }
  return data
}

/** Refresh the access token using the refresh token */
export async function refreshToken() {
  const { data } = await http.post('/token/refresh', {
    refreshToken: getRefreshToken()
  })
  if (data.token) {
    setToken(data.token)
    if (data.refreshToken) setRefreshToken(data.refreshToken)
  }
  return data
}
