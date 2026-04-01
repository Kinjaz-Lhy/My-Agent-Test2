<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面未找到</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}
.error-page h1 {
  font-size: 72px;
  color: #f56c6c;
  margin-bottom: 16px;
}
</style>
