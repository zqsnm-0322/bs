<template>
  <div class="assessment-page art-full-height p-4">
    <ElCard shadow="never" class="assessment-card h-full">
      <template #header>
        <div class="flex items-center justify-between">
          <div>
            <h3 class="font-bold">养老风险评估 - {{ residentName || '老人' }}</h3>
            <span class="text-sm text-gray-500">{{ residentInfo.residentId || '' }}</span>
          </div>
          <ElButton @click="router.back()">返回</ElButton>
        </div>
      </template>

      <div v-loading="loading" class="assessment-layout">
        <aside class="scale-list">
          <div class="mb-3 font-medium text-gray-500">选择评估量表</div>
          <ElEmpty v-if="!loading && scales.length === 0" description="暂无可用量表" :image-size="80" />
          <button
            v-for="scale in scales"
            :key="scale.id"
            type="button"
            class="scale-item"
            :class="{ active: currentScale?.id === scale.id }"
            @click="selectScale(scale)"
          >
            <span>{{ scale.scaleName }}</span>
            <small>{{ scale.version || '' }}</small>
          </button>
        </aside>

        <main class="assessment-content">
          <template v-if="currentScale">
            <h2 class="text-xl font-bold">{{ currentScale.scaleName }}</h2>
            <p class="mb-5 text-sm text-gray-500">{{ currentScale.description }}</p>

            <ElForm label-position="top">
              <ElFormItem
                v-for="(question, index) in currentScale.questions"
                :key="question.id"
                :label="`${index + 1}. ${question.questionText}${question.required ? ' *' : ''}`"
              >
                <ElRadioGroup v-model="answers[String(question.id)]" @change="calculatePreview">
                  <div class="option-list">
                    <ElRadio v-for="option in question.options" :key="String(option.value)" :value="option.value">
                      {{ option.label }}（{{ option.score }} 分）
                    </ElRadio>
                  </div>
                </ElRadioGroup>
              </ElFormItem>
            </ElForm>

            <div class="result-panel">
              <div>
                当前总分：<strong>{{ preview.totalScore }}</strong>
                <ElTag class="ml-3" :type="riskTagType(preview.riskLevel)">
                  {{ preview.riskLabel }}
                </ElTag>
              </div>
              <ElButton type="primary" :loading="submitting" @click="submitAssessment">提交评估</ElButton>
            </div>

            <ElAlert
              v-if="preview.interventions.length"
              class="mt-4"
              title="建议措施"
              :description="preview.interventions.join('；')"
              :type="riskAlertType(preview.riskLevel)"
              show-icon
              :closable="false"
            />

            <ElAlert
              v-if="lastResult"
              class="mt-4"
              title="评估已保存"
              :description="`后端复核结果：${riskText(lastResult.riskLevel)}，总分 ${lastResult.totalScore} 分。${(lastResult.interventions || []).join('；')}`"
              type="success"
              show-icon
              :closable="false"
            />
          </template>
          <ElEmpty v-else-if="!loading" description="请选择评估量表" />
        </main>
      </div>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { fetchResidentDetail } from '@/api/resident'
  import { fetchEnabledScales, fetchScaleByType, submitRiskAssessment } from '@/api/risk'
  import { useUserStore } from '@/store/modules/user'

  defineOptions({ name: 'ResidentAssessment' })

  interface ScaleOption {
    value: string | number
    label: string
    score: number
  }

  interface ScaleQuestion {
    id: number
    questionText: string
    required: boolean | number
    options: ScaleOption[]
  }

  interface ScaleSummary {
    id: number
    scaleType: string
    scaleName: string
    description?: string
    version?: string
  }

  interface ScaleDetail extends ScaleSummary {
    questions: ScaleQuestion[]
    scoringRules: { levels?: Array<{ level: string; min: number; max: number }> }
    riskLevels: Array<{ level: string; label?: string; interventions?: string[] }>
  }

  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const residentId = Number(route.params.id)
  const residentName = ref('')
  const residentInfo = ref<Record<string, any>>({})
  const loading = ref(false)
  const submitting = ref(false)
  const scales = ref<ScaleSummary[]>([])
  const currentScale = ref<ScaleDetail | null>(null)
  const answers = reactive<Record<string, string | number>>({})
  const lastResult = ref<any>(null)
  const preview = reactive({
    totalScore: 0,
    riskLevel: 'UNKNOWN',
    riskLabel: '待评估',
    interventions: [] as string[]
  })

  function parseJson<T>(value: string | undefined, fallback: T): T {
    if (!value) return fallback
    try {
      return JSON.parse(value) as T
    } catch {
      return fallback
    }
  }

  async function selectScale(scale: ScaleSummary) {
    try {
      const detail: any = await fetchScaleByType(scale.scaleType)
      Object.keys(answers).forEach((key) => delete answers[key])
      lastResult.value = null
      currentScale.value = {
        ...detail,
        questions: (detail.questions || []).map((question: any) => ({
          ...question,
          options: parseJson<ScaleOption[]>(question.optionsJson, [])
        })),
        scoringRules: parseJson(detail.scoringRulesJson, { levels: [] }),
        riskLevels: parseJson(detail.riskLevelsJson, [])
      }
      calculatePreview()
    } catch (error: any) {
      ElMessage.error(error?.message || '加载量表失败')
    }
  }

  function calculatePreview() {
    const scale = currentScale.value
    if (!scale) return
    const total = scale.questions.reduce((sum, question) => {
      const answer = answers[String(question.id)]
      const option = question.options.find((item) => item.value === answer)
      return sum + Number(option?.score || 0)
    }, 0)
    const levelRule = scale.scoringRules.levels?.find((item) => total >= item.min && total <= item.max)
    const level = levelRule?.level || 'UNKNOWN'
    const riskConfig = scale.riskLevels.find((item) => item.level === level)
    preview.totalScore = total
    preview.riskLevel = level
    preview.riskLabel = riskConfig?.label || riskText(level)
    preview.interventions = riskConfig?.interventions || []
  }

  async function submitAssessment() {
    const scale = currentScale.value
    if (!scale) return
    const unanswered = scale.questions.find(
      (question) => Boolean(question.required) && answers[String(question.id)] == null
    )
    if (unanswered) {
      ElMessage.warning(`请回答：${unanswered.questionText}`)
      return
    }

    submitting.value = true
    try {
      const user = userStore.getUserInfo
      lastResult.value = await submitRiskAssessment({
        residentId,
        scaleType: scale.scaleType,
        answers: { ...answers },
        evaluatorId: user.userId || 1,
        evaluatorName: user.realName || user.userName || '当前用户',
        requestSource: 'PC'
      })
      ElMessage.success('养老风险评估已保存')
    } catch (error: any) {
      ElMessage.error(error?.message || '评估提交失败')
    } finally {
      submitting.value = false
    }
  }

  function riskText(level: string) {
    return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '极高风险' } as Record<string, string>)[level] || '待评估'
  }

  function riskTagType(level: string): 'success' | 'warning' | 'danger' | 'info' {
    const types: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
      LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger'
    }
    return types[level] || 'info'
  }

  function riskAlertType(level: string): 'success' | 'warning' | 'error' | 'info' {
    const types: Record<string, 'success' | 'warning' | 'error' | 'info'> = {
      LOW: 'success', MEDIUM: 'warning', HIGH: 'error', CRITICAL: 'error'
    }
    return types[level] || 'info'
  }

  onMounted(async () => {
    if (!Number.isFinite(residentId)) {
      ElMessage.error('老人ID无效')
      return
    }
    loading.value = true
    try {
      const [resident, enabledScales] = await Promise.all([
        fetchResidentDetail(residentId),
        fetchEnabledScales()
      ])
      residentInfo.value = resident as Record<string, any>
      residentName.value = residentInfo.value.name || ''
      scales.value = (enabledScales || []) as ScaleSummary[]
      if (scales.value.length) await selectScale(scales.value[0])
    } catch (error: any) {
      ElMessage.error(error?.message || '加载评估数据失败')
    } finally {
      loading.value = false
    }
  })
