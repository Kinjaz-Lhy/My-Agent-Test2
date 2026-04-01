<template>
  <!-- Public pages (login, error) render without layout -->
  <router-view v-if="isPublicRoute" />

  <!-- Authenticated layout with sidebar -->
  <el-container v-else class="app-layout">
    <el-aside width="200px" class="app-sidebar">
      <div class="logo">财务AI客服</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/index'
import { getMenusByRole } from '@/router/index'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isPublicRoute = computed(() => route.meta.public === true)

const menus = computed(() => getMenusByRole(userStore.roles))

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
.app-layout {
  height: 100%;
}
.app-sidebar {
  background: #304156;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.app-sidebar .el-menu {
  border-right: none;
  flex: 1;
}
.logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.sidebar-footer {
  padding: 12px;
  text-align: center;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.app-main {
  padding: 0;
  overflow: auto;
}
</style>
