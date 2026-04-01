<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>财务共享智能客服</h2>
      <p>本地开发模式 — 输入任意用户名即可登录</p>
      <el-form v-if="showManualLogin" :model="form" @submit.prevent="handleManualLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="form.role" placeholder="选择角色" style="width:100%">
            <el-option label="员工 (EMPLOYEE)" value="EMPLOYEE" />
            <el-option label="运营人员 (OPERATOR)" value="OPERATOR" />
            <el-option label="审计人员 (AUDITOR)" value="AUDITOR" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <el-button v-else type="primary" size="large" :loading="loading" @click="handleSSOLogin">SSO 登录</el-button>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/index'
import { ssoCallback } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')
const showManualLogin = ref(true)
const form = ref({ username: '', role: 'EMPLOYEE' })

/** Redirect to SSO provider */
function handleSSOLogin() {
  const redirect = route.query.redirect || '/'
  window.location.href = `/api/v1/auth/sso/login?redirect=${encodeURIComponent(redirect)}`
}

/** Handle SSO callback code in URL query */
async function handleSSOCallback(code) {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await ssoCallback(code)
    userStore.login(data.token)
    const redirect = route.query.redirect || '/chat'
    router.replace(redirect)
  } catch {
    errorMsg.value = '登录失败，请重试'
  } finally {
    loading.value = false
  }
}

/** Manual login fallback (for dev/testing) */
async function handleManualLogin() {
  loading.value = true
  errorMsg.value = ''
  try {
    if (!form.value.username.trim()) {
      errorMsg.value = '请输入用户名'
      loading.value = false
      return
    }
    const mockPayload = btoa(JSON.stringify({
      sub: form.value.username,
      username: form.value.username,
      roles: [form.value.role],
      departmentId: 'DEPT-001',
      exp: Math.floor(Date.now() / 1000) + 3600
    }))
    const mockToken = `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.${mockPayload}.mock-signature`
    userStore.login(mockToken)
    const redirect = route.query.redirect || '/chat'
    router.replace(redirect)
  } catch {
    errorMsg.value = '登录失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // If already logged in, redirect
  if (userStore.isLoggedIn) {
    router.replace(route.query.redirect || '/chat')
    return
  }
  // Check for SSO callback code
  const code = route.query.code
  if (code) {
    handleSSOCallback(code)
  }
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  text-align: center;
  border-radius: 8px;
}
.login-card h2 {
  margin-bottom: 8px;
  color: #303133;
}
.login-card p {
  color: #909399;
  margin-bottom: 24px;
}
.error-msg {
  color: #f56c6c;
  margin-top: 12px;
}
</style>
