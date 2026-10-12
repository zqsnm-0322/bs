<template>
  <div class="statistics-page art-full-height">
    <ElRow :gutter="20">
      <!-- 统计卡片 -->
      <ElCol :span="6" v-for="(item, index) in statCards" :key="index">
        <ElCard shadow="hover" class="mb-5">
          <div class="flex justify-between items-center">
            <div>
              <p class="text-gray-500">{{ item.label }}</p>
              <h2 class="text-2xl font-bold mt-2">{{ item.value }}</h2>
            </div>
            <ArtSvgIcon :icon="item.icon" class="text-4xl text-theme opacity-20" />
          </div>
          <div class="mt-4 flex justify-between text-sm">
            <span class="text-gray-400">较上月</span>
            <span :class="item.trend === 'up' ? 'text-red-500' : 'text-green-500'">
              {{ item.change }} <i :class="item.trend === 'up' ? 'ri-arrow-up-line' : 'ri-arrow-down-line'"></i>
            </span>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard shadow="never" class="art-table-card">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold">工作量统计报表</span>
          <div class="flex items-center gap-3">
            <ElButton 
              type="primary" 
              size="small" 
              @click="initTestData"
              :loading="initLoading"
            >
              刷新数据
            </ElButton>
            <ElDatePicker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
            />
          </div>
        </div>
      </template>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { useTable } from '@/hooks/core/useTable'
  import { fetchStatisticsReport, fetchAssessmentStats } from '@/api/statistics'
  import { initTestData as initTestDataApi } from '@/api/test-data'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useUserStore } from '@/store/modules/user'

  defineOptions({ name: 'StatisticsReport' })

  const dateRange = ref<[Date, Date] | []>([])
  const initLoading = ref(false)
  const statCards = ref([
    { label: '本月评估总数', value: '128', icon: 'ri:bar-chart-box-line', trend: 'up', change: '9.51%' },
    { label: '高风险老人人数', value: '23', icon: 'ri:alarm-warning-line', trend: 'up', change: '2.6%' },
    { label: '护理干预次数', value: '46', icon: 'ri:stethoscope-line', trend: 'down', change: '5.86%' },
    { label: '评估及时率', value: '98.5%', icon: 'ri:time-line', trend: 'up', change: '1.28%' },
  ])

  // 加载统计卡片数据
  const loadStatCards = async () => {
    try {
      const params = dateRange.value.length === 2 ? {
        startDate: dateRange.value[0].toISOString().split('T')[0],
        endDate: dateRange.value[1].toISOString().split('T')[0]
      } : {}

      const res = await fetchStatisticsReport(params)
      
      if (res) {
        statCards.value = [
          { 
            label: '本月评估总数', 
            value: res.totalAssessments?.toString() || '0', 
            icon: 'ri:file-list-3-line', 
            trend: res.assessmentTrend >= 0 ? 'up' : 'down', 
            change: `${Math.abs(res.assessmentTrend || 0)}%` 
          },
          { 
            label: '高风险老人人数', 
            value: res.highRiskResidents?.toString() || '0', 
            icon: 'ri:alert-line', 
            trend: res.highRiskTrend >= 0 ? 'up' : 'down', 
            change: `${Math.abs(res.highRiskTrend || 0)}%` 
          },
          { 
            label: '护理干预次数', 
            value: res.interventionCount?.toString() || '0', 
            icon: 'ri:service-line', 
            trend: res.interventionTrend >= 0 ? 'up' : 'down', 
            change: `${Math.abs(res.interventionTrend || 0)}%` 
          },
          { 
            label: '评估及时率', 
            value: `${res.timelyRate || 0}%`, 
            icon: 'ri:time-line', 
            trend: res.timelyTrend >= 0 ? 'up' : 'down', 
            change: `${Math.abs(res.timelyTrend || 0)}%` 
          },
        ]
      }
    } catch (error) {
      ElMessage.error('加载统计数据失败')
    }
  }

  // 获取评估统计数据 - 从后端API获取真实数据
  const fetchStats = async (params: any) => {
    try {
      const queryParams: any = {
        pageNum: params.current || 1,
        pageSize: params.size || 10
      }
      
      // 如果有日期范围，添加到查询参数
      if (dateRange.value && dateRange.value.length === 2) {
        queryParams.startDate = dateRange.value[0].toISOString().split('T')[0]
        queryParams.endDate = dateRange.value[1].toISOString().split('T')[0]
      }
      
      // 一线照护人员只能查看自己的工作量统计
      const userStore = useUserStore()
      const userInfo = userStore.getUserInfo
      
      if (userInfo.role === 'CARE_MANAGER') {
        queryParams.evaluatorName = userInfo.realName || userInfo.userName
      } else if (userInfo.role === 'CAREGIVER') {
        queryParams.evaluatorName = userInfo.realName || userInfo.userName
      }
      // 管理员可以看到所有统计
      
      const response = await fetchAssessmentStats(queryParams)
      
      return {
        records: response.records || [],
        total: response.total || 0
      }
    } catch (error) {
      ElMessage.error('加载评估统计失败')
      return {
        records: [],
        total: 0
      }
    }
  }

  const { columns, data, loading, pagination, handleSizeChange, handleCurrentChange, getData } = useTable({
    core: {
      apiFn: fetchStats,
      columnsFactory: () => [
        { type: 'index', label: '排名', width: 80 },
        { prop: 'evaluatorName', label: '评估人员' },
        { prop: 'careZone', label: '护理区' },
        { prop: 'assessmentCount', label: '评估次数', sortable: true },
        { prop: 'highRiskCount', label: '发现高危数', sortable: true },
        { prop: 'interventionCount', label: '干预次数', sortable: true },
        { prop: 'timelyRate', label: '评估及时率', sortable: true },
      ]
    }
  })

  // 监听日期范围变化
  watch(dateRange, (newVal) => {
    if (newVal && newVal.length === 2) {
      loadStatCards()
      getData()
    }
  })

  // 刷新数据 - 从后端重新加载
  const initTestData = async () => {
    try {
      initLoading.value = true
      
      // 重新加载统计卡片数据
      await loadStatCards()
      
      // 刷新表格数据
      await getData()
      
      ElMessage.success('数据已刷新！')
      
    } catch (error: any) {
      ElMessage.error('刷新数据失败')
    } finally {
      initLoading.value = false
    }
  }

  // 页面加载时初始化数据
  onMounted(() => {
    loadStatCards()
  })
</script>
