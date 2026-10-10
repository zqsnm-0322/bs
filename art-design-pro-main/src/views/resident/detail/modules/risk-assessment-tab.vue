<template>
  <div class="risk-assessment-tab">
    <!-- 操作栏 -->
    <div class="mb-4">
      <el-button type="primary" @click="performAssessment" :loading="assessing">
        <el-icon><TrendCharts /></el-icon>
        执行评估
      </el-button>
      <el-button type="success" @click="viewPredictions" :disabled="!hasAssessment">
        <el-icon><View /></el-icon>
        健康预测
      </el-button>
      <el-button type="warning" @click="shareReport" :disabled="!hasAssessment" :loading="sharing">
        <el-icon><Share /></el-icon>
        分享报告给老人
      </el-button>
      <el-button @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 风险统计卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="总评估数" :value="statistics.total" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="高风险评估" :value="statistics.highRisk" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="中风险评估" :value="statistics.mediumRisk" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="低风险评估" :value="statistics.lowRisk" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 评估结果列表 -->
    <el-table :data="assessmentList" v-loading="loading" style="width: 100%">
      <el-table-column label="评估时间" width="150" fixed="left">
        <template #default="{ row }">
          {{ formatDateTime(row.assessmentTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="assessmentType" label="评估类型" width="120" />
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getRiskLevelType(row.riskLevel)">
            {{ getRiskLevelText(row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总评分" width="150">
        <template #default="{ row }">
          <el-progress 
            :percentage="Math.min(row.totalScore, 100)" 
            :color="getScoreColor(row.totalScore)"
          />
          <span style="margin-left: 8px">{{ row.totalScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="置信度" width="120">
        <template #default="{ row }">
          <el-progress 
            :percentage="row.confidenceLevel" 
            color="#67c23a"
          />
          <span style="margin-left: 8px">{{ row.confidenceLevel }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="healthHistoryScore" label="健康史" width="100" />
      <el-table-column prop="healthCheckScore" label="健康检查" width="100" />
      <el-table-column prop="vitalSignsScore" label="生命体征" width="100" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">
            详情
          </el-button>
          <el-button type="primary" link size="small" @click="viewTrend(row)">
            趋势
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!loading && assessmentList.length === 0" class="text-center py-8 text-gray-500">
      暂无风险评估记录
    </div>

    <!-- 详情查看对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="风险评估详情"
      width="800px"
    >
      <div v-if="selectedRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评估时间">{{ formatDateTime(selectedRecord.assessmentTime) }}</el-descriptions-item>
          <el-descriptions-item label="评估类型">{{ selectedRecord.assessmentType }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="getRiskLevelType(selectedRecord.riskLevel)">
              {{ getRiskLevelText(selectedRecord.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总评分">{{ selectedRecord.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ selectedRecord.confidenceLevel }}%</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">维度评分</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <span>主要维度</span>
              </template>
              <div class="score-list">
                <div class="score-item">
                  <span>健康史：</span>
                  <span>{{ selectedRecord.healthHistoryScore || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>健康检查：</span>
                  <span>{{ selectedRecord.healthCheckScore || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>生命体征：</span>
                  <span>{{ selectedRecord.vitalSignsScore || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>问卷调查：</span>
                  <span>{{ selectedRecord.questionnaireScore || 0 }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <span>子项评分</span>
              </template>
              <div class="score-list">
                <div class="score-item">
                  <span>跌倒风险：</span>
                  <span>{{ selectedRecord.fallRisk || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>压疮风险：</span>
                  <span>{{ selectedRecord.pressureSoreRisk || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>营养风险：</span>
                  <span>{{ selectedRecord.nutritionRisk || 0 }}</span>
                </div>
                <div class="score-item">
                  <span>认知风险：</span>
                  <span>{{ selectedRecord.cognitiveRisk || 0 }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-divider content-position="left">风险因素和建议</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <span>主要风险因素</span>
              </template>
              <div v-if="selectedRecord.riskFactors">
                <el-tag v-for="factor in parseJsonArray(selectedRecord.riskFactors)" :key="factor" type="danger">
                  {{ factor }}
                </el-tag>
              </div>
              <span v-else>无特殊风险因素</span>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <span>护理建议</span>
              </template>
              <div v-if="selectedRecord.recommendations">
                <p v-for="rec in parseJsonArray(selectedRecord.recommendations)" :key="rec">
                  • {{ rec }}
                </p>
              </div>
              <span v-else>暂无特殊建议</span>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <el-dialog v-model="trendVisible" title="风险评估趋势" width="720px">
      <el-table :data="trendRecords" border stripe>
        <el-table-column label="评估时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.assessmentTime) }}</template>
        </el-table-column>
        <el-table-column prop="assessmentType" label="评估类型" min-width="140" />
        <el-table-column prop="totalScore" label="评分" width="90" />
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="trendRecords.length === 0" description="暂无同类评估记录" />
    </el-dialog>

    <!-- 健康预测对话框 -->
    <el-dialog
      v-model="predictionsVisible"
      title="健康状况预测"
      width="900px"
    >
      <div v-if="predictions.length > 0">
        <el-alert
          title="预测说明"
          description="以下预测基于老人当前的风险评估结果和健康数据生成，仅供照护人员参考"
          type="info"
          show-icon
          :closable="false"
          class="mb-4"
        />
        
        <div class="predictions-header mb-4">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="评估时间">{{ formatDateTime(predictionsData.assessmentTime) }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="getRiskLevelType(predictionsData.riskLevel)">
                {{ getRiskLevelText(predictionsData.riskLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="评估评分">{{ predictionsData.totalScore }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="predictions-list">
          <el-card 
            v-for="(prediction, index) in predictions" 
            :key="index" 
            class="prediction-card mb-4"
            :class="getPredictionCardClass(prediction.severity)"
          >
            <template #header>
              <div class="prediction-header">
                <span class="prediction-title">{{ prediction.condition }}</span>
                <div class="prediction-meta">
                  <el-tag :type="getSeverityType(prediction.severity)" size="small">
                    {{ getSeverityText(prediction.severity) }}
                  </el-tag>
                  <span class="prediction-probability">发生概率: {{ prediction.probability }}</span>
                  <span class="prediction-timeframe">时间窗口: {{ prediction.timeframe }}</span>
                </div>
              </div>
            </template>
            
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="prediction-section">
                  <h4>可能症状</h4>
                  <ul class="symptom-list">
                    <li v-for="symptom in prediction.symptoms" :key="symptom">{{ symptom }}</li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="prediction-section">
                  <h4>建议干预措施</h4>
                  <ul class="intervention-list">
                    <li v-for="intervention in prediction.interventions" :key="intervention">{{ intervention }}</li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="8" v-if="prediction.statisticalBasis">
                <div class="prediction-section">
                  <h4>统计学依据</h4>
                  <p class="statistical-basis">{{ prediction.statisticalBasis }}</p>
                </div>
              </el-col>
            </el-row>
          </el-card>
        </div>

        <div class="predictions-actions mt-4">
          <el-button type="primary" @click="sharePredictionsToResident" :loading="sharingPredictions">
            <el-icon><Share /></el-icon>
            分享给老人
          </el-button>
          <el-button type="success" @click="notifyCareManager" :loading="notifying">
            <el-icon><Message /></el-icon>
            发送给照护主管
          </el-button>
          <el-button @click="exportPredictions">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </div>
      </div>
      
      <div v-else-if="!loadingPredictions" class="text-center py-8 text-gray-500">
        <el-empty description="暂无预测数据，请先进行风险评估" />
      </div>
      
      <div v-if="loadingPredictions" class="text-center py-8">
        <el-icon class="is-loading"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang='ts'>
// @ts-nocheck
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Refresh, View, Message, Download, Loading, Share } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import api from '@/utils/http'

const props = defineProps<{
  residentId: string
}>()

const loading = ref(false)
const assessing = ref(false)
const sharing = ref(false)
const sharingPredictions = ref(false)
const detailVisible = ref(false)
const trendVisible = ref(false)
const predictionsVisible = ref(false)
const loadingPredictions = ref(false)
const notifying = ref(false)
const selectedRecord = ref(null)
const assessmentList = ref([])
const trendRecords = ref([])
const predictions = ref([])
const predictionsData = ref({})
const hasAssessment = ref(false)

const statistics = reactive({
  total: 0,
  highRisk: 0,
  mediumRisk: 0,
  lowRisk: 0
})

onMounted(() => {
  loadData()
  loadStatistics()
})

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.get({
      url: `/api/enhanced-risk/history/${props.residentId}`,
      params: {
        current: 1,
        size: 100
      }
    })
    assessmentList.value = response.records || []
    hasAssessment.value = assessmentList.value.length > 0
  } catch (error) {
    ElMessage.error('加载风险评估失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const response = await api.get({
      url: `/api/enhanced-risk/statistics/${props.residentId}`
    })
    Object.assign(statistics, response)
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const performAssessment = async () => {
  assessing.value = true
  try {
    const response = await api.post({
      url: `/api/enhanced-risk/assess/${props.residentId}`
    })
    ElMessage.success('风险评估完成')
    loadData()
    loadStatistics()
    
    // 更新统计数据
    updateStatistics(response)
  } catch (error) {
    ElMessage.error('风险评估失败：' + (error.response?.data?.message || error.message))
  } finally {
    assessing.value = false
  }
}

const viewDetail = (record) => {
  selectedRecord.value = record
  detailVisible.value = true
}

const viewPredictions = async () => {
  loadingPredictions.value = true
  predictionsVisible.value = true
  
  try {
    const response = await api.get({
      url: `/api/enhanced-risk/predictions/${props.residentId}`
    })
    
    predictions.value = response.predictions || []
    predictionsData.value = {
      assessmentTime: response.assessmentTime,
      riskLevel: response.riskLevel,
      totalScore: response.totalScore,
      confidenceLevel: response.confidenceLevel
    }
  } catch (error) {
    ElMessage.error('加载健康预测失败：' + (error.response?.data?.message || error.message))
    predictions.value = []
  } finally {
    loadingPredictions.value = false
  }
}

const notifyCareManager = async () => {
  notifying.value = true
  
  try {
    await api.post({
      url: `/api/enhanced-risk/predictions/notify/${props.residentId}`,
      params: {
        message: '系统基于老人当前状况预测了可能出现的健康问题，请及时关注并采取相应措施'
      }
    })
    
    ElMessage.success('预测结果已发送给照护主管')
  } catch (error) {
    ElMessage.error('发送失败：' + (error.response?.data?.message || error.message))
  } finally {
    notifying.value = false
  }
}

const exportPredictions = () => {
  // 导出预测报告
  const reportData = {
    residentId: props.residentId,
    assessmentTime: predictionsData.value.assessmentTime,
    riskLevel: predictionsData.value.riskLevel,
    totalScore: predictionsData.value.totalScore,
    predictions: predictions.value
  }
  
  const dataStr = JSON.stringify(reportData, null, 2)
  const dataBlob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(dataBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = `健康预测报告_${props.residentId}_${dayjs().format('YYYYMMDD_HHmmss')}.json`
  link.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('报告已导出')
}

const shareReport = async () => {
  sharing.value = true
  
  try {
    const response = await api.post({
      url: `/api/enhanced-risk/share-report/${props.residentId}`
    })
    
    ElMessage.success('风险评估报告已分享给老人及家属，可在小程序中查看')
    
    // 显示分享详情
    ElMessage({
      message: `报告有效期至: ${dayjs(response.validUntil).format('YYYY-MM-DD HH:mm')}`,
      type: 'info',
      duration: 5000
    })
  } catch (error) {
    ElMessage.error('分享报告失败：' + (error.response?.data?.message || error.message))
  } finally {
    sharing.value = false
  }
}

const sharePredictionsToResident = async () => {
  sharingPredictions.value = true
  
  try {
    const response = await api.post({
      url: `/api/enhanced-risk/share-predictions/${props.residentId}`,
      data: {
        predictions: predictions.value,
        assessmentTime: predictionsData.value.assessmentTime,
        riskLevel: predictionsData.value.riskLevel,
        totalScore: predictionsData.value.totalScore,
        confidenceLevel: predictionsData.value.confidenceLevel
      }
    })
    
    ElMessage.success('健康预测结果已分享给老人及家属，可在小程序中查看')
    
    // 显示分享详情
    if (response.validUntil) {
      ElMessage({
        message: `报告有效期至: ${dayjs(response.validUntil).format('YYYY-MM-DD HH:mm')}`,
        type: 'info',
        duration: 5000
      })
    }
  } catch (error) {
    ElMessage.error('分享预测结果失败：' + (error.response?.data?.message || error.message))
  } finally {
    sharingPredictions.value = false
  }
}

const getSeverityType = (severity) => {
  const types = {
    'LOW': 'success',
    'MEDIUM': 'warning', 
    'HIGH': 'danger',
    'CRITICAL': 'danger'
  }
  return types[severity] || 'info'
}

const getSeverityText = (severity) => {
  const texts = {
    'LOW': '低风险',
    'MEDIUM': '中风险',
    'HIGH': '高风险', 
    'CRITICAL': '极高风险'
  }
  return texts[severity] || severity
}

const viewTrend = (record) => {
  const sameType = assessmentList.value.filter((item) => item.assessmentType === record.assessmentType)
  trendRecords.value = [...sameType].sort(
    (left, right) => dayjs(left.assessmentTime).valueOf() - dayjs(right.assessmentTime).valueOf()
  )
  trendVisible.value = true
}

const formatDateTime = (dateTime) => {
  return dateTime ? dayjs(dateTime).format('YYYY-MM-DD HH:mm') : '-'
}

const getRiskLevelType = (level) => {
  const types = {
    'LOW': 'success',
    'MEDIUM': 'warning',
    'HIGH': 'danger',
    'CRITICAL': 'danger'
  }
  return types[level] || ''
}

const getRiskLevelText = (level) => {
  const texts = {
    'LOW': '低风险',
    'MEDIUM': '中风险',
    'HIGH': '高风险',
    'CRITICAL': '极高风险'
  }
  return texts[level] || level
}

const getScoreColor = (score) => {
  if (score >= 75) return '#f5222d'
  if (score >= 50) return '#fa8c16'
  if (score >= 25) return '#fadb14'
  return '#52c41a'
}

const parseJsonArray = (jsonStr) => {
  try {
    const parsed = JSON.parse(jsonStr)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const updateStatistics = (newAssessment) => {
  statistics.total += 1
  
  switch (newAssessment.riskLevel) {
    case 'HIGH':
    case 'CRITICAL':
      statistics.highRisk += 1
      break
    case 'MEDIUM':
      statistics.mediumRisk += 1
      break
    case 'LOW':
      statistics.lowRisk += 1
      break
  }
}

const getPredictionCardClass = (severity) => {
  const classes = {
    'LOW': 'prediction-low',
    'MEDIUM': 'prediction-medium',
    'HIGH': 'prediction-high',
    'CRITICAL': 'prediction-critical'
  }
  return classes[severity] || ''
}
</script>

<style scoped>
.risk-assessment-tab {
  padding: 16px;
}

.score-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}

.score-item:last-child {
  border-bottom: none;
}

.predictions-header {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
}

.prediction-card {
  border-left: 4px solid #e6f7ff;
}

.prediction-card.prediction-low {
  border-left-color: #52c41a;
}

.prediction-card.prediction-medium {
  border-left-color: #faad14;
}

.prediction-card.prediction-high {
  border-left-color: #fa8c16;
}

.prediction-card.prediction-critical {
  border-left-color: #f5222d;
}

.prediction-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.prediction-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.prediction-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.prediction-probability {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.prediction-timeframe {
  background: #e6f7ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.prediction-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #595959;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.symptom-list,
.intervention-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.symptom-list li,
.intervention-list li {
  padding: 4px 0;
  position: relative;
  padding-left: 16px;
}

.symptom-list li::before {
  content: "•";
  color: #fa8c16;
  position: absolute;
  left: 0;
}

.intervention-list li::before {
  content: "✓";
  color: #52c41a;
  position: absolute;
  left: 0;
}

.statistical-basis {
  margin: 0;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  font-size: 13px;
  color: #389e0d;
  line-height: 1.5;
}

.predictions-actions {
  text-align: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
</style>
