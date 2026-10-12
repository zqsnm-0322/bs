<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加用户' : '编辑用户'"
    width="30%"
    align-center
  >
    <ElForm ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <ElFormItem label="用户名" prop="username">
        <ElInput
          v-model="formData.username"
          placeholder="请输入用户名"
          :disabled="dialogType === 'edit'"
        />
      </ElFormItem>

      <ElFormItem label="真实姓名" prop="realName">
        <ElInput v-model="formData.realName" placeholder="请输入真实姓名" />
      </ElFormItem>

      <ElFormItem label="密码" prop="password" v-if="dialogType === 'add'">
        <ElInput
          v-model="formData.password"
          type="password"
          placeholder="请输入密码"
          show-password
        />
      </ElFormItem>

      <ElFormItem label="手机号" prop="phone">
        <ElInput v-model="formData.phone" placeholder="请输入手机号" />
      </ElFormItem>

      <ElFormItem label="角色" prop="role">
        <ElSelect v-model="formData.role" placeholder="请选择角色">
          <ElOption label="管理员" value="ADMIN" />
          <ElOption label="院长" value="DIRECTOR" />
          <ElOption label="护理员" value="CAREGIVER" />
          <ElOption label="照护员" value="CAREGIVER" />
          <ElOption label="照护主管（历史角色）" value="CARE_MANAGER" />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="负责护理区" prop="careZone">
        <ElSelect v-model="formData.careZone" placeholder="请选择护理区" clearable>
          <ElOption label="自理区" value="自理区" />
          <ElOption label="半护区" value="半护区" />
          <ElOption label="全护区" value="全护区" />
          <ElOption label="失智区" value="失智区" />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="员工工号" prop="employeeNo">
        <ElInput v-model="formData.employeeNo" placeholder="请输入员工工号" />
      </ElFormItem>

      <ElFormItem label="入职日期" prop="hireDate">
        <ElDatePicker v-model="formData.hireDate" value-format="YYYY-MM-DD" style="width: 100%" />
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false" :disabled="loading">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="loading">
          {{ loading ? '提交中...' : '提交' }}
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { addUser, updateUser } from '@/api/system-manage'
  import type { FormInstance, FormRules } from 'element-plus'
  import { ElMessage } from 'element-plus'

  interface Props {
    visible: boolean
    type: string
    userData?: any
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const loading = ref(false)

  // 对话框显示控制
  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const dialogType = computed(() => props.type)

  // 表单实例
  const formRef = ref<FormInstance>()

  // 表单数据
  const formData = reactive({
    id: undefined as number | undefined,
    username: '',
    realName: '',
    password: '',
    phone: '',
    role: '',
    careZone: '',
    employeeNo: '',
    hireDate: ''
  })

  // 表单验证规则
  const rules: FormRules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
    ],
    realName: [
      { required: true, message: '请输入真实姓名', trigger: 'blur' },
      { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
    ],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }],
    employeeNo: [{ required: true, message: '请输入员工工号', trigger: 'blur' }]
  }

  /**
   * 初始化表单数据
   * 根据对话框类型（新增/编辑）填充表单
   */
  const initFormData = () => {
    const isEdit = props.type === 'edit' && props.userData
    const row = props.userData

    if (isEdit && row) {
      Object.assign(formData, {
        id: row.id,
        username: row.username || '',
        realName: row.realName || '',
        password: '', // 编辑时不显示密码
        phone: row.phone || '',
        role: row.role || '',
        careZone: row.careZone || '',
        employeeNo: row.employeeNo || '',
        hireDate: row.hireDate || ''
      })
    } else {
      // 新增时重置表单
      Object.assign(formData, {
        id: undefined,
        username: '',
        realName: '',
        password: '',
        phone: '',
        role: '',
        careZone: '',
        employeeNo: '',
        hireDate: ''
      })
    }
  }

  /**
   * 监听对话框状态变化
   * 当对话框打开时初始化表单数据并清除验证状态
   */
  watch(
    () => [props.visible, props.type, props.userData],
    ([visible]) => {
      if (visible) {
        initFormData()
        nextTick(() => {
          formRef.value?.clearValidate()
        })
      }
    },
    { immediate: true }
  )

  /**
   * 提交表单
   * 验证通过后调用API进行用户创建或更新
   */
  const handleSubmit = async () => {
    if (!formRef.value) return

    try {
      const valid = await formRef.value.validate()
      if (!valid) return

      loading.value = true

      const submitData = {
        ...formData,
        // 编辑时如果密码为空则不传递密码字段
        ...(dialogType.value === 'edit' && !formData.password && { password: undefined })
      }

      if (dialogType.value === 'add') {
        await addUser(submitData)
        ElMessage.success('用户添加成功')
      } else {
        await updateUser(submitData)
        ElMessage.success('用户更新成功')
      }

      dialogVisible.value = false
      emit('submit')
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error(dialogType.value === 'add' ? '添加用户失败' : '更新用户失败')
    } finally {
      loading.value = false
    }
  }
</script>

