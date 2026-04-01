const TOKEN_KEY = 'finance_ai_token'
const REFRESH_TOKEN_KEY = 'finance_ai_refresh_token'

/**
 * Get the stored JWT access token.
 * @returns {string|null}
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * Store the JWT access token.
 * @param {string} token
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * Remove the stored JWT access token.
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * Get the stored refresh token.
 * @returns {string|null}
 */
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/**
 * Store the refresh token.
 * @param {string} token
 */
export function setRefreshToken(token) {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/**
 * Remove the stored refresh token.
 */
export function removeRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/**
 * Check if the user is authenticated (has a non-expired token).
 * Performs a basic JWT expiry check without verifying the signature.
 * @returns {boolean}
 */
export function isAuthenticated() {
  const token = getToken()
  if (!token) return false

  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    // Check expiry: exp is in seconds, Date.now() in ms
    return payload.exp * 1000 > Date.now()
  } catch {
    return false
  }
}

/**
 * Parse user info from the JWT token payload.
 * @returns {{ employeeId: string, username: string, roles: string[], departmentId: string } | null}
 */
export function parseUserFromToken() {
  const token = getToken()
  if (!token) return null

  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return {
      employeeId: payload.sub || payload.employeeId,
      username: payload.username || payload.name || '',
      roles: payload.roles || [],
      departmentId: payload.departmentId || ''
    }
  } catch {
    return null
  }
}

/**
 * Clear all auth data on logout.
 */
export function clearAuth() {
  removeToken()
  removeRefreshToken()
}
