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
        <ElButton type="primary" :icon="Plus" @click="openEditor()">新增养老风险量表</ElButton>
      </div>

      <ElTable v-loading="loading" :data="rows" border stripe>
        <ElTableColumn prop="scaleName" label="量表名称" min-width="210" />
        <ElTableColumn label="风险类型" width="130">
          <template #default="{ row }">{{ typeLabel(row.scaleType) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="version" label="版本" width="90" />
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
          <ElCol :span="12"><ElFormItem label="量表名称" required><ElInput v-model="form.scaleName" /></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="风险类型" required><ElSelect v-model="form.scaleType" style="width:100%" :disabled="Boolean(form.id)"><ElOption v-for="item in scaleTypes" :key="item.value" :label="item.label" :value="item.value" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="4"><ElFormItem label="版本"><ElInput v-model="form.version" /></ElFormItem></ElCol>
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
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Plus, Refresh, Search } from '@element-plus/icons-vue'
  import { createScale, deleteScale, fetchScaleDetail, fetchScaleList, toggleScaleStatus, updateScale } from '@/api/scale'

  defineOptions({ name: 'ElderlyRiskScaleManagement' })
  const scaleTypes = [
    { value: 'FALL', label: '跌倒风险' }, { value: 'PRESSURE', label: '压疮风险' },
    { value: 'MALNUTRITION', label: '营养不良' }, { value: 'COGNITIVE', label: '认知障碍' },
    { value: 'WANDERING', label: '走失风险' }, { value: 'DEPRESSION', label: '抑郁风险' },
    { value: 'SELF_HARM', label: '自伤风险' }, { value: 'NEGLECT', label: '照护忽视' }
  ]
  const loading = ref(false), saving = ref(false), editorVisible = ref(false)
  const rows = ref<any[]>([])
  const filters = ref({ scaleName: '', scaleType: '' })
  const pagination = ref({ current: 1, size: 10, total: 0 })
  const form = ref<any>({ id: null, scaleName: '', scaleType: '', version: '1.0', description: '', status: 1 })
  const questions = ref<any[]>([])
  const levels = ref(defaultLevels())

  function defaultLevels() { return [
    { level: 'LOW', label: '低风险', min: 0, max: 3, intervention: '定期观察' },
    { level: 'MEDIUM', label: '中风险', min: 4, max: 7, intervention: '加强巡视与预防措施' },
    { level: 'HIGH', label: '高风险', min: 8, max: 100, intervention: '立即制定重点照护方案' }
  ] }
  function typeLabel(value: string) { return scaleTypes.find(item => item.value === value)?.label || value }
  function parse(value: string | undefined, fallback: any) { try { return value ? JSON.parse(value) : fallback } catch { return fallback } }
  async function loadData() { loading.value = true; try { const page:any = await fetchScaleList({ current: pagination.value.current, size: pagination.value.size, ...filters.value }); rows.value = page?.records || []; pagination.value.total = page?.total || 0 } finally { loading.value = false } }
  function search() { pagination.value.current = 1; loadData() }
  function reset() { filters.value = { scaleName: '', scaleType: '' }; search() }
  function addQuestion() { questions.value.push({ questionText: '', questionType: 'radio', required: 1, weight: 1, options: [{ label: '无', score: 0 }, { label: '有', score: 1 }] }) }
  function addOption(question:any) { question.options.push({ label: '', score: 0 }) }
  async function openEditor(row?:any) {
    form.value = { id: null, scaleName: '', scaleType: '', version: '1.0', description: '', status: 1 }; questions.value = []; levels.value = defaultLevels()
    if (row) {
      const detail:any = await fetchScaleDetail(row.id); const def = detail.scaleDef || row
      form.value = { id: def.id, scaleName: def.scaleName, scaleType: def.scaleType, version: def.version || '1.0', description: def.description || '', status: def.status }
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
  @media(max-width:900px){ .toolbar{align-items:flex-start;flex-direction:column}.option-row{grid-template-columns:1fr 120px 20px 50px} }
</style>
