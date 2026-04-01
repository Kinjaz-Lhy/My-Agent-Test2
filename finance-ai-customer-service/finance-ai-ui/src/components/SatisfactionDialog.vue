<template>
  <el-dialog v-model="visible" title="服务满意度评价" width="420px" :close-on-click-modal="false">
    <div class="satisfaction-form">
      <p>请对本次服务进行评价：</p>
      <div class="rating-row">
        <el-rate v-model="score" :max="5" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
      </div>
      <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入您的反馈意见（选填）" />
    </div>
    <template #footer>
      <el-button @click="handleSkip">跳过</el-button>
      <el-button type="primary" :disabled="!score" @click="handleSubmit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'submit', 'skip'])

const visible = ref(props.modelValue)
const score = ref(0)
const comment = ref('')

watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

function handleSubmit() {
  emit('submit', { score: score.value, comment: comment.value })
  reset()
}

function handleSkip() {
  emit('skip')
  reset()
}

function reset() {
  visible.value = false
  score.value = 0
  comment.value = ''
}
</script>

<style scoped>
.satisfaction-form p {
  margin-bottom: 12px;
  color: #606266;
}
.rating-row {
  margin-bottom: 16px;
}
</style>
