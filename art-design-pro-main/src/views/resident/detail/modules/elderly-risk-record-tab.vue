<template>
  <div class="risk-record-tab">
    <div class="summary-grid">
      <ElCard shadow="never"><ElStatistic title="养老风险评估总数" :value="records.length" /></ElCard>
      <ElCard shadow="never"><ElStatistic title="高风险" :value="levelCount.HIGH" /></ElCard>
      <ElCard shadow="never"><ElStatistic title="中风险" :value="levelCount.MEDIUM" /></ElCard>
      <ElCard shadow="never"><ElStatistic title="低风险" :value="levelCount.LOW" /></ElCard>
    </div>

    <div class="toolbar">
      <ElSelect v-model="scaleFilter" placeholder="全部风险类型" clearable style="width:190px">
        <ElOption v-for="item in scaleTypes" :key="item.value" :label="item.label" :value="item.value" />
      </ElSelect>
      <ElSelect v-model="levelFilter" placeholder="全部风险等级" clearable style="width:160px">
        <ElOption label="低风险" value="LOW" /><ElOption label="中风险" value="MEDIUM" /><ElOption label="高风险" value="HIGH" />
      </ElSelect>
      <ElButton :icon="Refresh" @click="loadRecords">刷新</ElButton>
    </div>

    <ElTable v-loading="loading" :data="filteredRecords" border stripe>
      <ElTableColumn prop="evaluateTime" label="评估时间" width="180"><template #default="{ row }">{{ formatTime(row.evaluateTime) }}</template></ElTableColumn>
      <ElTableColumn label="养老风险量表" min-width="210"><template #default="{ row }">{{ row.scaleName || typeLabel(row.scaleType) }}</template></ElTableColumn>
      <ElTableColumn label="量表权重" width="105"><template #default="{ row }"><ElTag type="primary">{{ weightOf(row.scaleType) }}%</ElTag></template></ElTableColumn>
      <ElTableColumn prop="totalScore" label="原始评分" width="100" />
      <ElTableColumn label="风险等级" width="105"><template #default="{ row }"><ElTag :type="riskTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</ElTag></template></ElTableColumn>
      <ElTableColumn prop="evaluatorName" label="评估人" width="120"><template #default="{ row }">{{ row.evaluatorName || '-' }}</template></ElTableColumn>
      <ElTableColumn label="报告状态" width="110"><template #default="{ row }"><ElTag :type="row.sentToResident ? 'success' : 'info'">{{ row.sentToResident ? '已分享' : '未分享' }}</ElTag></template></ElTableColumn>
      <ElTableColumn label="操作" width="90" fixed="right"><template #default="{ row }"><ElButton type="primary" link @click="viewDetail(row)">详情</ElButton></template></ElTableColumn>
    </ElTable>
    <ElEmpty v-if="!loading && filteredRecords.length === 0" description="暂无养老风险评估记录" />

    <ElDialog v-model="detailVisible" title="养老风险评估记录详情" width="700px">
      <ElDescriptions v-if="detail" :column="2" border>
        <ElDescriptionsItem label="老人姓名">{{ detail.residentName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="房间床位">{{ detail.bedNo }}</ElDescriptionsItem>
        <ElDescriptionsItem label="护理区">{{ detail.careZone }}</ElDescriptionsItem>
        <ElDescriptionsItem label="量表名称">{{ detail.scaleName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="量表权重">{{ weightOf(detail.scaleType) }}%</ElDescriptionsItem>
        <ElDescriptionsItem label="评估总分">{{ detail.totalScore }}</ElDescriptionsItem>
        <ElDescriptionsItem label="风险等级"><ElTag :type="riskTagType(detail.riskLevel)">{{ detail.riskLevelText }}</ElTag></ElDescriptionsItem>
        <ElDescriptionsItem label="评估人员">{{ detail.evaluatorName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="评估时间" :span="2">{{ formatTime(detail.evaluateTime) }}</ElDescriptionsItem>
        <ElDescriptionsItem label="干预建议" :span="2">
          <div v-if="detail.interventions?.length"><div v-for="item in detail.interventions" :key="item">• {{ item }}</div></div>
          <span v-else>暂无干预建议</span>
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { ElMessage } from 'element-plus'
  import { Refresh } from '@element-plus/icons-vue'
  import { fetchResidentRiskHistory, fetchRiskRecordDetail } from '@/api/risk'
  import { fetchScaleList } from '@/api/scale'

  const props = defineProps<{ residentId: number }>()
  const loading = ref(false), detailVisible = ref(false)
  const records = ref<any[]>([]), scales = ref<any[]>([]), detail = ref<any>(null)
  const scaleFilter = ref(''), levelFilter = ref('')
  const scaleTypes = [
    { value:'FALL', label:'跌倒风险' }, { value:'PRESSURE', label:'压疮风险' },
    { value:'MALNUTRITION', label:'营养不良' }, { value:'COGNITIVE', label:'认知障碍' },
    { value:'WANDERING', label:'走失风险' }, { value:'DEPRESSION', label:'抑郁风险' },
    { value:'SELF_HARM', label:'自伤风险' }, { value:'NEGLECT', label:'照护忽视' }
  ]
  const defaultWeights:Record<string, number> = { FALL:25, PRESSURE:20, COGNITIVE:20, MALNUTRITION:15, DEPRESSION:10, SELF_HARM:10, WANDERING:0, NEGLECT:0 }
  const filteredRecords = computed(() => records.value.filter(item =>
    (!scaleFilter.value || item.scaleType === scaleFilter.value) && (!levelFilter.value || item.riskLevel === levelFilter.value)))
  const levelCount = computed(() => ({
    HIGH: records.value.filter(item => ['HIGH','CRITICAL'].includes(item.riskLevel)).length,
    MEDIUM: records.value.filter(item => item.riskLevel === 'MEDIUM').length,
    LOW: records.value.filter(item => item.riskLevel === 'LOW').length
  }))
  function typeLabel(type:string) { return scaleTypes.find(item => item.value === type)?.label || type }
  function riskLevelText(level:string) { return ({ LOW:'低风险', MEDIUM:'中风险', HIGH:'高风险', CRITICAL:'极高风险' } as any)[level] || level || '-' }
  function riskTagType(level:string) { return level === 'LOW' ? 'success' : level === 'MEDIUM' ? 'warning' : ['HIGH','CRITICAL'].includes(level) ? 'danger' : 'info' }
  function weightOf(type:string) {
    const configured = scales.value.find(item => item.scaleType === type)?.assessmentWeight
    return configured == null ? (defaultWeights[type] || 0) : Number(configured)
  }
  function formatTime(value:string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12:false }) : '-' }
  async function loadRecords() {
    loading.value = true
    try {
      const [history, scalePage]:any = await Promise.all([
        fetchResidentRiskHistory(props.residentId), fetchScaleList({ current:1, size:50 })
      ])
      records.value = Array.isArray(history) ? history : []
      scales.value = scalePage?.records || []
    } catch { ElMessage.error('获取养老风险记录失败') } finally { loading.value = false }
  }
  async function viewDetail(row:any) {
    try { detail.value = await fetchRiskRecordDetail(row.id); detailVisible.value = true }
    catch { ElMessage.error('获取养老风险评估详情失败') }
  }
  onMounted(loadRecords)
</script>

<style scoped>
  .risk-record-tab { padding:10px; }
  .summary-grid { display:grid; grid-template-columns:repeat(4, 1fr); gap:16px; margin-bottom:18px; }
  .toolbar { display:flex; gap:12px; margin-bottom:16px; }
  @media(max-width:900px){ .summary-grid{grid-template-columns:repeat(2,1fr)} }
</style>
