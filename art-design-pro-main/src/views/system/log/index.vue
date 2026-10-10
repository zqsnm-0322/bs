<!-- 日志监控 -->
<template>
  <div class="log-page art-full-height">
    <ElCard class="art-table-card" shadow="never">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
           <ElButton type="danger" icon="Delete">清空日志</ElButton>
        </template>
      </ArtTableHeader>

      <!-- 表格 -->
      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
      </ArtTable>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { useTable } from '@/hooks/core/useTable'
  import { ElTag } from 'element-plus'

  defineOptions({ name: 'SystemLog' })

  // 模拟数据
  const mockData = [
    { id: 1, user: 'admin', ip: '192.168.1.1', module: '用户管理', action: '新增用户', status: 'success', time: '2023-10-25 10:00:00' },
    { id: 2, user: 'caregiver01', ip: '192.168.1.10', module: '风险评估', action: '提交评估', status: 'success', time: '2023-10-25 09:30:00' },
    { id: 3, user: 'care_manager01', ip: '192.168.1.20', module: '系统登录', action: '登录', status: 'fail', time: '2023-10-25 09:00:00' },
  ]

  const fetchLogList = () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          list: mockData,
          total: mockData.length
        })
      }, 500)
    })
  }

  const { columns, columnChecks, data, loading, pagination, handleSizeChange, handleCurrentChange, refreshData } = useTable({
    core: {
      apiFn: fetchLogList,
      columnsFactory: () => [
        { type: 'index', label: '序号', width: 80 },
        { prop: 'user', label: '操作用户', width: 120 },
        { prop: 'ip', label: 'IP地址', width: 150 },
        { prop: 'module', label: '功能模块', width: 150 },
        { prop: 'action', label: '操作内容', width: 200 },
        {
          prop: 'status',
          label: '状态',
          formatter: (row: any) => {
            const type = row.status === 'success' ? 'success' : 'danger'
            const text = row.status === 'success' ? '成功' : '失败'
            return h(ElTag, { type }, () => text)
          }
        },
        { prop: 'time', label: '操作时间', sortable: true }
      ]
    }
  })
</script>
