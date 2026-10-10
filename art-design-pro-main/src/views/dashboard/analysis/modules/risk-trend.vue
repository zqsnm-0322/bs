<template>
  <div class="art-card h-82 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>健康等级分布</h4>
        <p>在住老人自理能力等级</p>
      </div>
    </div>
    <ArtLineChart
      height="calc(100% - 30px)"
      :data="chartData"
      :xAxisData="xAxisData"
      :showLegend="true"
      :showAxisLabel="true"
      :showAxisLine="false"
      :showSplitLine="true"
    />
  </div>
</template>

<script setup lang="ts">
  import type { LineDataItem } from '@/types/component/chart'
  import { fetchDashboardStats } from '@/api/dashboard'

  defineOptions({ name: 'RiskTrend' })

  const xAxisData = ref<string[]>([])
  const chartData = ref<LineDataItem[]>([
    {
      name: '老人数量',
      data: []
    }
  ])

  // 加载风险趋势数据
  const loadRiskTrend = async () => {
    try {
      const stats = await fetchDashboardStats()
      if (Array.isArray(stats.healthLevelDistribution)) {
        xAxisData.value = stats.healthLevelDistribution.map((item: any) => item.name)
        chartData.value = [
          {
            name: '老人数量',
            data: stats.healthLevelDistribution.map((item: any) => item.value)
          }
        ]
      }
    } catch (error) {
      console.error('加载风险趋势数据失败:', error)
    }
  }

  onMounted(() => {
    loadRiskTrend()
  })
</script>
