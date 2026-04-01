<template>
  <span>{{ displayedText }}<span v-if="isTyping" class="cursor">|</span></span>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  speed: { type: Number, default: 30 }
})

const emit = defineEmits(['done'])
const displayedText = ref('')
const isTyping = ref(false)
let timer = null
let charIndex = 0

function startTyping(fullText) {
  stopTyping()
  displayedText.value = ''
  charIndex = 0
  isTyping.value = true
  timer = setInterval(() => {
    if (charIndex < fullText.length) {
      displayedText.value += fullText[charIndex]
      charIndex++
    } else {
      stopTyping()
      emit('done')
    }
  }, props.speed)
}

function stopTyping() {
  if (timer) { clearInterval(timer); timer = null }
  isTyping.value = false
}

watch(() => props.text, (val) => {
  if (val) startTyping(val)
}, { immediate: true })

onUnmounted(stopTyping)
</script>

<style scoped>
.cursor {
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}
</style>
