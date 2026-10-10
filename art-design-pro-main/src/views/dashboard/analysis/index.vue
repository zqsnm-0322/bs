<!-- 分析页页面 -->
<template>
  <div>
    <ElRow :gutter="20">
      <ElCol :span="24">
        <ElderlyCareOverview />
      </ElCol>
    </ElRow>

    <ElRow :gutter="20">
      <ElCol :xl="8" :lg="10" :xs="24">
        <RiskDistribution />
      </ElCol>
      <ElCol :xl="16" :lg="14" :xs="24">
        <RiskTrend />
      </ElCol>
    </ElRow>
  </div>
</template>

<script setup lang="ts">
  import ElderlyCareOverview from './modules/elderly-care-overview.vue'
  import RiskDistribution from './modules/risk-distribution.vue'
  import RiskTrend from './modules/risk-trend.vue'

  defineOptions({ name: 'Analysis' })
</script>
