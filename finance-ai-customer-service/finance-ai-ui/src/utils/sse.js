import { getToken } from './auth'

/**
 * SSE EventSource wrapper for streaming chat responses.
 * Uses fetch + ReadableStream to support POST requests with auth headers,
 * since native EventSource only supports GET and no custom headers.
 *
 * @param {string} sessionId - The chat session ID
 * @param {string} message - The user message
 * @param {object} callbacks - { onMessage, onDone, onError }
 * @returns {AbortController} controller to abort the stream
 */
export function streamChat(sessionId, message, { onMessage, onDone, onError }) {
  const controller = new AbortController()

  fetch('/api/v1/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`,
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify({ sessionId, message }),
    signal: controller.signal
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      return readSSEStream(response.body, { onMessage, onDone })
    })
    .catch(err => {
      if (err.name !== 'AbortError') {
        onError && onError(err)
      }
    })

  return controller
}

/**
 * Read and parse an SSE stream from a ReadableStream body.
 */
async function readSSEStream(body, { onMessage, onDone }) {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() // keep incomplete line in buffer

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const dataStr = line.slice(5).trim()
        if (!dataStr) continue
        try {
          const data = JSON.parse(dataStr)
          if (data.done) {
            // 最后一个 chunk 也可能有内容，先追加再触发 done
            if (data.content) {
              onMessage && onMessage(data.content)
            }
            onDone && onDone(data)
          } else {
            onMessage && onMessage(data.content)
          }
        } catch {
          // non-JSON data line, treat as plain text content
          onMessage && onMessage(dataStr)
        }
      }
    }
  }
}

/**
 * Simple EventSource wrapper for GET-based SSE endpoints.
 * Useful for endpoints that don't require POST body.
 *
 * @param {string} url - The SSE endpoint URL
 * @param {object} callbacks - { onMessage, onDone, onError }
 * @returns {EventSource}
 */
export function createEventSource(url, { onMessage, onDone, onError }) {
  const eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.done) {
        if (data.content) {
          onMessage && onMessage(data.content)
        }
        eventSource.close()
        onDone && onDone(data)
      } else {
        onMessage && onMessage(data.content)
      }
    } catch {
      onMessage && onMessage(event.data)
    }
  }

  eventSource.onerror = () => {
    eventSource.close()
    onError && onError(new Error('SSE connection error'))
  }

  return eventSource
}
