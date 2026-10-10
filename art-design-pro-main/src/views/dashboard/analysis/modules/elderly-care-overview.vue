<template>
  <div class="art-card h-82 p-5 mb-5 overflow-hidden max-lg:h-auto max-sm:mb-4">
    <div class="art-card-header pr-0">
      <div class="title">
        <h4>全院概览</h4>
        <p>入住、离院与床位实时数据</p>
      </div>
      <div class="flex-cc h-7.5 min-w-17 border border-g-300 rounded-lg text-g-500 c-p">
        <ArtSvgIcon icon="ri:arrow-up-line" class="text-base mr-1.5" />
        <span class="text-xs">导出</span>
      </div>
    </div>

    <div class="mt-2">
      <ElRow :gutter="20">
        <ElCol :span="6" :xs="24" v-for="(item, index) in dataList" :key="index">
          <div
            class="flex px-5 flex-col justify-center h-55 border border-g-300/85 rounded-xl max-lg:mb-4 max-sm:flex-row max-sm:justify-between max-sm:items-center max-sm:h-40"
          >
            <div class="size-12 rounded-lg flex-cc bg-theme/10">
              <ArtSvgIcon :icon="item.icon" class="text-xl text-theme" />
            </div>

            <div class="max-sm:ml-4 mt-3.5 max-sm:mt-0 max-sm:text-end">
              <ArtCountTo class="text-2xl font-medium" :target="item.value" :duration="1500" />
              <p class="mt-2 text-base text-g-600 max-sm:mt-1">{{ item.label }}</p>
              <small class="text-g-500 mt-1 max-sm:mt-0.5">
                {{ item.hint }}
              </small>
            </div>
          </div>
        </ElCol>
      </ElRow>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { fetchDashboardStats } from '@/api/dashboard'

  interface DataItem {
    label: string
    value: number
    hint: string
    icon: string
  }

  const dataList = ref<DataItem[]>([
    {
      label: '在住老人',
      value: 0,
      hint: '当前在住总数',
      icon: 'ri:user-heart-line'
    },
    {
      label: '本月新增入住',
      value: 0,
      hint: '本月入住登记',
      icon: 'ri:user-add-line'
    },
    {
      label: '本月离院',
      value: 0,
      hint: '本月离院人数',
      icon: 'ri:user-unfollow-line'
    },
    {
      label: '床位使用率',
      value: 0,
      hint: '占全部床位百分比',
      icon: 'ri:hotel-bed-line'
    }
  ])

  // 加载仪表盘数据
  const loadDashboardData = async () => {
    try {
      const stats = await fetchDashboardStats()
      dataList.value[0].value = stats.residentCount || 0
      dataList.value[1].value = stats.newAdmissions || 0
      dataList.value[2].value = stats.leavingCount || 0
      dataList.value[3].value = stats.bedOccupancyRate || 0
    } catch (error) {
      console.error('加载仪表盘数据失败:', error)
    }
  }

  onMounted(() => {
    loadDashboardData()
  })
</script>
