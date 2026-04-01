<template>
  <div class="message-input">
    <el-input
      v-model="text"
      type="textarea"
      :rows="2"
      placeholder="输入您的问题..."
      :disabled="disabled"
      @keydown.enter.exact.prevent="handleSend"
      resize="none"
    />
    <el-button type="primary" :disabled="disabled || !text.trim()" @click="handleSend">
      发送
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])
const text = ref('')

function handleSend() {
  const msg = text.value.trim()
  if (!msg) return
  emit('send', msg)
  text.value = ''
}
</script>

<style scoped>
.message-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}
.message-input .el-input {
  flex: 1;
}
</style>
