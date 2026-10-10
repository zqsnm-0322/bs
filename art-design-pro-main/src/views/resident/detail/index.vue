<template>
  <div class="resident-detail">
    <ElCard>
      <template #header>
        <ElPageHeader title="老人档案" @back="goBack">
          <template #extra>
            <ElButton v-if="!isResident" type="primary" @click="openEdit">编辑档案</ElButton>
          </template>
        </ElPageHeader>
      </template>

      <ElTabs v-model="activeTab">
        <ElTabPane label="基本信息" name="basic">
          <ElDescriptions :column="3" border>
            <ElDescriptionsItem label="入住编号">{{ resident.residentId }}</ElDescriptionsItem>
            <ElDescriptionsItem label="姓名">{{ resident.name }}</ElDescriptionsItem>
            <ElDescriptionsItem label="性别">{{ resident.gender }}</ElDescriptionsItem>
            <ElDescriptionsItem label="年龄">{{ resident.age }}岁</ElDescriptionsItem>
            <ElDescriptionsItem label="护理区">{{ resident.careZone }}</ElDescriptionsItem>
            <ElDescriptionsItem label="健康等级">{{ resident.healthLevel }}</ElDescriptionsItem>
            <ElDescriptionsItem label="房间床位">{{ resident.roomBedNo }}</ElDescriptionsItem>
            <ElDescriptionsItem label="入住类型">{{
              resident.admissionType ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="照护主管">{{
              resident.careManager ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="护理员">{{ resident.caregiverName ?? '-' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="慢性疾病">{{
              resident.chronicDiseases ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="饮食禁忌">{{
              resident.dietRestriction ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="过敏史">{{
              resident.allergyHistory ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="健康状况">{{
              resident.healthStatus ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="证件类型">{{ resident.idType ?? '-' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="证件号码">{{ resident.idNumber ?? '-' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="籍贯/住址" :span="2">{{
              resident.originAddress ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="紧急联系人">{{
              resident.emergencyContactName ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="联系电话">{{
              resident.emergencyContactPhone ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="联系人关系">{{
              resident.emergencyContactRelation ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="保险类型">{{
              resident.insuranceType ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="月费标准">{{
              resident.feeStandard == null ? '-' : `¥${resident.feeStandard}`
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="入住时间">{{
              formatTime(resident.admitTime)
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="合同开始">{{
              resident.contractStartDate ?? '-'
            }}</ElDescriptionsItem>
            <ElDescriptionsItem label="合同结束">{{
              resident.contractEndDate ?? '-'
            }}</ElDescriptionsItem>
          </ElDescriptions>
        </ElTabPane>

        <ElTabPane label="家属信息" name="family">
          <FamilyMemberTab :resident-id="residentId" />
        </ElTabPane>

        <ElTabPane label="健康检查" name="checkup">
          <HealthCheckupTab :resident-id="residentId" />
        </ElTabPane>

        <ElTabPane label="风险评估" name="risk">
          <RiskAssessmentTab :resident-id="String(residentId)" />
        </ElTabPane>

        <ElTabPane label="护理任务" name="care">
          <CareTaskTab :resident-id="residentId" />
        </ElTabPane>
      </ElTabs>
    </ElCard>

    <ElDialog v-model="editVisible" title="编辑老人档案" width="760px" :close-on-click-modal="false">
      <ElForm :model="editForm" label-width="100px">
        <ElRow :gutter="20">
          <ElCol :span="8"><ElFormItem label="姓名" required><ElInput v-model="editForm.name" /></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="性别"><ElSelect v-model="editForm.gender"><ElOption label="男" value="男" /><ElOption label="女" value="女" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="年龄"><ElInputNumber v-model="editForm.age" :min="60" :max="120" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8"><ElFormItem label="证件类型"><ElSelect v-model="editForm.idType"><ElOption label="居民身份证" value="IDCARD" /><ElOption label="护照" value="PASSPORT" /><ElOption label="其他" value="OTHER" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="证件号码"><ElInput v-model="editForm.idNumber" /></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="籍贯/住址"><ElInput v-model="editForm.originAddress" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="护理区"><ElSelect v-model="editForm.careZone" style="width:100%"><ElOption v-for="item in careZones" :key="item" :label="item" :value="item" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="健康等级"><ElSelect v-model="editForm.healthLevel" style="width:100%"><ElOption v-for="item in healthLevels" :key="item" :label="item" :value="item" /></ElSelect></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="照护主管"><ElInput v-model="editForm.careManager" /></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="护理员"><ElInput v-model="editForm.caregiverName" /></ElFormItem></ElCol>
        </ElRow>
        <ElFormItem label="健康状况"><ElInput v-model="editForm.healthStatus" type="textarea" :rows="2" /></ElFormItem>
        <ElFormItem label="慢性疾病"><ElInput v-model="editForm.chronicDiseases" type="textarea" :rows="2" /></ElFormItem>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="饮食禁忌"><ElInput v-model="editForm.dietRestriction" /></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="过敏史"><ElInput v-model="editForm.allergyHistory" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="紧急联系人"><ElInput v-model="editForm.emergencyContactName" /></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="联系电话"><ElInput v-model="editForm.emergencyContactPhone" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="12"><ElFormItem label="联系人关系"><ElInput v-model="editForm.emergencyContactRelation" /></ElFormItem></ElCol>
          <ElCol :span="12"><ElFormItem label="保险类型"><ElInput v-model="editForm.insuranceType" /></ElFormItem></ElCol>
        </ElRow>
        <ElRow :gutter="20">
          <ElCol :span="8"><ElFormItem label="入住类型"><ElSelect v-model="editForm.admissionType"><ElOption label="长期" value="长期" /><ElOption label="短期" value="短期" /><ElOption label="托养" value="托养" /></ElSelect></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="月费标准"><ElInputNumber v-model="editForm.feeStandard" :min="0" :precision="2" /></ElFormItem></ElCol>
          <ElCol :span="8"><ElFormItem label="合同开始"><ElDatePicker v-model="editForm.contractStartDate" value-format="YYYY-MM-DD" /></ElFormItem></ElCol>
        </ElRow>
        <ElFormItem label="合同结束"><ElDatePicker v-model="editForm.contractEndDate" value-format="YYYY-MM-DD" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveResident">保存档案</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { fetchResidentDetail, updateResident } from '@/api/resident'
  import { useUserStore } from '@/store/modules/user'
  import FamilyMemberTab from './modules/family-tab.vue'
  import HealthCheckupTab from './modules/health-checkup-tab.vue'
  import RiskAssessmentTab from './modules/risk-assessment-tab.vue'
  import CareTaskTab from './modules/care-task-tab.vue'

  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const currentUser = userStore.getUserInfo as any
  const isResident = currentUser.role === 'RESIDENT'
  const residentId = Number(route.params.id || currentUser.id || currentUser.userId)
  const resident = ref<any>({})
  const activeTab = ref('basic')
  const editVisible = ref(false)
  const saving = ref(false)
  const careZones = ['自理区', '半护区', '全护区', '失智区']
  const healthLevels = ['自理', '半护', '全护', '失智']
  const editForm = ref<any>({})

  async function loadResident() {
    try {
      resident.value = await fetchResidentDetail(residentId)
    } catch (error) {
      ElMessage.error('获取老人详情失败')
    }
  }

  onMounted(loadResident)

  function openEdit() {
    editForm.value = {
      id: resident.value.id,
      name: resident.value.name || '',
      gender: resident.value.gender || '',
      age: resident.value.age,
      idType: resident.value.idType || 'IDCARD',
      idNumber: resident.value.idNumber || '',
      originAddress: resident.value.originAddress || '',
      careZone: resident.value.careZone || '',
      healthLevel: resident.value.healthLevel || '',
      careManager: resident.value.careManager || '',
      caregiverName: resident.value.caregiverName || '',
      healthStatus: resident.value.healthStatus || '',
      chronicDiseases: resident.value.chronicDiseases || '',
      dietRestriction: resident.value.dietRestriction || '',
      allergyHistory: resident.value.allergyHistory || '',
      emergencyContactName: resident.value.emergencyContactName || '',
      emergencyContactPhone: resident.value.emergencyContactPhone || '',
      emergencyContactRelation: resident.value.emergencyContactRelation || '',
      insuranceType: resident.value.insuranceType || '',
      admissionType: resident.value.admissionType || '',
      feeStandard: resident.value.feeStandard,
      contractStartDate: resident.value.contractStartDate || '',
      contractEndDate: resident.value.contractEndDate || ''
    }
    editVisible.value = true
  }

  async function saveResident() {
    if (!editForm.value.name?.trim()) {
      ElMessage.warning('老人姓名不能为空')
      return
    }
    saving.value = true
    try {
      await updateResident(editForm.value)
      ElMessage.success('老人档案已更新')
      editVisible.value = false
      await loadResident()
    } catch (error: any) {
      ElMessage.error(error?.message || '档案更新失败')
    } finally {
      saving.value = false
    }
  }

  const goBack = () => router.back()
  const formatTime = (t: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-')
</script>
