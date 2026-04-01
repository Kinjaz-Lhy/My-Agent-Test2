<template>
  <div class="chat-view">
    <!-- Left sidebar: session list -->
    <aside class="sidebar">
      <SessionList
        :sessions="chatStore.sessions"
        :current-id="chatStore.currentSessionId"
        @select="selectSession"
        @new-session="startNewSession"
        @refresh="loadSessions"
        @deleted="onSessionDeleted"
      />
    </aside>

    <!-- Right area: messages + input -->
    <main class="chat-main">
      <div class="chat-header">
        <span>智能客服对话</span>
        <el-button
          v-if="chatStore.currentSessionId && currentSession?.status === 'ACTIVE'"
          type="danger"
          text
          size="small"
          @click="handleCloseSession"
        >
          结束会话
        </el-button>
      </div>

      <el-scrollbar ref="scrollRef" class="messages-area">
        <div class="messages-inner">
          <ChatBubble
            v-for="(msg, i) in chatStore.messages"
            :key="i"
            :role="msg.role"
            :content="msg.content"
            :typing="msg.typing"
            @typing-done="msg.typing = false"
          />
          <!-- 等待人工坐席接入状态提示 -->
          <div v-if="waitingMessage" class="waiting-status">
            <el-alert :title="waitingMessage" type="info" :closable="false" show-icon />
          </div>
          <el-empty v-if="!chatStore.messages.length" description="开始新的对话吧" :image-size="80" />
        </div>
      </el-scrollbar>

      <MessageInput :disabled="chatStore.isStreaming" @send="sendMessage" />
    </main>

    <SatisfactionDialog
      v-model="showSatisfaction"
      @submit="handleFeedback"
      @skip="showSatisfaction = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useChatStore } from '@/store/index'
import { streamChat, getSessions, getSession, closeSession, submitFeedback } from '@/api/chat'
import { createEventSource } from '@/utils/sse'
import SessionList from '@/components/SessionList.vue'
import ChatBubble from '@/components/ChatBubble.vue'
import MessageInput from '@/components/MessageInput.vue'
import SatisfactionDialog from '@/components/SatisfactionDialog.vue'

const chatStore = useChatStore()
const scrollRef = ref(null)
const showSatisfaction = ref(false)
const waitingMessage = ref('')
let abortController = null
let waitingEventSource = null

const currentSession = computed(() => chatStore.currentSession)

async function loadSessions() {
  try {
    const data = await getSessions()
    chatStore.setSessions(Array.isArray(data) ? data : data.content || [])
  } catch { /* ignore */ }
}

async function selectSession(sessionId) {
  // 切换会话前，中止正在进行的 SSE 流
  abortActiveStream()
  chatStore.setCurrentSession(sessionId)
  stopWaitingStatus()
  try {
    const data = await getSession(sessionId)
    chatStore.setMessages(data.messages || [])
    scrollToBottom()
    // If session is in TRANSFERRED status, start listening for waiting updates
    if (data.status === 'TRANSFERRED') {
      startWaitingStatus(sessionId)
    }
  } catch { /* ignore */ }
}

function startNewSession() {
  abortActiveStream()
  stopWaitingStatus()
  chatStore.clearChat()
}

function onSessionDeleted(sessionId) {
  if (chatStore.currentSessionId === sessionId) {
    abortActiveStream()
    stopWaitingStatus()
    chatStore.clearChat()
  }
}

/** 中止正在进行的 SSE 流，重置 streaming 状态 */
function abortActiveStream() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  chatStore.setStreaming(false)
}

/**
 * Start listening for waiting status SSE events when session is transferred to human agent.
 */
function startWaitingStatus(sessionId) {
  stopWaitingStatus()
  waitingMessage.value = '正在为您转接人工客服，请稍候...'
  waitingEventSource = createEventSource(
    `/api/v1/chat/sessions/${sessionId}/waiting`,
    {
      onMessage(content) {
        waitingMessage.value = content
        scrollToBottom()
      },
      onDone() {
        waitingMessage.value = ''
        waitingEventSource = null
      },
      onError() {
        waitingMessage.value = ''
        waitingEventSource = null
      }
    }
  )
}

/**
 * Stop listening for waiting status events.
 */
function stopWaitingStatus() {
  waitingMessage.value = ''
  if (waitingEventSource) {
    waitingEventSource.close()
    waitingEventSource = null
  }
}

async function sendMessage(text) {
  // Add user message
  chatStore.addMessage({ role: 'USER', content: text })
  scrollToBottom()

  chatStore.setStreaming(true)
  const sessionId = chatStore.currentSessionId

  abortController = streamChat(sessionId, text, {
    onMessage(content) {
      chatStore.appendToLastMessage(content)
      scrollToBottom()
    },
    onDone(data) {
      chatStore.setStreaming(false)
      // If server returned a new sessionId, update it
      if (data?.sessionId && !chatStore.currentSessionId) {
        chatStore.setCurrentSession(data.sessionId)
        loadSessions()
      }
      scrollToBottom()
    },
    onError() {
      chatStore.setStreaming(false)
      chatStore.addMessage({ role: 'ASSISTANT', content: '抱歉，服务暂时不可用，请稍后重试。' })
      scrollToBottom()
    }
  })
}

async function handleCloseSession() {
  const sid = chatStore.currentSessionId
  if (!sid) return
  try {
    await closeSession(sid)
    showSatisfaction.value = true
    await loadSessions()
  } catch { /* ignore */ }
}

async function handleFeedback({ score, comment }) {
  const sid = chatStore.currentSessionId
  if (!sid) return
  try {
    await submitFeedback(sid, score, comment)
  } catch { /* ignore */ }
  showSatisfaction.value = false
}

function scrollToBottom() {
  nextTick(() => {
    const wrap = scrollRef.value?.$el?.querySelector('.el-scrollbar__wrap')
    if (wrap) wrap.scrollTop = wrap.scrollHeight
  })
}

onMounted(loadSessions)

onUnmounted(() => {
  abortActiveStream()
  stopWaitingStatus()
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: 100%;
}
.sidebar {
  width: 280px;
  border-right: 1px solid #ebeef5;
  flex-shrink: 0;
  background: #fafafa;
}
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}
.messages-area {
  flex: 1;
}
.messages-inner {
  padding: 16px;
}
.waiting-status {
  margin: 8px 0;
}

@media (max-width: 768px) {
  .sidebar { width: 200px; }
}
</style>
