<template>
  <div class="care-task-tab">
    <div class="header">
      <span>护理任务</span>
      <ElButton type="primary" size="small" icon="Refresh" :loading="loading" @click="loadTasks"
        >刷新</ElButton
      >
    </div>
    <ElTable :data="taskList" border stripe>
      <ElTableColumn prop="title" label="任务名称" width="200" />
      <ElTableColumn prop="time" label="时间" width="150" />
      <ElTableColumn prop="completed" label="状态" width="80">
        <template #default="{ row }">
          <ElTag :type="row.completed ? 'success' : 'warning'">
            {{ row.completed ? '已完成' : '待完成' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="completeTime" label="完成时间" width="160" />
      <ElTableColumn label="操作" width="100">
        <template #default="{ row }">
          <ElButton v-if="!row.completed" type="success" link @click="handleComplete(row)"
            >标记完成</ElButton
          >
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import { fetchResidentCareTasks, completeCareTask } from '@/api/care-plan'
  import { useUserStore } from '@/store/modules/user'

  const props = defineProps<{ residentId: number }>()
  const taskList = ref<any[]>([])
  const loading = ref(false)
  const userStore = useUserStore()

  watch(
    () => props.residentId,
    (id) => {
      if (id) {
        loadTasks()
      }
    },
    { immediate: true }
  )

  async function loadTasks() {
    if (!props.residentId) return
    loading.value = true
    try {
      taskList.value = await fetchResidentCareTasks(props.residentId)
    } catch {
      ElMessage.error('获取护理任务失败')
    } finally {
      loading.value = false
    }
  }

  async function handleComplete(row: any) {
    try {
      const user = userStore.getUserInfo
      await completeCareTask(row.id, {
        userId: Number(user.userId || 1),
        userName: user.realName || user.userName || '当前用户'
      })
      ElMessage.success('护理任务已完成')
      loadTasks()
    } catch {
      ElMessage.error('更新护理任务失败')
    }
  }
</script>

<style scoped>
  .care-task-tab {
    padding: 10px;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
</style>
