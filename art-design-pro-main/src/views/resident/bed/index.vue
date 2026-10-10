<template>
  <div class="bed-page">
    <ElRow :gutter="16" class="mb-4">
      <ElCol v-for="item in statCards" :key="item.label" :span="6" :xs="12">
        <ElCard shadow="never">
          <ElStatistic :title="item.label" :value="item.value" />
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard shadow="never">
      <div class="toolbar mb-4">
        <ElSpace wrap>
          <ElSelect v-model="filters.careZone" placeholder="护理区" clearable @change="reload">
            <ElOption v-for="zone in careZones" :key="zone" :label="zone" :value="zone" />
          </ElSelect>
          <ElSelect v-model="filters.status" placeholder="床位状态" clearable @change="reload">
            <ElOption label="空闲" value="AVAILABLE" />
            <ElOption label="已入住" value="OCCUPIED" />
            <ElOption label="维护中" value="MAINTENANCE" />
          </ElSelect>
          <ElInput v-model="filters.roomBedNo" placeholder="房间床位号" clearable @keyup.enter="reload" />
          <ElButton type="primary" @click="reload">查询</ElButton>
          <ElButton @click="resetFilters">重置</ElButton>
        </ElSpace>
        <ElSpace><ElButton type="primary" @click="openBedDialog()">新增床位</ElButton><ElButton :loading="loading" @click="loadData">刷新</ElButton></ElSpace>
      </div>

      <ElTable v-loading="loading" :data="beds" border stripe>
        <ElTableColumn prop="roomBedNo" label="房间床位" width="120" />
        <ElTableColumn prop="careZone" label="护理区" width="110" />
        <ElTableColumn prop="floor" label="楼层" width="80" />
        <ElTableColumn prop="roomNo" label="房间号" width="100" />
        <ElTableColumn prop="roomType" label="房型" width="100" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="statusType(row.status)">{{ statusText(row.status) }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="residentName" label="入住老人" min-width="120">
          <template #default="{ row }">{{ row.residentName || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <ElButton v-if="row.status === 'AVAILABLE'" type="primary" link @click="openAssign(row)">
              分配老人
            </ElButton>
            <ElButton v-if="row.residentId" type="primary" link @click="viewResident(row)">
              查看档案
            </ElButton>
            <ElButton type="primary" link @click="openBedDialog(row)">编辑</ElButton>
            <ElButton v-if="row.status !== 'OCCUPIED'" type="danger" link @click="removeBed(row)">删除</ElButton>
            <ElButton
              v-if="row.status !== 'OCCUPIED'"
              type="warning"
              link
              @click="toggleMaintenance(row)"
            >
              {{ row.status === 'MAINTENANCE' ? '结束维护' : '设为维护' }}
            </ElButton>
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
        @current-change="loadData"
        @size-change="loadData"
      />
    </ElCard>

    <ElDialog v-model="assignVisible" title="分配房间床位" width="520px">
      <ElForm label-width="100px">
        <ElFormItem label="目标床位"><ElInput :model-value="selectedBed?.roomBedNo" disabled /></ElFormItem>
        <ElFormItem label="护理区"><ElInput :model-value="selectedBed?.careZone" disabled /></ElFormItem>
        <ElFormItem label="选择老人" required>
          <ElSelect v-model="selectedResidentId" filterable style="width:100%" placeholder="请选择在住老人">
            <ElOption
              v-for="resident in residents"
              :key="resident.id"
              :value="resident.id"
              :label="`${resident.name}（${resident.roomBedNo || '尚未分配'}）`"
            />
          </ElSelect>
        </ElFormItem>
        <ElAlert title="如老人已有床位，保存后将自动迁移到当前床位。" type="info" :closable="false" />
      </ElForm>
      <template #footer>
        <ElButton @click="assignVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="confirmAssign">确认分配</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="bedDialogVisible" :title="bedForm.id ? '编辑床位' : '新增床位'" width="520px">
      <ElForm :model="bedForm" label-width="90px">
        <ElFormItem label="床位号" required><ElInput v-model="bedForm.roomBedNo" placeholder="如 A101-1" /></ElFormItem>
        <ElFormItem label="护理区" required><ElSelect v-model="bedForm.careZone" style="width:100%"><ElOption v-for="zone in careZones" :key="zone" :label="zone" :value="zone" /></ElSelect></ElFormItem>
        <ElFormItem label="楼层" required><ElInputNumber v-model="bedForm.floor" :min="1" /></ElFormItem>
        <ElFormItem label="房间号" required><ElInput v-model="bedForm.roomNo" placeholder="如 101" /></ElFormItem>
        <ElFormItem label="房型" required><ElSelect v-model="bedForm.roomType" style="width:100%"><ElOption v-for="type in ['单人房','双人房','多人房']" :key="type" :label="type" :value="type" /></ElSelect></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="bedDialogVisible=false">取消</ElButton><ElButton type="primary" :loading="saving" @click="saveBed">保存</ElButton></template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { addBed, updateBed, deleteBed, assignBed, getBedList, getBedStatistics, setBedMaintenance } from '@/api/bed'
  import { fetchResidentList } from '@/api/resident'

  defineOptions({ name: 'ResidentBedManagement' })

  const router = useRouter()
  const careZones = ['自理区', '半护区', '全护区', '失智区']
  const loading = ref(false)
  const saving = ref(false)
  const beds = ref<any[]>([])
  const residents = ref<any[]>([])
  const statistics = ref({ total: 0, available: 0, occupied: 0, maintenance: 0 })
  const pagination = ref({ current: 1, size: 20, total: 0 })
  const filters = ref({ careZone: '', status: '', roomBedNo: '' })
  const assignVisible = ref(false)
  const selectedBed = ref<any>(null)
  const selectedResidentId = ref<number | null>(null)
  const bedDialogVisible = ref(false)
  const bedForm = ref<any>({ id: null, roomBedNo: '', careZone: '', floor: 1, roomNo: '', roomType: '双人房' })

  const statCards = computed(() => [
    { label: '全部床位', value: statistics.value.total },
    { label: '空闲床位', value: statistics.value.available },
    { label: '已入住', value: statistics.value.occupied },
    { label: '维护中', value: statistics.value.maintenance }
  ])

  function statusText(status: string) {
    return ({ AVAILABLE: '空闲', OCCUPIED: '已入住', MAINTENANCE: '维护中' } as Record<string, string>)[status] || status
  }

  function statusType(status: string): 'success' | 'warning' | 'info' {
    return ({ AVAILABLE: 'success', OCCUPIED: 'warning', MAINTENANCE: 'info' } as Record<string, any>)[status] || 'info'
  }

  async function loadData() {
    loading.value = true
    try {
      const [page, stats]: any[] = await Promise.all([
        getBedList({
          current: pagination.value.current,
          size: pagination.value.size,
          careZone: filters.value.careZone || undefined,
          bedNo: filters.value.roomBedNo || undefined,
          status: filters.value.status || undefined
        }),
        getBedStatistics(filters.value.careZone || undefined)
      ])
      beds.value = page?.records || []
      pagination.value.total = page?.total || 0
      statistics.value = stats || statistics.value
    } catch (error: any) {
      ElMessage.error(error?.message || '获取床位信息失败')
    } finally {
      loading.value = false
    }
  }

  async function loadResidents() {
    const page: any = await fetchResidentList({ current: 1, size: 1000 })
    residents.value = page?.records || []
  }

  function reload() {
    pagination.value.current = 1
    loadData()
  }

  function resetFilters() {
    filters.value = { careZone: '', status: '', roomBedNo: '' }
    reload()
  }

  async function openAssign(row: any) {
    selectedBed.value = row
    selectedResidentId.value = null
    try {
      if (!residents.value.length) await loadResidents()
      assignVisible.value = true
    } catch {
      ElMessage.error('获取在住老人失败')
    }
  }

  async function confirmAssign() {
    const resident = residents.value.find((item) => item.id === selectedResidentId.value)
    if (!resident || !selectedBed.value) {
      ElMessage.warning('请选择需要分配床位的老人')
      return
    }
    saving.value = true
    try {
      await assignBed({
        bedId: selectedBed.value.id,
        residentId: resident.id,
        residentName: resident.name
      })
      ElMessage.success('床位分配成功')
      assignVisible.value = false
      residents.value = []
      await loadData()
    } catch (error: any) {
      ElMessage.error(error?.message || '床位分配失败')
    } finally {
      saving.value = false
    }
  }

  async function toggleMaintenance(row: any) {
    const maintenance = row.status !== 'MAINTENANCE'
    const action = maintenance ? '设为维护' : '结束维护'
    try {
      await ElMessageBox.confirm(`确定将床位 ${row.roomBedNo} ${action}吗？`, action, {
        type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
      })
      await setBedMaintenance(row.id, maintenance)
      ElMessage.success(`${action}成功`)
      await loadData()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error(`${action}失败`)
    }
  }

  function viewResident(row: any) {
    router.push({ name: 'ResidentDetail', params: { id: row.residentId } })
  }

  function openBedDialog(row?: any) {
    bedForm.value = row ? { id: row.id, roomBedNo: row.roomBedNo, careZone: row.careZone, floor: row.floor, roomNo: row.roomNo, roomType: row.roomType } : { id: null, roomBedNo: '', careZone: '', floor: 1, roomNo: '', roomType: '双人房' }
    bedDialogVisible.value = true
  }

  async function saveBed() {
    if (!bedForm.value.roomBedNo || !bedForm.value.careZone || !bedForm.value.floor || !bedForm.value.roomNo || !bedForm.value.roomType) return ElMessage.warning('请完整填写床位号、护理区、楼层、房间号和房型')
    saving.value = true
    try {
      if (bedForm.value.id) await updateBed(bedForm.value)
      else await addBed(bedForm.value)
      ElMessage.success('保存成功'); bedDialogVisible.value = false; await loadData()
    } catch (error: any) { ElMessage.error(error?.message || '保存失败') } finally { saving.value = false }
  }

  async function removeBed(row: any) {
    try {
      await ElMessageBox.confirm(`确定删除床位 ${row.roomBedNo} 吗？`, '删除床位', { type: 'warning' })
      await deleteBed(row.id); ElMessage.success('删除成功'); await loadData()
    } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败') }
  }

  onMounted(loadData)
</script>

<style scoped>
  .bed-page { padding: 20px; }
  .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
  @media (max-width: 900px) { .toolbar { align-items: flex-start; flex-direction: column; } }
</style>
