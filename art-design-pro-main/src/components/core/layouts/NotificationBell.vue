<template>
  <div class="notification-bell">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
      <el-button 
        circle 
        @click="goToNotifications"
        :class="{ 'has-notifications': unreadCount > 0 }"
      >
        <el-icon><Bell /></el-icon>
      </el-button>
    </el-badge>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import api from '@/utils/http'

const router = useRouter()
const unreadCount = ref(0)
let intervalId: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadUnreadCount()
  // 每30秒检查一次未读消息
  intervalId = setInterval(loadUnreadCount, 30000)
})

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId)
  }
})

const loadUnreadCount = async () => {
  try {
    const response = await api.get<{ unreadCount: number }>({
      url: '/api/notifications/unread'
    })
    unreadCount.value = response.unreadCount || 0
  } catch (error) {
    console.error('获取未读消息数量失败:', error)
  }
}

const goToNotifications = () => {
  router.push('/dashboard/notifications')
}
</script>

<style scoped>
.notification-bell {
  display: flex;
  align-items: center;
}

.notification-bell .el-button {
  border: none;
  background: transparent;
  color: #595959;
  transition: all 0.3s ease;
}

.notification-bell .el-button:hover {
  background: #f0f0f0;
  color: #1890ff;
}

.notification-bell .el-button.has-notifications {
  color: #1890ff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}
</style>
