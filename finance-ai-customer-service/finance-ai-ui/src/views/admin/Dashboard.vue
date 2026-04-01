<template>
  <div class="dashboard">
    <h3>运营指标看板</h3>

    <!-- Metric cards -->
    <el-row :gutter="16" class="metric-cards">
      <el-col :xs="12" :sm="6" v-for="card in metricCards" :key="card.label">
        <el-card shadow="hover">
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :sm="12">
        <el-card>
          <template #header>热点问题 TOP10</template>
          <div ref="hotTopicsChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card>
          <template #header>满意度趋势</template>
          <div ref="satisfactionChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getMetrics, getHotTopics } from '@/api/admin'

const metrics = ref({})
const hotTopicsChartRef = ref(null)
const satisfactionChartRef = ref(null)
let hotChart = null
let satChart = null

const metricCards = computed(() => [
  { label: '今日服务量', value: metrics.value.totalSessions ?? '-' },
  { label: '自助解决率', value: metrics.value.selfResolveRate != null ? `${(metrics.value.selfResolveRate * 100).toFixed(1)}%` : '-' },
  { label: '人工转接率', value: metrics.value.handoffRate != null ? `${(metrics.value.handoffRate * 100).toFixed(1)}%` : '-' },
  { label: '平均响应时间', value: metrics.value.avgResponseTimeMs != null ? `${(metrics.value.avgResponseTimeMs / 1000).toFixed(1)}s` : '-' }
])

async function loadData() {
  try {
    const data = await getMetrics()
    metrics.value = data || {}
    renderSatisfactionChart(data.satisfactionTrend || [])
  } catch { /* ignore */ }

  try {
    const topics = await getHotTopics()
    renderHotTopicsChart(Array.isArray(topics) ? topics : topics?.items || [])
  } catch { /* ignore */ }
}

function renderHotTopicsChart(items) {
  if (!hotTopicsChartRef.value) return
  hotChart = echarts.init(hotTopicsChartRef.value)
  const top10 = items.slice(0, 10)
  hotChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: top10.map(i => i.topic || i.name), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: top10.map(i => i.count || i.value), itemStyle: { color: '#409eff' } }]
  })
}

function renderSatisfactionChart(trend) {
  if (!satisfactionChartRef.value) return
  satChart = echarts.init(satisfactionChartRef.value)
  satChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(t => t.date) },
    yAxis: { type: 'value', min: 0, max: 5 },
    series: [{ type: 'line', data: trend.map(t => t.score), smooth: true, itemStyle: { color: '#67c23a' } }]
  })
}

function handleResize() {
  hotChart?.resize()
  satChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  hotChart?.dispose()
  satChart?.dispose()
})
</script>

<style scoped>
.dashboard { padding: 20px; }
.metric-cards .el-card { text-align: center; }
.metric-value { font-size: 28px; font-weight: 700; color: #409eff; }
.metric-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>
