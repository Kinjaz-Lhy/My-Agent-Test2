<template>
  <div class="audit-logs">
    <h3>审计日志</h3>

    <!-- Filter bar -->
    <el-form :inline="true" :model="filters" class="filter-bar">
      <el-form-item label="时间范围">
        <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="员工ID">
        <el-input v-model="filters.employeeId" placeholder="员工ID" clearable />
      </el-form-item>
      <el-form-item label="会话ID">
        <el-input v-model="filters.sessionId" placeholder="会话ID" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleExport">导出</el-button>
      </el-form-item>
    </el-form>

    <!-- Audit log table -->
    <el-table :data="logs" stripe border style="width:100%" v-loading="loading">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <p><strong>请求内容：</strong>{{ row.requestContent }}</p>
            <p><strong>脱敏响应：</strong>{{ row.maskedResponseContent || row.responseContent }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="logId" label="日志ID" width="200" show-overflow-tooltip />
      <el-table-column prop="employeeId" label="员工ID" width="120" />
      <el-table-column prop="action" label="操作类型" width="120">
        <template #default="{ row }">
          <el-tag :type="actionTagType[row.action] || 'info'" size="small">
            {{ actionLabel[row.action] || row.action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sessionId" label="会话ID" width="200" show-overflow-tooltip />
      <el-table-column prop="responseTimeMs" label="响应耗时" width="110">
        <template #default="{ row }">
          {{ row.responseTimeMs > 0 ? (row.responseTimeMs / 1000).toFixed(1) + 's' : '-' }}
        </template>
      </el-table-column>
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
import { queryAuditLogs } from '@/api/audit'

const filters = ref({ dateRange: null, employeeId: '', sessionId: '' })
const actionLabel = {
  CHAT: '闲聊',
  EXPENSE_QUERY: '报销查询',
  INVOICE_VERIFY: '发票验真',
  SALARY_QUERY: '薪资查询',
  SUPPLIER_QUERY: '供应商查询',
  GUIDE: '流程引导',
  HANDOFF: '人工转接'
}
const actionTagType = {
  CHAT: 'info',
  EXPENSE_QUERY: '',
  INVOICE_VERIFY: 'warning',
  SALARY_QUERY: 'success',
  SUPPLIER_QUERY: '',
  GUIDE: 'success',
  HANDOFF: 'danger'
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
      sessionId: filters.value.sessionId || undefined,
      startTime: filters.value.dateRange?.[0] ? filters.value.dateRange[0] + ' 00:00:00' : undefined,
      endTime: filters.value.dateRange?.[1] ? filters.value.dateRange[1] + ' 23:59:59' : undefined
    }
    const data = await queryAuditLogs(params)
    logs.value = Array.isArray(data) ? data : (data.content || data.items || [])
    total.value = Array.isArray(data) ? data.length : (data.totalElements || data.total || 0)
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

function handleExport() {
  if (!logs.value.length) return
  const headers = ['日志ID', '员工ID', '操作类型', '会话ID', '请求内容', '脱敏响应', '响应耗时(ms)', '时间']
  const rows = logs.value.map(r => [
    r.logId, r.employeeId, actionLabel[r.action] || r.action,
    r.sessionId, r.requestContent, r.maskedResponseContent || r.responseContent,
    r.responseTimeMs, r.timestamp
  ])
  const csv = [headers, ...rows].map(r => r.map(c => `"${(c ?? '').toString().replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
}
</script>

<style scoped>
.audit-logs { padding: 20px; }
.filter-bar { margin-bottom: 16px; }
.expand-content { padding: 12px; line-height: 1.8; }
.expand-content p { margin: 4px 0; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
