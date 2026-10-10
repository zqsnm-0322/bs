<template>
  <div class="checkup-tab">
    <div class="header">
      <span>健康检查记录</span>
      <ElButton type="primary" size="small" icon="Plus" @click="showAddDialog">添加记录</ElButton>
    </div>
    <ElTable :data="checkupList" border stripe>
      <ElTableColumn prop="checkDate" label="检查日期" width="120" />
      <ElTableColumn prop="bloodPressureSystolic" label="收缩压" width="80">
        <template #default="{ row }">{{ row.bloodPressureSystolic ?? '-' }}</template>
      </ElTableColumn>
      <ElTableColumn prop="bloodPressureDiastolic" label="舒张压" width="80">
        <template #default="{ row }">{{ row.bloodPressureDiastolic ?? '-' }}</template>
      </ElTableColumn>
      <ElTableColumn prop="heartRate" label="心率" width="70">
        <template #default="{ row }">{{ row.heartRate ?? '-' }}</template>
      </ElTableColumn>
      <ElTableColumn prop="temperature" label="体温" width="70">
        <template #default="{ row }">{{ row.temperature ?? '-' }}</template>
      </ElTableColumn>
      <ElTableColumn prop="bmi" label="BMI" width="70">
        <template #default="{ row }">{{ row.bmi ?? '-' }}</template>
      </ElTableColumn>
      <ElTableColumn prop="checkedBy" label="检查人员" width="100" />
      <ElTableColumn prop="conclusion" label="检查结论" />
      <ElTableColumn label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" link size="small" @click="showEditDialog(row)">编辑</ElButton>
          <ElButton type="danger" link size="small" @click="handleDelete(row.id)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑健康检查记录' : '添加健康检查记录'" width="720px">
      <ElForm :model="form" label-width="120px">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElFormItem label="检查日期" required>
              <ElDatePicker
                v-model="form.checkDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="12">
            <ElFormItem label="检查人员">
              <ElInput v-model="form.checkedBy" placeholder="请输入检查人员" />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8">
            <ElFormItem label="体温(℃)">
              <ElInputNumber v-model="form.temperature" :min="30" :max="45" :precision="1" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="血氧(%)">
              <ElInputNumber v-model="form.bloodOxygen" :min="0" :max="100" :precision="1" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="体重(kg)">
              <ElInputNumber v-model="form.weight" :min="0" :max="300" :precision="1" />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8">
            <ElFormItem label="身高(cm)">
              <ElInputNumber v-model="form.height" :min="0" :max="250" :precision="1" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="空腹血糖">
              <ElInputNumber v-model="form.bloodSugarFasting" :min="0" :precision="2" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="餐后血糖">
              <ElInputNumber v-model="form.bloodSugarPostprandial" :min="0" :precision="2" />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8">
            <ElFormItem label="收缩压">
              <ElInputNumber v-model="form.bloodPressureSystolic" :min="0" :max="300" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="舒张压">
              <ElInputNumber v-model="form.bloodPressureDiastolic" :min="0" :max="200" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="8">
            <ElFormItem label="心率">
              <ElInputNumber v-model="form.heartRate" :min="0" :max="200" />
            </ElFormItem>
          </ElCol>
        </ElRow>
        <ElFormItem label="检查结论">
          <ElInput
            v-model="form.conclusion"
            type="textarea"
            :rows="3"
            placeholder="请输入检查结论"
          />
        </ElFormItem>
        <ElFormItem label="护理建议">
          <ElInput
            v-model="form.suggestion"
            type="textarea"
            :rows="2"
            placeholder="请输入护理建议"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="loading">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    fetchHealthCheckupList,
    addHealthCheckup,
    updateHealthCheckup,
    deleteHealthCheckup
  } from '@/api/health-checkup'

  const props = defineProps<{ residentId: number }>()
  const checkupList = ref<any[]>([])
  const dialogVisible = ref(false)
  const editingId = ref<number | null>(null)
  const loading = ref(false)
  const form = ref({
    checkDate: null as any,
    checkedBy: '',
    bloodPressureSystolic: null as any,
    bloodPressureDiastolic: null as any,
    heartRate: null as any,
    temperature: null as any,
    bloodOxygen: null as any,
    weight: null as any,
    height: null as any,
    bloodSugarFasting: null as any,
    bloodSugarPostprandial: null as any,
    conclusion: '',
    suggestion: ''
  })

  function emptyForm() {
    return {
      checkDate: null as any,
      checkedBy: '',
      bloodPressureSystolic: null as any,
      bloodPressureDiastolic: null as any,
      heartRate: null as any,
      temperature: null as any,
      bloodOxygen: null as any,
      weight: null as any,
      height: null as any,
      bloodSugarFasting: null as any,
      bloodSugarPostprandial: null as any,
      conclusion: '',
      suggestion: ''
    }
  }

  watch(
    () => props.residentId,
    async (id) => {
      if (id) {
        await loadCheckup()
      }
    },
    { immediate: true }
  )

  async function loadCheckup() {
    try {
      const res: any = await fetchHealthCheckupList({ residentId: props.residentId })
      checkupList.value = res?.records || []
    } catch {
      ElMessage.error('获取健康检查记录失败')
    }
  }

  function showAddDialog() {
    editingId.value = null
    form.value = emptyForm()
    dialogVisible.value = true
  }

  function showEditDialog(row: any) {
    editingId.value = row.id
    form.value = {
      checkDate: row.checkDate,
      checkedBy: row.checkedBy || '',
      bloodPressureSystolic: row.bloodPressureSystolic,
      bloodPressureDiastolic: row.bloodPressureDiastolic,
      heartRate: row.heartRate,
      temperature: row.temperature,
      bloodOxygen: row.bloodOxygen,
      weight: row.weight,
      height: row.height,
      bloodSugarFasting: row.bloodSugarFasting,
      bloodSugarPostprandial: row.bloodSugarPostprandial,
      conclusion: row.conclusion || '',
      suggestion: row.suggestion || ''
    }
    dialogVisible.value = true
  }

  async function handleSubmit() {
    if (!form.value.checkDate) {
      ElMessage.warning('请选择检查日期')
      return
    }
    loading.value = true
    try {
      const payload = { ...form.value, residentId: props.residentId }
      if (editingId.value) {
        await updateHealthCheckup({ ...payload, id: editingId.value })
        ElMessage.success('健康检查记录已更新')
      } else {
        await addHealthCheckup(payload)
        ElMessage.success('健康检查记录已添加')
      }
      dialogVisible.value = false
      await loadCheckup()
    } catch {
      ElMessage.error('添加失败')
    } finally {
      loading.value = false
    }
  }

  async function handleDelete(id: number) {
    try {
      await ElMessageBox.confirm('确定删除这条健康检查记录吗？', '删除确认', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      })
      await deleteHealthCheckup(id)
      ElMessage.success('删除成功')
      await loadCheckup()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败')
    }
  }
</script>

<style scoped>
  .checkup-tab {
    padding: 10px;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
</style>
