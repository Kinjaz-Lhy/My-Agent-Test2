<template>
  <div class="log-query">
    <h3>对话日志查询</h3>

    <!-- Filter bar -->
    <el-form :inline="true" :model="filters" class="filter-bar">
      <el-form-item label="时间范围">
        <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="用户">
        <el-input v-model="filters.employeeId" placeholder="员工ID" clearable />
      </el-form-item>
      <el-form-item label="意图分类">
        <el-select v-model="filters.intent" placeholder="全部" clearable :teleported="true" style="width: 160px">
          <el-option label="闲聊" value="CHAT" />
          <el-option label="报销查询" value="EXPENSE_QUERY" />
          <el-option label="发票验真" value="INVOICE_VERIFY" />
          <el-option label="薪资查询" value="SALARY_QUERY" />
          <el-option label="供应商查询" value="SUPPLIER_QUERY" />
          <el-option label="流程引导" value="GUIDE" />
          <el-option label="人工转接" value="HANDOFF" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <!-- Log table -->
    <el-table :data="logs" stripe border style="width:100%" v-loading="loading">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <p><strong>请求：</strong>{{ row.requestContent }}</p>
            <p><strong>响应：</strong>{{ row.responseContent }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="logId" label="日志ID" width="180" show-overflow-tooltip />
      <el-table-column prop="employeeId" label="员工ID" width="120" />
      <el-table-column prop="action" label="操作类型" width="120">
        <template #default="{ row }">
          {{ actionLabel[row.action] || row.action }}
        </template>
      </el-table-column>
      <el-table-column prop="sessionId" label="会话ID" width="180" show-overflow-tooltip />
      <el-table-column prop="timestamp" label="时间" width="180" />
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { queryLogs } from '@/api/admin'

const filters = ref({ dateRange: null, employeeId: '', intent: '' })
const actionLabel = {
  CHAT: '闲聊',
  EXPENSE_QUERY: '报销查询',
  INVOICE_VERIFY: '发票验真',
  SALARY_QUERY: '薪资查询',
  SUPPLIER_QUERY: '供应商查询',
  GUIDE: '流程引导',
  HANDOFF: '人工转接'
}
const logs = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

async function handleSearch() {
  currentPage.value = 1
  await fetchLogs()
}

async function handlePageChange(page) {
  currentPage.value = page
  await fetchLogs()
}

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      employeeId: filters.value.employeeId || undefined,
      intent: filters.value.intent || undefined,
      startTime: filters.value.dateRange?.[0] ? filters.value.dateRange[0] + ' 00:00:00' : undefined,
      endTime: filters.value.dateRange?.[1] ? filters.value.dateRange[1] + ' 23:59:59' : undefined
    }
    const data = await queryLogs(params)
    logs.value = Array.isArray(data) ? data : (data.content || data.items || [])
    total.value = Array.isArray(data) ? data.length : (data.totalElements || data.total || 0)
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.log-query { padding: 20px; }
.filter-bar { margin-bottom: 16px; }
.expand-content { padding: 12px; line-height: 1.8; }
.expand-content p { margin: 4px 0; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
