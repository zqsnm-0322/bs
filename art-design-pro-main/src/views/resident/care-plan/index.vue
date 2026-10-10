<template>
  <div class="care-plan-page art-full-height">
    <ElCard shadow="never" class="art-table-card">
      <template #header>
        <div class="header">
          <div>
            <strong>护理计划管理</strong>
            <span class="hint">为在住老人制定日常照护、康复和安全任务</span>
          </div>
          <ElSpace>
            <ElSelect v-model="statusFilter" placeholder="计划状态" clearable @change="reload">
              <ElOption label="执行中" value="ACTIVE" />
              <ElOption label="已完成" value="COMPLETED" />
              <ElOption label="已取消" value="CANCELLED" />
            </ElSelect>
            <ElInput v-model="keyword" placeholder="老人姓名/计划名称/床位" clearable @keyup.enter="reload" />
            <ElButton type="primary" @click="openCreate">新建护理计划</ElButton>
          </ElSpace>
        </div>
      </template>

      <ElTable v-loading="loading" :data="plans" border stripe>
        <ElTableColumn prop="residentName" label="老人姓名" width="100" />
        <ElTableColumn prop="roomBedNo" label="房间床位" width="110">
          <template #default="{ row }">{{ row.roomBedNo || '未分配' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="careZone" label="护理区" width="100" />
        <ElTableColumn prop="planTitle" label="计划名称" min-width="160" />
        <ElTableColumn prop="planTypeLabel" label="计划类型" width="110" />
        <ElTableColumn label="优先级" width="90">
          <template #default="{ row }"><ElTag :type="priorityType(row.priority)">{{ row.priorityLabel }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="assignedCaregiver" label="负责护理员" width="110" />
        <ElTableColumn label="任务进度" width="150">
          <template #default="{ row }"><ElProgress :percentage="row.progress || 0" /></template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90">
          <template #default="{ row }"><ElTag :type="statusType(row.status)">{{ statusText(row.status) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link @click="openEdit(row)">编辑</ElButton>
            <ElButton v-if="row.status === 'ACTIVE'" type="warning" link @click="changeStatus(row, 'CANCELLED')">取消</ElButton>
            <ElButton v-if="row.status === 'CANCELLED'" type="success" link @click="changeStatus(row, 'ACTIVE')">恢复</ElButton>
            <ElButton type="danger" link @click="removePlan(row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <ElPagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        class="mt-4 justify-end"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadPlans"
        @size-change="loadPlans"
      />
    </ElCard>

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑护理计划' : '新建护理计划'" width="760px" :close-on-click-modal="false">
      <ElForm :model="form" label-width="100px">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElFormItem label="在住老人" required>
              <ElSelect v-model="form.residentId" filterable :disabled="Boolean(editingId)" style="width:100%" @change="selectResident">
                <ElOption v-for="item in residents" :key="item.id" :value="item.id" :label="`${item.name}（${item.bedNo || '未分配床位'}）`" />
              </ElSelect>
            </ElFormItem>
          </ElCol>
          <ElCol :span="12"><ElFormItem label="计划名称" required><ElInput v-model="form.planTitle" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8"><ElFormItem label="计划类型"><ElSelect v-model="form.planType" @change="syncLabels"><ElOption v-for="item in planTypes" :key="item.value" :value="item.value" :label="item.label" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="优先级"><ElSelect v-model="form.priority" @change="syncLabels"><ElOption v-for="item in priorities" :key="item.value" :value="item.value" :label="item.label" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="负责护理员"><ElSelect v-model="form.assignedCaregiverId" clearable @change="selectCaregiver"><ElOption v-for="item in caregivers" :key="item.id" :value="item.id" :label="item.name" /></ElSelect></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="开始日期" required><ElDatePicker v-model="form.startDate" value-format="YYYY-MM-DD" style="width:100%" /></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="结束日期" required><ElDatePicker v-model="form.endDate" value-format="YYYY-MM-DD" style="width:100%" /></ElFormItem></ElCol>
        </ElRow>
        <ElFormItem label="护理任务">
          <div class="task-list">
            <div v-for="(task, index) in form.tasks" :key="index" class="task-row">
              <ElInput v-model="task.title" placeholder="任务内容，如：协助晨间洗漱" />
              <ElTimePicker v-model="task.time" value-format="HH:mm" placeholder="执行时间" />
              <ElButton type="danger" link @click="form.tasks.splice(index, 1)">移除</ElButton>
            </div>
            <ElButton type="primary" link @click="form.tasks.push({ title: '', time: '' })">+ 添加任务</ElButton>
          </div>
        </ElFormItem>
        <ElFormItem label="照护说明"><ElInput v-model="form.notes" type="textarea" :rows="3" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="submitPlan">保存计划</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    createCarePlan,
    deleteCarePlan,
    fetchCarePlanDetail,
    fetchCarePlanList,
    fetchCarePlanCaregivers,
    fetchCarePlanResidents,
    updateCarePlan,
    updateCarePlanStatus
  } from '@/api/care-plan'
  import { useUserStore } from '@/store/modules/user'

  defineOptions({ name: 'ResidentCarePlan' })

  const planTypes = [
    { value: 'DAILY', label: '日常照护' }, { value: 'REHABILITATION', label: '康复训练' },
    { value: 'SAFETY', label: '安全防护' }, { value: 'NUTRITION', label: '营养照护' }
  ]
  const priorities = [
    { value: 'LOW', label: '一般' }, { value: 'MEDIUM', label: '重要' }, { value: 'HIGH', label: '紧急' }
  ]
  const userStore = useUserStore()
  const loading = ref(false)
  const saving = ref(false)
  const plans = ref<any[]>([])
  const residents = ref<any[]>([])
  const caregivers = ref<any[]>([])
  const statusFilter = ref('')
  const keyword = ref('')
  const pagination = ref({ current: 1, size: 20, total: 0 })
  const dialogVisible = ref(false)
  const editingId = ref<number | null>(null)
  const form = ref<any>({})

  function emptyForm() {
    return {
      residentId: null, residentName: '', bedNo: '', careZone: '', planTitle: '',
      planType: 'DAILY', planTypeLabel: '日常照护', priority: 'MEDIUM', priorityLabel: '重要',
      startDate: '', endDate: '', assignedCaregiverId: null, assignedCaregiver: '', notes: '',
      tasks: [{ title: '', time: '' }]
    }
  }

  function statusText(status: string) {
    return ({ ACTIVE: '执行中', COMPLETED: '已完成', CANCELLED: '已取消' } as Record<string, string>)[status] || status
  }
  function statusType(status: string): 'success' | 'warning' | 'info' {
    return ({ ACTIVE: 'success', COMPLETED: 'info', CANCELLED: 'warning' } as Record<string, any>)[status] || 'info'
  }
  function priorityType(priority: string): 'info' | 'warning' | 'danger' {
    return ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' } as Record<string, any>)[priority] || 'info'
  }

  async function loadPlans() {
    loading.value = true
    try {
      const user = userStore.getUserInfo
      const currentUserId = (user as any).id || user.userId
      const result: any = await fetchCarePlanList({
        current: pagination.value.current, size: pagination.value.size,
        status: statusFilter.value || undefined, keyword: keyword.value || undefined,
        userId: currentUserId, role: user.role
      })
      plans.value = result?.records || []
      pagination.value.total = result?.total || 0
    } catch (error: any) {
      ElMessage.error(error?.message || '获取护理计划失败')
    } finally { loading.value = false }
  }

  function reload() { pagination.value.current = 1; loadPlans() }

  async function openCreate() {
    editingId.value = null
    form.value = emptyForm()
    try {
      const [residentData, caregiverData] = await Promise.all([fetchCarePlanResidents(), fetchCarePlanCaregivers()])
      residents.value = residentData || []
      caregivers.value = caregiverData || []
      dialogVisible.value = true
    } catch { ElMessage.error('加载计划基础数据失败') }
  }

  async function openEdit(row: any) {
    try {
      const [detail, residentData, caregiverData]: any[] = await Promise.all([
        fetchCarePlanDetail(row.id), fetchCarePlanResidents(), fetchCarePlanCaregivers(row.careZone)
      ])
      editingId.value = row.id
      residents.value = residentData || []
      caregivers.value = caregiverData || []
      form.value = { ...emptyForm(), ...detail.plan, bedNo: detail.plan?.roomBedNo || '', tasks: (detail.tasks || []).map((task: any) => ({ title: task.title, time: task.time })) }
      dialogVisible.value = true
    } catch { ElMessage.error('加载护理计划详情失败') }
  }

  async function selectResident(id: number) {
    const resident = residents.value.find((item) => item.id === id)
    if (!resident) return
    Object.assign(form.value, { residentName: resident.name, bedNo: resident.bedNo || '', careZone: resident.careZone || '' })
    caregivers.value = await fetchCarePlanCaregivers(resident.careZone) || []
  }

  function selectCaregiver(id: number) {
    form.value.assignedCaregiver = caregivers.value.find((item) => item.id === id)?.name || ''
  }

  function syncLabels() {
    form.value.planTypeLabel = planTypes.find((item) => item.value === form.value.planType)?.label || ''
    form.value.priorityLabel = priorities.find((item) => item.value === form.value.priority)?.label || ''
  }

  async function submitPlan() {
    if (!form.value.residentId || !form.value.planTitle?.trim() || !form.value.startDate || !form.value.endDate) {
      ElMessage.warning('请完整填写老人、计划名称和计划日期')
      return
    }
    syncLabels()
    saving.value = true
    try {
      const user = userStore.getUserInfo
      const payload = {
        ...form.value,
        tasks: form.value.tasks.filter((task: any) => task.title?.trim()),
        creatorId: user.userId || 1,
        creatorName: user.realName || user.userName || '当前用户',
        creatorRole: user.role || 'CAREGIVER'
      }
      if (editingId.value) await updateCarePlan(editingId.value, payload)
      else await createCarePlan(payload)
      ElMessage.success(editingId.value ? '护理计划已更新' : '护理计划已创建')
      dialogVisible.value = false
      await loadPlans()
    } catch (error: any) {
      ElMessage.error(error?.message || '护理计划保存失败')
    } finally { saving.value = false }
  }

  async function changeStatus(row: any, status: string) {
    try {
      await updateCarePlanStatus(row.id, status)
      ElMessage.success('计划状态已更新')
      await loadPlans()
    } catch { ElMessage.error('计划状态更新失败') }
  }

  async function removePlan(row: any) {
    try {
      await ElMessageBox.confirm(`确定删除护理计划“${row.planTitle}”及其任务吗？`, '删除护理计划', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      })
      await deleteCarePlan(row.id)
      ElMessage.success('护理计划已删除')
      await loadPlans()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('护理计划删除失败')
    }
  }

  onMounted(loadPlans)
</script>

<style scoped>
  .care-plan-page { padding: 20px; }
  .header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
  .hint { margin-left: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
  .task-list { display: flex; width: 100%; flex-direction: column; gap: 8px; }
  .task-row { display: grid; grid-template-columns: minmax(0, 1fr) 150px 60px; gap: 8px; }
  @media (max-width: 900px) { .header { align-items: flex-start; flex-direction: column; } }
</style>
