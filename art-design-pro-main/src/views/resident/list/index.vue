<template>
  <div class="resident-page art-full-height">
    <ResidentSearch v-model="searchForm" @search="handleSearch" @reset="resetSearchParams" />

    <ElCard class="art-table-card" shadow="never">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="getData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" :icon="Plus" @click="showAdmissionDialog">入住登记</ElButton>
            <ElButton :icon="Download" @click="handleExport" :loading="exportLoading"
              >导出数据</ElButton
            >
          </ElSpace>
        </template>
      </ArtTableHeader>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columnChecks"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
        <template #operation="{ row }">
          <div class="flex gap-2">
            <ElButton type="primary" link size="small" @click="handleViewDetail(row)"
              >查看档案</ElButton
            >
            <ElButton type="primary" link size="small" @click="handleRiskAssessment(row)"
              >风险评估</ElButton
            >
            <ElButton type="success" link size="small" @click="showEditManagerDialog(row)"
              >照护主管</ElButton
            >
            <ElButton type="info" link size="small" @click="showEditCaregiverDialog(row)"
              >护理员</ElButton
            >
            <ElButton type="warning" link size="small" @click="showAssignBedDialog(row)"
              >分配房间</ElButton
            >
            <ElButton
              v-if="row.status === 'RESIDENT'"
              type="warning"
              link
              size="small"
              @click="showLeaveDialog(row)"
            >
              办理离院
            </ElButton>
          </div>
        </template>
      </ArtTable>
    </ElCard>

    <ResidentAdmissionDialog v-model="admissionDialogVisible" @success="handleAdmissionSuccess" />
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Download, Plus } from '@element-plus/icons-vue'
  import dayjs from 'dayjs'
  import * as XLSX from 'xlsx'
  import { fetchResidentList, leaveResident, updateResident } from '@/api/resident'
  import ResidentSearch from './modules/resident-search.vue'
  import ResidentAdmissionDialog from './modules/resident-admission-dialog.vue'
  import type { ColumnOption } from '@/types/component'

  defineOptions({ name: 'ResidentList' })

  const router = useRouter()
  const loading = ref(false)
  const data = ref<any[]>([])
  const pagination = ref({ current: 1, size: 20, total: 0 })
  const searchForm = ref({ name: '', roomBedNo: '', careZone: '', healthLevel: '' })
  const exportLoading = ref(false)
  const admissionDialogVisible = ref(false)

  const columns: ColumnOption[] = [
    { prop: 'residentId', label: '入住编号', width: 120 },
    { prop: 'name', label: '姓名', width: 100 },
    { prop: 'gender', label: '性别', width: 60 },
    { prop: 'age', label: '年龄', width: 70 },
    { prop: 'roomBedNo', label: '房间床位', width: 100 },
    { prop: 'careZone', label: '护理区', width: 100 },
    { prop: 'healthLevel', label: '健康等级', width: 100 },
    { prop: 'careManager', label: '照护主管', width: 100 },
    { prop: 'caregiverName', label: '护理员', width: 100 },
    { prop: 'admitTime', label: '入住时间', width: 160 },
    { prop: 'operation', label: '操作', width: 280, fixed: 'right', useSlot: true }
  ]
  const columnChecks = ref<ColumnOption[]>(columns)

  async function getData() {
    loading.value = true
    try {
      const res: any = await fetchResidentList({
        current: pagination.value.current,
        size: pagination.value.size,
        ...searchForm.value
      })
      data.value = res?.records || []
      pagination.value.total = res?.total || 0
    } catch {
      ElMessage.error('获取老人列表失败')
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pagination.value.current = 1
    getData()
  }

  function resetSearchParams() {
    searchForm.value = { name: '', roomBedNo: '', careZone: '', healthLevel: '' }
    handleSearch()
  }

  function handleSizeChange(size: number) {
    pagination.value.size = size
    getData()
  }

  function handleCurrentChange(current: number) {
    pagination.value.current = current
    getData()
  }

  function showAdmissionDialog() {
    admissionDialogVisible.value = true
  }

  function handleAdmissionSuccess() {
    admissionDialogVisible.value = false
    getData()
  }

  function handleViewDetail(row: any) {
    router.push({ name: 'ResidentDetail', params: { id: row.id } })
  }

  function handleRiskAssessment(row: any) {
    router.push({ name: 'ResidentAssessment', params: { id: row.id } })
  }

  async function showEditManagerDialog(row: any) {
    try {
      const { value } = await ElMessageBox.prompt('请输入照护主管姓名', '修改照护主管', {
        inputValue: row.careManager || '',
        inputPlaceholder: '照护主管姓名'
      })
      await updateResident({ id: row.id, careManager: value })
      ElMessage.success('照护主管已更新')
      getData()
    } catch (error: any) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '更新失败')
    }
  }

  async function showEditCaregiverDialog(row: any) {
    try {
      const { value } = await ElMessageBox.prompt('请输入护理员姓名', '修改护理员', {
        inputValue: row.caregiverName || '',
        inputPlaceholder: '护理员姓名'
      })
      await updateResident({ id: row.id, caregiverName: value })
      ElMessage.success('护理员已更新')
      getData()
    } catch (error: any) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '更新失败')
    }
  }

  async function showAssignBedDialog(row: any) {
    try {
      const { value } = await ElMessageBox.prompt('请输入房间床位号', '分配房间床位', {
        inputValue: row.roomBedNo || '',
        inputPlaceholder: '例如：A101-1',
        inputValidator: (text) => Boolean(text?.trim()) || '请输入房间床位号'
      })
      await updateResident({ id: row.id, roomBedNo: value })
      ElMessage.success('房间床位已更新')
      getData()
    } catch (error: any) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '更新失败')
    }
  }

  async function showLeaveDialog(row: any) {
    try {
      const { value } = await ElMessageBox.prompt(
        `确认为 ${row.name} 办理离院，请填写离院原因`,
        '办理离院',
        {
          confirmButtonText: '确认离院',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入离院原因',
          inputValidator: (text) => Boolean(text?.trim()) || '请填写离院原因'
        }
      )
      await leaveResident(row.id, { leaveReason: value })
      ElMessage.success('离院办理成功，床位已释放')
      getData()
    } catch (error: any) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '离院办理失败')
    }
  }

  async function handleExport() {
    exportLoading.value = true
    try {
      const result: any = await fetchResidentList({
        current: 1,
        size: Math.max(pagination.value.total, 1000),
        ...searchForm.value
      })
      const rows = (result?.records || []).map((resident: any) => ({
        '入住编号': resident.residentId,
        '姓名': resident.name,
        '性别': resident.gender,
        '年龄': resident.age,
        '房间床位': resident.roomBedNo,
        '护理区': resident.careZone,
        '健康等级': resident.healthLevel,
        '照护主管': resident.careManager,
        '护理员': resident.caregiverName,
        '入住时间': resident.admitTime,
        '入住类型': resident.admissionType,
        '月费标准': resident.feeStandard,
        '紧急联系人': resident.emergencyContactName,
        '紧急联系电话': resident.emergencyContactPhone
      }))
      if (!rows.length) {
        ElMessage.warning('当前筛选条件下没有可导出数据')
        return
      }
      const worksheet = XLSX.utils.json_to_sheet(rows)
      worksheet['!cols'] = Object.keys(rows[0]).map((title) => ({
        wch: Math.max(12, title.length * 2 + 2)
      }))
      const workbook = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(workbook, worksheet, '在住老人')
      XLSX.writeFile(workbook, `在住老人_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
      ElMessage.success(`已导出 ${rows.length} 条老人数据`)
    } catch (error: any) {
      ElMessage.error(error?.message || '导出失败')
    } finally {
      exportLoading.value = false
    }
  }

  onMounted(() => {
    getData()
  })
</script>

<style scoped>
  .resident-page {
    padding: 20px;
  }
</style>
