<template>
  <div class="error-page">
    <h1>403</h1>
    <p>您没有权限访问此页面</p>
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
  color: #e6a23c;
  margin-bottom: 16px;
}
</style>
