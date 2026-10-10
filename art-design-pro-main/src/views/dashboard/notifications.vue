<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>消息通知</h2>
      <div class="header-actions">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0">
          <el-button @click="loadNotifications">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-badge>
        <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">
          全部已读
        </el-button>
      </div>
    </div>

    <div class="notifications-container">
      <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
      
      <div v-loading="loading" class="notifications-list">
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item"
          :class="{ 'unread': !notification.isRead }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-header">
            <div class="notification-info">
              <span class="notification-type">{{ getNotificationTypeText(notification.notificationType) }}</span>
              <span class="notification-time">{{ formatDateTime(notification.createTime) }}</span>
            </div>
            <el-tag v-if="!notification.isRead" type="danger" size="small">未读</el-tag>
          </div>
          
          <h3 class="notification-title">{{ notification.title }}</h3>
          <p class="notification-content">{{ notification.content }}</p>
          
          <div class="notification-resident">
            <el-icon><User /></el-icon>
            <span>老人：{{ notification.residentName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="通知详情"
      width="800px"
    >
      <div v-if="selectedNotification">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="通知类型">
            {{ getNotificationTypeText(selectedNotification.notificationType) }}
          </el-descriptions-item>
          <el-descriptions-item label="老人姓名">{{ selectedNotification.residentName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(selectedNotification.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="阅读状态">
            <el-tag :type="selectedNotification.isRead ? 'success' : 'danger'">
              {{ selectedNotification.isRead ? '已读' : '未读' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">通知内容</el-divider>
        <div class="notification-detail-content">
          <p>{{ selectedNotification.content }}</p>
        </div>

        <el-divider content-position="left" v-if="selectedNotification.extraData">养老照护风险提示详情</el-divider>
        <div v-if="selectedNotification.extraData" class="prediction-details">
          <div 
            v-for="(prediction, index) in parsePredictions(selectedNotification.extraData)" 
            :key="index"
            class="prediction-card"
          >
            <h4>{{ prediction.condition }}</h4>
            <p><strong>风险评分：</strong>{{ prediction.probability || prediction.score }}</p>
            <p><strong>关注周期：</strong>{{ prediction.timeframe || '持续观察' }}</p>
            <div v-if="prediction.symptoms">
              <strong>可能症状：</strong>
              <ul>
                <li v-for="symptom in prediction.symptoms" :key="symptom">{{ symptom }}</li>
              </ul>
            </div>
            <div v-if="prediction.interventions">
              <strong>建议措施：</strong>
              <ul>
                <li v-for="intervention in prediction.interventions" :key="intervention">{{ intervention }}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToResident" v-if="selectedNotification">
          查看老人档案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import api from '@/utils/http'

const router = useRouter()

interface PredictionItem {
  condition: string
  probability?: string
  score?: string | number
  timeframe?: string
  symptoms?: string[]
  interventions?: string[]
}

interface NotificationItem {
  id: number
  residentId: number
  residentName?: string
  notificationType: string
  title: string
  content: string
  createTime?: string
  isRead: boolean
  extraData?: string
}

const loading = ref(false)
const detailVisible = ref(false)
const notifications = ref<NotificationItem[]>([])
const selectedNotification = ref<NotificationItem | null>(null)
const unreadCount = ref(0)

onMounted(() => {
  loadNotifications()
})

const loadNotifications = async () => {
  loading.value = true
  try {
    // 获取未读通知数量
    const unreadResponse = await api.get<{ unreadCount: number }>({
      url: '/api/notifications/unread'
    })
    unreadCount.value = unreadResponse.unreadCount || 0

    // 获取所有通知
    const allResponse = await api.get<NotificationItem[]>({
      url: '/api/notifications/all',
      params: { limit: 50 }
    })
    notifications.value = allResponse || []
  } catch (error) {
    ElMessage.error('加载通知失败')
    console.error('加载通知失败:', error)
  } finally {
    loading.value = false
  }
}

const handleNotificationClick = async (notification: NotificationItem) => {
  selectedNotification.value = notification
  detailVisible.value = true
  
  // 如果是未读通知，标记为已读
  if (!notification.isRead) {
    try {
      await api.post({
        url: `/api/notifications/read/${notification.id}`
      })
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

const markAllAsRead = async () => {
  try {
    await api.post({
      url: '/api/notifications/read-all'
    })
    
    // 更新本地状态
    notifications.value.forEach(notification => {
      notification.isRead = true
    })
    unreadCount.value = 0
    
    ElMessage.success('全部标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('标记全部已读失败:', error)
  }
}

const goToResident = () => {
  if (selectedNotification.value) {
    router.push(`/resident/detail/${selectedNotification.value.residentId}`)
    detailVisible.value = false
  }
}

const getNotificationTypeText = (type: string) => {
  const types: Record<string, string> = {
    'HEALTH_PREDICTION': '照护风险提示',
    'RISK_ALERT': '风险预警',
    'SYSTEM': '系统通知'
  }
  return types[type] || type
}

const formatDateTime = (dateTime?: string) => {
  return dateTime ? dayjs(dateTime).format('YYYY-MM-DD HH:mm') : '-'
}

const parsePredictions = (extraData: string): PredictionItem[] => {
  try {
    return JSON.parse(extraData) || []
  } catch {
    return []
  }
}
</script>

<style scoped>
.notifications-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #262626;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.notification-item.unread {
  border-left: 4px solid #1890ff;
  background: #f6ffed;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notification-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.notification-type {
  background: #e6f7ff;
  color: #1890ff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.notification-time {
  color: #8c8c8c;
  font-size: 14px;
}

.notification-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.notification-content {
  margin: 0 0 12px 0;
  color: #595959;
  line-height: 1.5;
}

.notification-resident {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8c8c8c;
  font-size: 14px;
}

.notification-detail-content {
  background: #fafafa;
  padding: 16px;
  border-radius: 6px;
  margin: 16px 0;
}

.prediction-details {
  max-height: 400px;
  overflow-y: auto;
}

.prediction-card {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 12px;
}

.prediction-card h4 {
  margin: 0 0 12px 0;
  color: #389e0d;
}

.prediction-card p {
  margin: 8px 0;
  color: #595959;
}

.prediction-card ul {
  margin: 8px 0;
  padding-left: 20px;
}

.prediction-card li {
  color: #595959;
  margin: 4px 0;
}
</style>
