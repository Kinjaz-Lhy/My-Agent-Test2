<template>
  <div class="hot-topics">
    <h3>热点问题统计</h3>
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="时间范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-card>
      <div ref="chartRef" style="height:400px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getHotTopics } from '@/api/admin'

const dateRange = ref(null)
const chartRef = ref(null)
let chart = null

async function loadData() {
  try {
    const params = {
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    const data = await getHotTopics(params)
    const items = Array.isArray(data) ? data : data?.items || []
    renderChart(items)
  } catch { /* ignore */ }
}

function renderChart(items) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const top20 = items.slice(0, 20)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: top20.map(i => i.topic || i.name), axisLabel: { rotate: 45, fontSize: 11 } },
    yAxis: { type: 'value', name: '出现次数' },
    series: [{
      type: 'bar',
      data: top20.map(i => i.count || i.value),
      itemStyle: { color: '#409eff' },
      label: { show: true, position: 'top' }
    }]
  })
}

function handleResize() { chart?.resize() }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.hot-topics { padding: 20px; }
.filter-bar { margin-bottom: 16px; }
</style>
