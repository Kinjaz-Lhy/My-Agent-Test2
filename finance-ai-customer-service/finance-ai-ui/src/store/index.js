import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, parseUserFromToken, clearAuth, isAuthenticated } from '@/utils/auth'

/**
 * User store: manages authentication state and user info.
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: parseUserFromToken() || null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token && isAuthenticated(),
    roles: (state) => state.userInfo?.roles || [],
    employeeId: (state) => state.userInfo?.employeeId || '',
    username: (state) => state.userInfo?.username || ''
  },

  actions: {
    login(token) {
      this.token = token
      setToken(token)
      this.userInfo = parseUserFromToken()
    },

    logout() {
      this.token = ''
      this.userInfo = null
      clearAuth()
    },

    /** Refresh user info from the current token (e.g. after token refresh). */
    refreshUserInfo() {
      this.token = getToken() || ''
      this.userInfo = parseUserFromToken()
    }
  }
})

/**
 * Chat store: manages active chat session state.
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    currentSessionId: null,
    sessions: [],
    messages: [],
    isStreaming: false
  }),

  getters: {
    currentSession: (state) =>
      state.sessions.find(s => s.sessionId === state.currentSessionId) || null
  },

  actions: {
    setCurrentSession(sessionId) {
      this.currentSessionId = sessionId
    },

    setSessions(sessions) {
      this.sessions = sessions
    },

    setMessages(messages) {
      this.messages = messages
    },

    addMessage(message) {
      this.messages.push(message)
    },

    /** Append streaming content to the last assistant message. */
    appendToLastMessage(content) {
      const lastMsg = this.messages[this.messages.length - 1]
      if (lastMsg && lastMsg.role === 'ASSISTANT') {
        lastMsg.content += content
      } else {
        this.messages.push({ role: 'ASSISTANT', content })
      }
    },

    setStreaming(value) {
      this.isStreaming = value
    },

    clearChat() {
      this.currentSessionId = null
      this.messages = []
      this.isStreaming = false
    }
  }
})