</script>

<style scoped>
  .assessment-page {
    box-sizing: border-box;
    overflow: hidden;
  }
  :deep(.assessment-card) {
    display: flex;
    height: 100% !important;
    min-height: 0;
    flex-direction: column;
  }
  :deep(.assessment-card .el-card__header) {
    flex: none;
  }
  :deep(.assessment-card .el-card__body) {
    min-height: 0;
    flex: 1;
    overflow-y: auto;
    overscroll-behavior: contain;
  }
  .assessment-layout {
    display: grid;
    grid-template-columns: 240px minmax(0, 1fr);
    gap: 24px;
    min-height: 520px;
  }
  .scale-list {
    padding-right: 20px;
    border-right: 1px solid var(--el-border-color-lighter);
  }
  .scale-item {
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    padding: 12px;
    color: var(--el-text-color-regular);
    text-align: left;
    background: transparent;
    border: 0;
    border-radius: 6px;
    cursor: pointer;
  }
  .scale-item:hover,
  .scale-item.active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
  .assessment-content {
    min-width: 0;
    padding-bottom: 24px;
    padding-right: 8px;
  }
  .option-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .result-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
  }
  @media (max-width: 900px) {
    .assessment-layout {
      grid-template-columns: 1fr;
    }
    .scale-list {
      padding-right: 0;
      border-right: 0;
    }
  }
</style>
