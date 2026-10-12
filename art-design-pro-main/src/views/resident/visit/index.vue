<template>
  <div class="visit-page">
    <ElCard>
      <template #header>
        <div class="card-header">
          <span>探访预约管理</span>
          <ElSpace>
            <ElSelect v-model="statusFilter" placeholder="预约状态" clearable style="width: 140px" @change="handleFilter">
              <ElOption label="待确认" value="PENDING" />
              <ElOption label="已批准" value="APPROVED" />
              <ElOption label="已拒绝" value="REJECTED" />
              <ElOption label="已取消" value="CANCELLED" />
            </ElSelect>
            <ElButton type="primary" :icon="Plus" @click="showSubmitDialog">提交预约</ElButton>
          </ElSpace>
        </div>
      </template>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
        <template #status="{ row }">
          <ElTag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </ElTag>
        </template>
        <template #operation="{ row }">
          <ElSpace>
            <ElButton type="primary" link size="small" @click="showDetail(row)">
              查看
            </ElButton>
            <ElButton
              v-if="row.status === 'PENDING'"
              type="primary"
              link
              size="small"
              @click="showEditDialog(row)"
            >
              编辑
            </ElButton>
            <ElButton
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleApprove(row)"
            >
              审批通过
            </ElButton>
            <ElButton
              v-if="row.status === 'PENDING'"
              type="danger"
              link
              size="small"
              @click="handleReject(row)"
            >
              拒绝
            </ElButton>
            <ElButton
              v-if="row.status !== 'APPROVED' && row.status !== 'COMPLETED'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </ElButton>
            <ElButton
              v-if="row.status === 'APPROVED'"
              type="success"
              link
              size="small"
              @click="handleComplete(row)"
            >
              标记完成
            </ElButton>
            <ElButton
              v-if="row.status === 'APPROVED'"
              type="warning"
              link
              size="small"
              @click="handleCancel(row)"
            >
              取消预约
            </ElButton>
          </ElSpace>
        </template>
      </ArtTable>
    </ElCard>

    <ElDialog v-model="submitDialogVisible" :title="editingId ? '编辑探访预约' : '提交探访预约'" width="500px">
      <ElForm :model="submitForm" label-width="100px">
        <ElFormItem label="探访老人" required>
          <ElSelect
            v-model="submitForm.residentId"
            filterable
            placeholder="请选择在住老人"
            style="width: 100%"
          >
            <ElOption
              v-for="resident in residents"
              :key="resident.id"
              :label="`${resident.name}（${resident.roomBedNo || '未分配床位'}）`"
              :value="resident.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="探访人姓名" required>
          <ElInput v-model="submitForm.visitorName" placeholder="请输入探访人姓名" />
        </ElFormItem>
        <ElFormItem label="探访人电话">
          <ElInput v-model="submitForm.visitorPhone" placeholder="请输入探访人电话" />
        </ElFormItem>
        <ElFormItem label="与老人关系">
          <ElSelect v-model="submitForm.visitorRelation" placeholder="请选择关系">
            <ElOption label="子女" value="子女" />
            <ElOption label="配偶" value="配偶" />
            <ElOption label="其他" value="其他" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="探访日期" required>
          <ElDatePicker
            v-model="submitForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
          />
        </ElFormItem>
        <ElFormItem label="探访人数">
          <ElInputNumber v-model="submitForm.visitorCount" :min="1" :max="10" />
        </ElFormItem>
        <ElFormItem label="探访时段">
          <ElTimePicker
            v-model="visitTimeRange"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="HH:mm:ss"
          />
        </ElFormItem>
        <ElFormItem label="特殊备注">
          <ElInput
            v-model="submitForm.specialNotes"
            type="textarea"
            :rows="3"
            placeholder="请输入特殊备注"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="submitDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="submitLoading">{{ editingId ? '保存' : '提交预约' }}</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="探访预约详情" width="560px">
      <ElDescriptions v-if="detailRecord" :column="2" border>
        <ElDescriptionsItem label="老人姓名">{{ detailRecord.residentName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="预约状态">{{ getStatusText(detailRecord.status) }}</ElDescriptionsItem>
        <ElDescriptionsItem label="探访人">{{ detailRecord.visitorName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="关系">{{ detailRecord.visitorRelation || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="联系电话">{{ detailRecord.visitorPhone || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="探访人数">{{ detailRecord.visitorCount || 1 }}</ElDescriptionsItem>
        <ElDescriptionsItem label="探访日期">{{ detailRecord.visitDate || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="探访时段">
          {{ detailRecord.visitStartTime || '-' }} 至 {{ detailRecord.visitEndTime || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="审批人">{{ detailRecord.approvedBy || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="拒绝原因">{{ detailRecord.rejectReason || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="备注" :span="2">{{ detailRecord.specialNotes || '-' }}</ElDescriptionsItem>
      </ElDescriptions>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Plus } from '@element-plus/icons-vue'
  import {
    fetchVisitList,
    submitVisitAppointment,
    approveVisit,
    rejectVisit,
    updateVisit,
    deleteVisit,
    cancelVisit,
    completeVisit
  } from '@/api/visit'
  import { fetchResidentList } from '@/api/resident'
  import { useUserStore } from '@/store/modules/user'
  import type { ColumnOption } from '@/types/component'

  defineOptions({ name: 'VisitManagement' })

  const userStore = useUserStore()
  const loading = ref(false)
  const data = ref<any[]>([])
  const pagination = ref({ current: 1, size: 20, total: 0 })
  const statusFilter = ref('')
  const submitDialogVisible = ref(false)
  const submitLoading = ref(false)
  const detailDialogVisible = ref(false)
  const detailRecord = ref<any>(null)
  const editingId = ref<number | null>(null)
  const residents = ref<any[]>([])
  const visitTimeRange = ref<[string, string] | null>(null)
  const submitForm = ref({
    residentId: null as number | null,
    visitorName: '',
    visitorPhone: '',
    visitorRelation: '',
    visitDate: null as any,
    visitorCount: 1,
    specialNotes: ''
  })

  const columns: ColumnOption[] = [
    { prop: 'residentName', label: '老人姓名', width: 100 },
    { prop: 'visitorName', label: '探访人', width: 100 },
    { prop: 'visitorRelation', label: '关系', width: 80 },
    { prop: 'visitorPhone', label: '联系电话', width: 130 },
    { prop: 'visitDate', label: '探访日期', width: 120 },
    { prop: 'visitStartTime', label: '开始时间', width: 100 },
    { prop: 'visitEndTime', label: '结束时间', width: 100 },
    { prop: 'visitorCount', label: '人数', width: 60 },
    { prop: 'status', label: '状态', width: 100 },
    { prop: 'createTime', label: '预约时间', width: 160 },
    { prop: 'approvedBy', label: '审批人', width: 100 },
    { prop: 'operation', label: '操作', width: 280, fixed: 'right', useSlot: true }
  ]

  function getStatusType(status: string): 'success' | 'warning' | 'info' | 'danger' {
    const types: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
      PENDING: 'warning',
      APPROVED: 'success',
      REJECTED: 'danger',
      CANCELLED: 'info',
      COMPLETED: 'info'
    }
    return types[status] || 'info'
  }

  function getStatusText(status: string) {
    const texts: Record<string, string> = {
      PENDING: '待确认',
      APPROVED: '已批准',
      REJECTED: '已拒绝',
      CANCELLED: '已取消',
      COMPLETED: '已完成'
    }
    return texts[status] || status
  }

  async function getData() {
    loading.value = true
    try {
      const res: any = await fetchVisitList({
        current: pagination.value.current,
        size: pagination.value.size,
        status: statusFilter.value || undefined
      })
      data.value = res?.records || []
      pagination.value.total = res?.total || 0
    } catch {
      ElMessage.error('获取探访预约列表失败')
    } finally {
      loading.value = false
    }
  }

  function handleFilter() {
    pagination.value.current = 1
    getData()
  }

  function handleSizeChange(size: number) {
    pagination.value.size = size
    getData()
  }

  function handleCurrentChange(current: number) {
    pagination.value.current = current
    getData()
  }

  function showSubmitDialog() {
    editingId.value = null
    submitForm.value = {
      residentId: null,
      visitorName: '',
      visitorPhone: '',
      visitorRelation: '',
      visitDate: null,
      visitorCount: 1,
      specialNotes: ''
    }
    visitTimeRange.value = null
    submitDialogVisible.value = true
  }

  function showEditDialog(row: any) {
    editingId.value = row.id
    submitForm.value = {
      residentId: row.residentId,
      visitorName: row.visitorName || '',
      visitorPhone: row.visitorPhone || '',
      visitorRelation: row.visitorRelation || '',
      visitDate: row.visitDate,
      visitorCount: row.visitorCount || 1,
      specialNotes: row.specialNotes || ''
    }
    visitTimeRange.value = row.visitStartTime && row.visitEndTime
      ? [row.visitStartTime, row.visitEndTime]
      : null
    submitDialogVisible.value = true
  }

  function showDetail(row: any) {
    detailRecord.value = row
    detailDialogVisible.value = true
  }

  async function handleSubmit() {
    if (!submitForm.value.residentId) {
      ElMessage.warning('请选择探访老人')
      return
    }
    if (!submitForm.value.visitorName) {
      ElMessage.warning('请输入探访人姓名')
      return
    }
    if (!submitForm.value.visitDate) {
      ElMessage.warning('请选择探访日期')
      return
    }

    submitLoading.value = true
    try {
      const payload = {
        ...submitForm.value,
        visitStartTime: visitTimeRange.value?.[0],
        visitEndTime: visitTimeRange.value?.[1]
      }
      if (editingId.value) await updateVisit({ id: editingId.value, ...payload })
      else await submitVisitAppointment(payload)
      ElMessage.success(editingId.value ? '预约更新成功' : '预约提交成功，等待审批')
      submitDialogVisible.value = false
      getData()
    } catch {
      ElMessage.error('提交失败')
    } finally {
      submitLoading.value = false
    }
  }

  async function handleApprove(row: any) {
    try {
      const user = userStore.getUserInfo
      await approveVisit(row.id, { approvedBy: user.realName || user.userName || '当前用户' })
      ElMessage.success('审批通过')
      getData()
    } catch {
      ElMessage.error('审批失败')
    }
  }

  async function handleReject(row: any) {
    try {
      const reason = window.prompt('请输入拒绝原因', '预约时间冲突')
      if (reason === null) return
      const user = userStore.getUserInfo
      await rejectVisit(row.id, {
        reason,
        approvedBy: user.realName || user.userName || '当前用户'
      })
      ElMessage.success('已拒绝')
      getData()
    } catch {
      ElMessage.error('拒绝失败')
    }
  }

  async function handleDelete(row: any) {
    try {
      await ElMessageBox.confirm(`确定删除 ${row.visitorName} 的探访预约吗？`, '删除预约', {
        type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '返回'
      })
      await deleteVisit(row.id)
      ElMessage.success('预约已删除')
      await getData()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('删除预约失败')
    }
  }

  async function handleComplete(row: any) {
    try {
      await ElMessageBox.confirm('确认本次探访已经完成吗？', '完成探访', {
        type: 'success', confirmButtonText: '确认完成', cancelButtonText: '返回'
      })
      await completeVisit(row.id)
      ElMessage.success('已标记为完成')
      await getData()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('操作失败')
    }
  }

  async function handleCancel(row: any) {
    try {
      await ElMessageBox.confirm('确定取消这条已批准的探访预约吗？', '取消预约', {
        type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回'
      })
      await cancelVisit(row.id)
      ElMessage.success('预约已取消')
      await getData()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('取消预约失败')
    }
  }

  onMounted(async () => {
    try {
      const result: any = await fetchResidentList({ current: 1, size: 1000 })
      residents.value = result?.records || []
    } catch {
      ElMessage.error('获取在住老人列表失败')
    }
    getData()
  })
</script>

<style scoped>
  .visit-page {
    padding: 20px;
  }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>
