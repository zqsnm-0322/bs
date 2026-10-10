<template>
  <div class="art-card h-82 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>护理区分布</h4>
        <p>各护理区在住老人占比</p>
      </div>
    </div>
    <ArtRingChart
      height="calc(100% - 30px)"
      :data="chartData"
      :radius="['40%', '70%']"
      :showLegend="true"
      legendPosition="right"
      centerText="护理区分布"
      :showLabel="false"
    />
  </div>
</template>

<script setup lang="ts">
  import { fetchDashboardStats } from '@/api/dashboard'

  defineOptions({ name: 'RiskDistribution' })

  const chartData = ref([
    { value: 0, name: '自理区' },
    { value: 0, name: '半护区' },
    { value: 0, name: '全护区' },
    { value: 0, name: '失智区' }
  ])

  // 加载风险分布数据
  const loadRiskDistribution = async () => {
    try {
      const stats = await fetchDashboardStats()
      if (stats.zoneDistribution && Array.isArray(stats.zoneDistribution)) {
        chartData.value = stats.zoneDistribution
      }
    } catch (error) {
      console.error('加载风险分布数据失败:', error)
    }
  }

  onMounted(() => {
    loadRiskDistribution()
  })
</script>
