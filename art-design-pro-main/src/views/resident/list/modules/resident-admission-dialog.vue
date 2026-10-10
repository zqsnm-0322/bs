<template>
  <ElDialog
    v-model="visible"
    title="入住登记"
    width="760px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <ElForm ref="formRef" :model="form" :rules="rules" label-width="100px">
      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="姓名" prop="name">
            <ElInput v-model="form.name" placeholder="请输入老人姓名" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="性别" prop="gender">
            <ElSelect v-model="form.gender" placeholder="请选择性别">
              <ElOption label="男" value="男" />
              <ElOption label="女" value="女" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="年龄" prop="age">
            <ElInputNumber v-model="form.age" :min="60" :max="120" placeholder="请输入年龄" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="房间床位" prop="roomBedNo">
            <ElSelect
              v-model="form.roomBedNo"
              filterable
              clearable
              placeholder="请选择可用床位"
              style="width: 100%"
            >
              <ElOption
                v-for="bed in availableBeds"
                :key="bed.id"
                :label="`${bed.careZone} / ${bed.roomBedNo} / ${bed.roomType || '未设置房型'}`"
                :value="bed.roomBedNo"
              />
            </ElSelect>
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="证件类型">
            <ElSelect v-model="form.idType" style="width: 100%">
              <ElOption label="居民身份证" value="IDCARD" />
              <ElOption label="护照" value="PASSPORT" />
              <ElOption label="其他" value="OTHER" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="证件号码">
            <ElInput v-model="form.idNumber" placeholder="请输入证件号码" />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="入住类型">
            <ElSelect v-model="form.admissionType" style="width: 100%">
              <ElOption label="长期" value="长期" />
              <ElOption label="短期" value="短期" />
              <ElOption label="托养" value="托养" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="月费标准">
            <ElInputNumber v-model="form.feeStandard" :min="0" :precision="2" style="width: 100%" />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="合同开始">
            <ElDatePicker
              v-model="form.contractStartDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="合同结束">
            <ElDatePicker
              v-model="form.contractEndDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElFormItem label="籍贯/住址">
        <ElInput v-model="form.originAddress" placeholder="请输入籍贯或常住地址" />
      </ElFormItem>

      <ElFormItem label="健康状况">
        <ElInput v-model="form.healthStatus" type="textarea" placeholder="请输入健康状况描述" />
      </ElFormItem>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="护理区" prop="careZone">
            <ElSelect v-model="form.careZone" placeholder="请选择护理区">
              <ElOption label="自理区" value="自理区" />
              <ElOption label="半护区" value="半护区" />
              <ElOption label="全护区" value="全护区" />
              <ElOption label="失智区" value="失智区" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="健康等级" prop="healthLevel">
            <ElSelect v-model="form.healthLevel" placeholder="请选择健康等级">
              <ElOption label="自理" value="自理" />
              <ElOption label="半护" value="半护" />
              <ElOption label="全护" value="全护" />
              <ElOption label="失智" value="失智" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="照护主管" prop="careManager">
            <ElInput v-model="form.careManager" placeholder="请输入照护主管姓名" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="护理员" prop="caregiverName">
            <ElInput v-model="form.caregiverName" placeholder="请输入护理员姓名" />
          </ElFormItem>
        </ElCol>
      </ElRow>

      <ElFormItem label="慢性疾病">
        <ElInput v-model="form.chronicDiseases" type="textarea" placeholder="请输入慢性疾病历史" />
      </ElFormItem>

      <ElFormItem label="饮食禁忌">
        <ElInput v-model="form.dietRestriction" placeholder="请输入饮食禁忌" />
      </ElFormItem>

      <ElFormItem label="过敏史">
        <ElInput v-model="form.allergyHistory" type="textarea" placeholder="请输入过敏史" />
      </ElFormItem>

      <ElFormItem label="紧急联系人">
        <ElInput v-model="form.emergencyContactName" placeholder="请输入紧急联系人姓名" />
      </ElFormItem>

      <ElFormItem label="紧急联系电话">
        <ElInput v-model="form.emergencyContactPhone" placeholder="请输入紧急联系电话" />
      </ElFormItem>

      <ElRow :gutter="20">
        <ElCol :span="12">
          <ElFormItem label="联系人关系">
            <ElInput v-model="form.emergencyContactRelation" placeholder="如：子女、配偶" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="保险类型">
            <ElInput v-model="form.insuranceType" placeholder="请输入保险类型" />
          </ElFormItem>
        </ElCol>
      </ElRow>
    </ElForm>
    <template #footer>
      <ElButton @click="visible = false">取消</ElButton>
      <ElButton type="primary" @click="handleSubmit" :loading="loading">确定入住</ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import { addResident } from '@/api/resident'
  import { getAvailableBeds } from '@/api/bed'

  const props = defineProps<{ modelValue: boolean }>()
  const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()

  const visible = ref(props.modelValue)
  const loading = ref(false)
  const formRef = ref()
  const availableBeds = ref<any[]>([])

  const form = ref({
    name: '',
    gender: '',
    age: null as any,
    roomBedNo: '',
    careZone: '',
    healthLevel: '自理',
    careManager: '',
    caregiverName: '',
    chronicDiseases: '',
    dietRestriction: '',
    allergyHistory: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
    emergencyContactRelation: '',
    insuranceType: '',
    idType: 'IDCARD',
    idNumber: '',
    originAddress: '',
    admissionType: '长期',
    healthStatus: '',
    feeStandard: null as number | null,
    contractStartDate: '',
    contractEndDate: ''
  })

  const rules = {
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    age: [{ required: true, message: '请输入60至120岁的年龄', trigger: 'change' }],
    careZone: [{ required: true, message: '请选择护理区', trigger: 'change' }]
  }

  watch(
    () => props.modelValue,
    (val) => {
      visible.value = val
      if (val) loadAvailableBeds()
    }
  )

  watch(
    () => form.value.careZone,
    () => {
      form.value.roomBedNo = ''
      if (visible.value) loadAvailableBeds()
    }
  )

  watch(visible, (val) => {
    emit('update:modelValue', val)
  })

  function handleClose() {
    formRef.value?.resetFields()
    form.value = {
      name: '',
      gender: '',
      age: null,
      roomBedNo: '',
      careZone: '',
      healthLevel: '自理',
      careManager: '',
      caregiverName: '',
      chronicDiseases: '',
      dietRestriction: '',
      allergyHistory: '',
      emergencyContactName: '',
      emergencyContactPhone: '',
      emergencyContactRelation: '',
      insuranceType: '',
      idType: 'IDCARD',
      idNumber: '',
      originAddress: '',
      admissionType: '长期',
      healthStatus: '',
      feeStandard: null,
      contractStartDate: '',
      contractEndDate: ''
    }
  }

  async function loadAvailableBeds() {
    try {
      const result: any = await getAvailableBeds(
        form.value.careZone ? { careZone: form.value.careZone } : undefined
      )
      availableBeds.value = result || []
    } catch {
      availableBeds.value = []
      ElMessage.error('获取可用床位失败')
    }
  }

  async function handleSubmit() {
    try {
      await formRef.value.validate()
      loading.value = true
      await addResident(form.value)
      ElMessage.success('入住登记成功')
      emit('success')
      handleClose()
    } catch (error: any) {
      if (error?.message) {
        ElMessage.error(error.message)
      }
    } finally {
      loading.value = false
    }
  }
</script>
