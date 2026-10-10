<template>
  <div class="scale-page art-full-height">
    <ElCard shadow="never">
      <div class="toolbar">
        <ElSpace wrap>
          <ElInput v-model="filters.scaleName" placeholder="量表名称" clearable @keyup.enter="loadData" />
          <ElSelect v-model="filters.scaleType" placeholder="风险类型" clearable>
            <ElOption v-for="item in scaleTypes" :key="item.value" :label="item.label" :value="item.value" />
          </ElSelect>
          <ElButton type="primary" :icon="Search" @click="search">查询</ElButton>
          <ElButton :icon="Refresh" @click="reset">重置</ElButton>
        </ElSpace>
        <ElSpace>
          <ElTag :type="enabledWeightTotal === 100 ? 'success' : 'warning'">启用权重合计：{{ enabledWeightTotal }}%</ElTag>
          <ElButton :icon="Document" @click="openRecords">养老风险记录</ElButton>
          <ElButton type="primary" :icon="Plus" @click="openEditor()">新增养老风险量表</ElButton>
        </ElSpace>
      </div>

      <ElTable v-loading="loading" :data="rows" border stripe>
        <ElTableColumn prop="scaleName" label="量表名称" min-width="210" />
        <ElTableColumn label="风险类型" width="130">
          <template #default="{ row }">{{ typeLabel(row.scaleType) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="version" label="版本" width="90" />
        <ElTableColumn label="权重" width="100">
          <template #default="{ row }"><ElTag type="primary">{{ scaleWeight(row) }}%</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <ElTableColumn label="状态" width="90">
          <template #default="{ row }"><ElTag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已启用' : '已停用' }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link @click="openEditor(row)">编辑题目</ElButton>
            <ElButton :type="row.status === 1 ? 'warning' : 'success'" link @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</ElButton>
            <ElButton type="danger" link @click="remove(row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <ElPagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" class="pagination" :total="pagination.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
    </ElCard>

    <ElDialog v-model="editorVisible" :title="form.id ? '编辑养老风险量表' : '新增养老风险量表'" width="min(1100px, 94vw)" destroy-on-close>
      <ElForm label-width="95px">
        <ElRow :gutter="16">
          <ElCol :span="10"><ElFormItem label="量表名称" required><ElInput v-model="form.scaleName" /></ElFormItem></ElCol>
          <ElCol :span="6"><ElFormItem label="风险类型" required><ElSelect v-model="form.scaleType" style="width:100%" :disabled="Boolean(form.id)"><ElOption v-for="item in scaleTypes" :key="item.value" :label="item.label" :value="item.value" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="4"><ElFormItem label="版本"><ElInput v-model="form.version" /></ElFormItem></ElCol>
          <ElCol :span="4"><ElFormItem label="权重"><ElInputNumber v-model="form.assessmentWeight" :min="0" :max="100" :precision="1" controls-position="right" style="width:100%" /><span class="weight-hint">量表模块内部占比</span></ElFormItem></ElCol>
        </ElRow>
        <ElFormItem label="量表说明"><ElInput v-model="form.description" type="textarea" :rows="2" /></ElFormItem>

        <div class="section-title"><span>评估题目</span><ElButton type="primary" link :icon="Plus" @click="addQuestion">添加题目</ElButton></div>
        <ElEmpty v-if="questions.length === 0" description="请至少添加一道题目" :image-size="70" />
        <div v-for="(question, qIndex) in questions" :key="qIndex" class="question-card">
          <div class="question-head">
            <strong>第 {{ qIndex + 1 }} 题</strong>
            <ElButton type="danger" link @click="questions.splice(qIndex, 1)">删除题目</ElButton>
          </div>
          <ElInput v-model="question.questionText" placeholder="请输入题目内容" />
          <div v-for="(option, oIndex) in question.options" :key="oIndex" class="option-row">
            <ElInput v-model="option.label" placeholder="选项内容" />
            <ElInputNumber v-model="option.score" :min="0" :max="100" />
            <span>分</span>
            <ElButton type="danger" link @click="question.options.splice(oIndex, 1)">删除</ElButton>
          </div>
          <ElButton link type="primary" :icon="Plus" @click="addOption(question)">添加选项</ElButton>
        </div>

        <div class="section-title">风险等级分值范围</div>
        <ElRow :gutter="16">
          <ElCol v-for="level in levels" :key="level.level" :span="8">
            <ElCard shadow="never">
              <div class="level-title">{{ level.label }}</div>
              <div class="range-row">
                <ElInputNumber v-model="level.min" :min="0" controls-position="right" />
                <span>至</span>
                <ElInputNumber v-model="level.max" :min="0" controls-position="right" />
              </div>
              <ElInput v-model="level.intervention" class="mt-3" placeholder="建议干预措施" />
            </ElCard>
          </ElCol>
        </ElRow>
      </ElForm>
      <template #footer><ElButton @click="editorVisible=false">取消</ElButton><ElButton type="primary" :loading="saving" @click="save">保存量表</ElButton></template>
    </ElDialog>

    <ElDialog v-model="recordsVisible" title="养老风险评估记录" width="min(1100px, 94vw)">
      <ElTable v-loading="recordsLoading" :data="riskRecords" border stripe max-height="560">
        <ElTableColumn prop="residentName" label="老人姓名" width="110" />
        <ElTableColumn label="量表" min-width="180"><template #default="{ row }">{{ typeLabel(row.scaleType) }}</template></ElTableColumn>
        <ElTableColumn prop="totalScore" label="总分" width="80" />
        <ElTableColumn label="风险等级" width="100"><template #default="{ row }"><ElTag :type="riskTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</ElTag></template></ElTableColumn>
        <ElTableColumn prop="evaluatorName" label="评估人" width="110"><template #default="{ row }">{{ row.evaluatorName || '-' }}</template></ElTableColumn>
        <ElTableColumn prop="evaluateTime" label="评估时间" width="180" />
        <ElTableColumn label="操作" width="90" fixed="right"><template #default="{ row }"><ElButton type="primary" link @click="viewRecord(row)">详情</ElButton></template></ElTableColumn>
      </ElTable>
      <ElEmpty v-if="!recordsLoading && riskRecords.length === 0" description="暂无养老风险评估记录" />
    </ElDialog>

    <ElDialog v-model="recordDetailVisible" title="养老风险评估详情" width="680px">
      <ElDescriptions v-if="recordDetail" :column="2" border>
        <ElDescriptionsItem label="老人姓名">{{ recordDetail.residentName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="房间床位">{{ recordDetail.bedNo }}</ElDescriptionsItem>
        <ElDescriptionsItem label="护理区">{{ recordDetail.careZone }}</ElDescriptionsItem>
        <ElDescriptionsItem label="量表名称">{{ recordDetail.scaleName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="评估总分">{{ recordDetail.totalScore }}</ElDescriptionsItem>
        <ElDescriptionsItem label="风险等级"><ElTag :type="riskTagType(recordDetail.riskLevel)">{{ recordDetail.riskLevelText }}</ElTag></ElDescriptionsItem>
        <ElDescriptionsItem label="评估人员">{{ recordDetail.evaluatorName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="评估时间">{{ recordDetail.evaluateTime }}</ElDescriptionsItem>
        <ElDescriptionsItem label="干预建议" :span="2">
          <div v-if="recordDetail.interventions?.length"><div v-for="item in recordDetail.interventions" :key="item">• {{ item }}</div></div>
          <span v-else>暂无干预建议</span>
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Document, Plus, Refresh, Search } from '@element-plus/icons-vue'
  import { createScale, deleteScale, fetchScaleDetail, fetchScaleList, toggleScaleStatus, updateScale } from '@/api/scale'
  import { fetchRiskRecordDetail, fetchRiskRecordList } from '@/api/risk'

  defineOptions({ name: 'ElderlyRiskScaleManagement' })
  const scaleTypes = [
    { value: 'FALL', label: '跌倒风险' }, { value: 'PRESSURE', label: '压疮风险' },
    { value: 'MALNUTRITION', label: '营养不良' }, { value: 'COGNITIVE', label: '认知障碍' },
    { value: 'WANDERING', label: '走失风险' }, { value: 'DEPRESSION', label: '抑郁风险' },
    { value: 'SELF_HARM', label: '自伤风险' }, { value: 'NEGLECT', label: '照护忽视' }
  ]
  const defaultWeights:Record<string, number> = { FALL:25, PRESSURE:20, COGNITIVE:20, MALNUTRITION:15, DEPRESSION:10, SELF_HARM:10, WANDERING:0, NEGLECT:0 }
  const loading = ref(false), saving = ref(false), editorVisible = ref(false)
  const recordsVisible = ref(false), recordsLoading = ref(false), recordDetailVisible = ref(false)
  const riskRecords = ref<any[]>([]), recordDetail = ref<any>(null)
  const rows = ref<any[]>([])
  const filters = ref({ scaleName: '', scaleType: '' })
  const pagination = ref({ current: 1, size: 10, total: 0 })
  const scaleWeight = (item:any) => item.assessmentWeight == null ? (defaultWeights[item.scaleType] || 0) : Number(item.assessmentWeight)
  const enabledWeightTotal = computed(() => rows.value.filter(item => item.status === 1).reduce((sum, item) => sum + scaleWeight(item), 0))
  const form = ref<any>({ id: null, scaleName: '', scaleType: '', version: '1.0', assessmentWeight: 0, description: '', status: 1 })
  const questions = ref<any[]>([])
  const levels = ref(defaultLevels())

  function defaultLevels() { return [
    { level: 'LOW', label: '低风险', min: 0, max: 3, intervention: '定期观察' },
    { level: 'MEDIUM', label: '中风险', min: 4, max: 7, intervention: '加强巡视与预防措施' },
    { level: 'HIGH', label: '高风险', min: 8, max: 100, intervention: '立即制定重点照护方案' }
  ] }
  function typeLabel(value: string) { return scaleTypes.find(item => item.value === value)?.label || value }
  function riskLevelText(value:string) { return ({ LOW:'低风险', MEDIUM:'中风险', HIGH:'高风险', CRITICAL:'极高风险' } as any)[value] || value || '-' }
  function riskTagType(value:string) { return ({ LOW:'success', MEDIUM:'warning', HIGH:'danger', CRITICAL:'danger' } as any)[value] || 'info' }
  function parse(value: string | undefined, fallback: any) { try { return value ? JSON.parse(value) : fallback } catch { return fallback } }
  async function loadData() { loading.value = true; try { const page:any = await fetchScaleList({ current: pagination.value.current, size: pagination.value.size, ...filters.value }); rows.value = page?.records || []; pagination.value.total = page?.total || 0 } finally { loading.value = false } }
  function search() { pagination.value.current = 1; loadData() }
  function reset() { filters.value = { scaleName: '', scaleType: '' }; search() }
  function addQuestion() { questions.value.push({ questionText: '', questionType: 'radio', required: 1, weight: 1, options: [{ label: '无', score: 0 }, { label: '有', score: 1 }] }) }
  function addOption(question:any) { question.options.push({ label: '', score: 0 }) }
  async function openEditor(row?:any) {
    form.value = { id: null, scaleName: '', scaleType: '', version: '1.0', assessmentWeight: 0, description: '', status: 1 }; questions.value = []; levels.value = defaultLevels()
    if (row) {
      const detail:any = await fetchScaleDetail(row.id); const def = detail.scaleDef || row
      form.value = { id: def.id, scaleName: def.scaleName, scaleType: def.scaleType, version: def.version || '1.0', assessmentWeight: def.assessmentWeight == null ? (defaultWeights[def.scaleType] || 0) : Number(def.assessmentWeight), description: def.description || '', status: def.status }
      questions.value = (detail.questions || []).map((q:any) => ({ ...q, options: parse(q.optionsJson, []) }))
      const rules = parse(def.scoringRulesJson, { levels: [] }).levels || []
      const configs = parse(def.riskLevelsJson, [])
      levels.value = defaultLevels().map(base => { const rule=rules.find((x:any)=>x.level===base.level); const cfg=configs.find((x:any)=>x.level===base.level); return { ...base, ...rule, intervention: cfg?.interventions?.join('；') || base.intervention } })
    }
    editorVisible.value = true
  }
  async function save() {
    if (!form.value.scaleName || !form.value.scaleType) return ElMessage.warning('请填写量表名称和风险类型')
    if (!questions.value.length || questions.value.some(q => !q.questionText || q.options.length < 2 || q.options.some((o:any)=>!o.label))) return ElMessage.warning('请完整填写题目，每题至少包含两个选项')
    if (levels.value.some((item, i) => item.min > item.max || (i > 0 && item.min <= levels.value[i-1].max))) return ElMessage.warning('风险等级分值范围不能重叠且最小值不能大于最大值')
    saving.value = true
    try {
      const scaleDef = { ...form.value, scoringRulesJson: JSON.stringify({ levels: levels.value.map(({level,min,max})=>({level,min,max})) }), riskLevelsJson: JSON.stringify(levels.value.map(item=>({ level:item.level, label:item.label, interventions:item.intervention.split(/[；;]/).filter(Boolean) }))) }
      const payload = { scaleDef, questions: questions.value.map((q,index)=>({ id:q.id, questionOrder:index+1, questionText:q.questionText, questionType:'radio', optionsJson:JSON.stringify(q.options.map((o:any, oi:number)=>({ value:String(oi), label:o.label, score:Number(o.score) }))), required:1, weight:Number(q.weight || 1), groupName:q.groupName || '' })) }
      if (form.value.id) await updateScale(payload); else await createScale(payload)
      ElMessage.success('量表保存成功'); editorVisible.value = false; await loadData()
    } catch (error:any) { ElMessage.error(error?.message || '量表保存失败') } finally { saving.value = false }
  }
  async function toggle(row:any) { await toggleScaleStatus(row.id, row.status !== 1); ElMessage.success(row.status === 1 ? '量表已停用' : '量表已启用'); loadData() }
  async function remove(row:any) { try { await ElMessageBox.confirm(`确定删除“${row.scaleName}”及其全部题目吗？`, '删除量表', { type:'warning' }); await deleteScale(row.id); ElMessage.success('删除成功'); loadData() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败') } }
  async function openRecords() { recordsVisible.value = true; recordsLoading.value = true; try { const data:any = await fetchRiskRecordList(); riskRecords.value = Array.isArray(data) ? data : data?.records || [] } catch { ElMessage.error('获取养老风险记录失败') } finally { recordsLoading.value = false } }
  async function viewRecord(row:any) { try { recordDetail.value = await fetchRiskRecordDetail(row.id); recordDetailVisible.value = true } catch { ElMessage.error('获取评估详情失败') } }
  loadData()
</script>

<style scoped>
  .scale-page { padding: 20px; }
  .toolbar { display:flex; justify-content:space-between; gap:16px; margin-bottom:16px; }
  .pagination { justify-content:flex-end; margin-top:16px; }
  .section-title { display:flex; align-items:center; justify-content:space-between; margin:18px 0 10px; font-weight:600; }
  .question-card { margin-bottom:12px; padding:14px; border:1px solid var(--el-border-color-lighter); border-radius:8px; }
  .question-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
  .option-row { display:grid; grid-template-columns:1fr 150px 24px 55px; align-items:center; gap:8px; margin-top:8px; }
  .level-title { margin-bottom:10px; font-weight:600; }
  .range-row { display:grid; grid-template-columns:minmax(90px, 1fr) 24px minmax(90px, 1fr); align-items:center; gap:8px; }
  .range-row :deep(.el-input-number) { width:100%; }
  .range-row > span { text-align:center; }
  .weight-hint { display:block; margin-top:4px; color:var(--el-text-color-secondary); font-size:12px; }
  @media(max-width:900px){ .toolbar{align-items:flex-start;flex-direction:column}.option-row{grid-template-columns:1fr 120px 20px 50px} }
</style>
