<template>
  <div class="bubble-row" :class="{ 'is-user': isUser }">
    <div class="avatar">
      <el-avatar :size="36" :style="{ background: isUser ? '#409eff' : '#67c23a' }">
        {{ isUser ? '我' : 'AI' }}
      </el-avatar>
    </div>
    <div class="bubble" :class="{ user: isUser, assistant: !isUser }">
      <TypingEffect v-if="typing" :text="content" @done="$emit('typingDone')" />
      <span v-else>{{ content }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import TypingEffect from './TypingEffect.vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, default: '' },
  typing: { type: Boolean, default: false }
})

defineEmits(['typingDone'])

const isUser = computed(() => props.role === 'USER')
</script>

<style scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 8px;
}
.bubble-row.is-user {
  flex-direction: row-reverse;
}
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble.user {
  background: #ecf5ff;
  color: #303133;
}
.bubble.assistant {
  background: #f0f9eb;
  color: #303133;
}
</style>
