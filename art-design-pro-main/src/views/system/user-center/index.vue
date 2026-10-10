<!-- 个人中心页面 -->
<template>
  <div class="w-full h-full p-0 bg-transparent border-none shadow-none">
    <div class="relative flex-b mt-2.5 max-md:block max-md:mt-1">
      <div class="w-112 mr-5 max-md:w-full max-md:mr-0">
        <div class="art-card-sm relative p-9 pb-6 overflow-hidden text-center">
          <img class="absolute top-0 left-0 w-full h-50 object-cover" src="@imgs/user/bg.webp" />
          <div class="relative z-10 w-20 h-20 mt-30 mx-auto cursor-pointer group" @click="handleAvatarClick" title="点击更换头像">
            <img
              class="w-full h-full object-cover border-2 border-white rounded-full"
              :src="avatarUrl"
            />
            <div class="absolute inset-0 flex items-center justify-center bg-black bg-opacity-50 rounded-full opacity-0 group-hover:opacity-100 transition-opacity">
              <ArtSvgIcon icon="ri:camera-line" class="text-white text-2xl" />
            </div>
          </div>
          <p class="mt-2 text-xs text-gray-400 group-hover:text-gray-600 transition-colors">点击更换头像</p>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="handleAvatarChange"
          />
          <h2 class="mt-5 text-xl font-normal">{{ form.realName || '未设置' }}</h2>
          <p class="mt-2 text-sm text-gray-500">ID: {{ form.userId || '未设置' }}</p>
          
          <!-- 工作台入口 -->
          <div v-if="workspaceLink" class="mt-4">
            <ElButton 
              type="primary" 
              @click="goToWorkspace"
              class="w-40"
              v-ripple
            >
              <ArtSvgIcon icon="ri:dashboard-line" class="mr-2" />
              {{ workspaceLabel }}
            </ElButton>
          </div>
        </div>
      </div>
      <div class="flex-1 overflow-hidden max-md:w-full max-md:mt-3.5">
        <div class="art-card-sm">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">基本设置</h1>

          <ElForm
            :model="form"
            class="box-border p-5 [&>.el-row_.el-form-item]:w-[calc(50%-10px)] [&>.el-row_.el-input]:w-full [&>.el-row_.el-select]:w-full"
            ref="ruleFormRef"
            :rules="rules"
            label-width="86px"
            label-position="top"
          >
            <ElRow>
              <ElFormItem label="用户ID" prop="userId">
                <ElInput v-model="form.userId" disabled />
              </ElFormItem>
              <ElFormItem label="用户名" prop="username" class="ml-5">
                <ElInput v-model="form.username" disabled />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="姓名" prop="realName">
                <ElInput v-model="form.realName" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="性别" prop="sex" class="ml-5">
                <ElSelect v-model="form.sex" placeholder="请选择性别" :disabled="!isEdit">
                  <ElOption
                    v-for="item in options"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="昵称" prop="nikeName">
                <ElInput v-model="form.nikeName" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="邮箱" prop="email" class="ml-5">
                <ElInput v-model="form.email" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="手机" prop="mobile">
                <ElInput v-model="form.mobile" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="地址" prop="address" class="ml-5">
                <ElInput v-model="form.address" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <ElFormItem label="个人介绍" prop="des" class="h-32">
              <ElInput type="textarea" :rows="4" v-model="form.des" :disabled="!isEdit" />
            </ElFormItem>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton v-if="isEdit" @click="cancelEdit">取消</ElButton>
              <ElButton type="primary" class="w-22.5" v-ripple @click="handleSaveUserInfo" :loading="saveLoading">
                {{ isEdit ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>

        <div class="art-card-sm my-5">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">更改密码</h1>

          <ElForm 
            :model="pwdForm" 
            ref="pwdFormRef"
            :rules="pwdRules"
            class="box-border p-5" 
            label-width="86px" 
            label-position="top"
          >
            <ElFormItem label="当前密码" prop="oldPassword">
              <ElInput
                v-model="pwdForm.oldPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
                placeholder="请输入当前密码"
              />
            </ElFormItem>

            <ElFormItem label="新密码" prop="newPassword">
              <ElInput
                v-model="pwdForm.newPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
                placeholder="请输入新密码（至少6位）"
              />
            </ElFormItem>

            <ElFormItem label="确认新密码" prop="confirmPassword">
              <ElInput
                v-model="pwdForm.confirmPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
                placeholder="请再次输入新密码"
              />
            </ElFormItem>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton v-if="isEditPwd" @click="cancelEditPwd">取消</ElButton>
              <ElButton type="primary" class="w-22.5" v-ripple @click="handleChangePassword" :loading="pwdLoading">
                {{ isEditPwd ? '保存' : '修改密码' }}
              </ElButton>
            </div>
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useUserStore } from '@/store/modules/user'
  import type { FormInstance, FormRules } from 'element-plus'
  import defaultAvatar from '@imgs/user/avatar.webp'
  import { fetchCurrentUserDetail, updateCurrentUser, changePassword, uploadAvatar } from '@/api/system-manage'
  import { useRouter } from 'vue-router'

  defineOptions({ name: 'UserCenter' })

  const router = useRouter()
  const userStore = useUserStore()
  const userInfo = computed(() => userStore.getUserInfo)
  
  /**
   * 根据用户角色计算工作台链接和标签
   */
  const workspaceLink = computed(() => {
    const role = userInfo.value?.role
    if (role === 'CARE_MANAGER') {
      return '/resident/list'
    } else if (role === 'CAREGIVER') {
      return '/resident/list'
    }
    return null
  })
  
  const workspaceLabel = computed(() => {
    const role = userInfo.value?.role
    if (role === 'CARE_MANAGER') {
      return '照护主管工作台'
    } else if (role === 'CAREGIVER') {
      return '护理员工作台'
    }
    return ''
  })
  
  /**
   * 跳转到工作台
   */
  const goToWorkspace = () => {
    if (workspaceLink.value) {
      router.push(workspaceLink.value)
    }
  }

  const isEdit = ref(false)
  const isEditPwd = ref(false)
  const saveLoading = ref(false)
  const pwdLoading = ref(false)
  const ruleFormRef = ref<FormInstance>()
  const pwdFormRef = ref<FormInstance>()

  /**
   * 用户信息表单
   */
  const form = reactive({
    userId: '',
    username: '',
    realName: '',
    nikeName: '',
    email: '',
    mobile: '',
    address: '',
    sex: '',
    des: ''
  })

  // 保存原始数据用于取消编辑
  let originalFormData: any = {}

  /**
   * 密码修改表单
   */
  const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  /**
   * 表单验证规则
   */
  const rules = reactive<FormRules>({
    realName: [
      { required: true, message: '请输入姓名', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    nikeName: [
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    email: [
      { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    mobile: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ]
  })

  /**
   * 密码表单验证规则
   */
  const validateConfirmPassword = (rule: any, value: any, callback: any) => {
    if (value === '') {
      callback(new Error('请再次输入新密码'))
    } else if (value !== pwdForm.newPassword) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const pwdRules = reactive<FormRules>({
    oldPassword: [
      { required: true, message: '请输入当前密码', trigger: 'blur' }
    ],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, validator: validateConfirmPassword, trigger: 'blur' }
    ]
  })

  /**
   * 性别选项
   */
  const options = [
    { value: '1', label: '男' },
    { value: '2', label: '女' }
  ]

  /**
   * 头像相关
   */
  const avatarInput = ref<HTMLInputElement>()
  const avatarUrl = ref(defaultAvatar)

  /**
   * 点击头像触发文件选择
   */
  const handleAvatarClick = () => {
    avatarInput.value?.click()
  }

  /**
   * 处理头像上传
   */
  const handleAvatarChange = async (event: Event) => {
    const target = event.target as HTMLInputElement
    const file = target.files?.[0]
    if (file) {
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.error('请选择图片文件')
        return
      }
      // 验证文件大小（限制为2MB）
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过2MB')
        return
      }
      
      try {
        // 读取并预览图片
        const reader = new FileReader()
        reader.onload = (e) => {
          avatarUrl.value = e.target?.result as string
        }
        reader.readAsDataURL(file)

        // 上传到服务器
        const result = await uploadAvatar(file)
        const uploadedAvatar = result?.avatar || avatarUrl.value
        avatarUrl.value = `${uploadedAvatar}${uploadedAvatar.includes('?') ? '&' : '?'}t=${Date.now()}`
        userStore.setUserInfo({
          ...userInfo.value,
          avatar: avatarUrl.value
        } as Api.Auth.UserInfo)
        ElMessage.success('头像上传成功')
        // 重新加载用户信息以同步最新头像
        await loadUserInfo()
      } catch (error) {
        ElMessage.error('头像上传失败')
        console.error('上传头像失败:', error)
      } finally {
        target.value = ''
      }
    }
  }

  /**
   * 加载用户信息
   */
  const loadUserInfo = async () => {
    try {
      const res = await fetchCurrentUserDetail()
      if (res) {
        Object.assign(form, {
          userId: res.id || res.userId || '',
          username: res.username || '',
          realName: res.realName || res.name || '',
          nikeName: res.nikeName || res.nickname || '',
          email: res.email || '',
          mobile: res.mobile || res.phone || '',
          address: res.address || '',
          sex: res.sex ? String(res.sex) : '',
          des: res.des || res.description || ''
        })
        
        // 保存原始数据
        originalFormData = { ...form }
        
        // 如果有头像URL，更新头像
        if (res.avatar) {
          avatarUrl.value = res.avatar
          userStore.setUserInfo({
            ...userInfo.value,
            avatar: res.avatar
          } as Api.Auth.UserInfo)
        }
      }
    } catch (error) {
      console.error('加载用户信息失败:', error)
      ElMessage.error('加载用户信息失败')
    }
  }

  onMounted(() => {
    loadUserInfo()
  })

  /**
   * 保存用户信息
   */
  const handleSaveUserInfo = async () => {
    if (!isEdit.value) {
      isEdit.value = true
      return
    }

    // 验证表单
    if (!ruleFormRef.value) return
    
    try {
      await ruleFormRef.value.validate()
      saveLoading.value = true
      
      // 调用API保存
      await updateCurrentUser({
        realName: form.realName,
        nikeName: form.nikeName,
        email: form.email,
        mobile: form.mobile,
        address: form.address,
        sex: form.sex,
        des: form.des
      })
      
      ElMessage.success('保存成功')
      isEdit.value = false
      originalFormData = { ...form }
      
      // 更新store中的用户信息
      await loadUserInfo()
    } catch (error: any) {
      if (error !== false) { // 表单验证失败时error为false
        console.error('保存失败:', error)
        ElMessage.error('保存失败')
      }
    } finally {
      saveLoading.value = false
    }
  }

  /**
   * 取消编辑用户信息
   */
  const cancelEdit = () => {
    Object.assign(form, originalFormData)
    isEdit.value = false
    ruleFormRef.value?.clearValidate()
  }

  /**
   * 修改密码
   */
  const handleChangePassword = async () => {
    if (!isEditPwd.value) {
      isEditPwd.value = true
      return
    }

    // 验证表单
    if (!pwdFormRef.value) return
    
    try {
      await pwdFormRef.value.validate()
      pwdLoading.value = true
      
      // 调用API修改密码
      await changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      
      ElMessage.success('密码修改成功，请重新登录')
      isEditPwd.value = false
      
      // 清空表单
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
      
      // 延迟跳转到登录页
      setTimeout(() => {
        userStore.logOut()
      }, 1500)
    } catch (error: any) {
      if (error !== false) {
        console.error('修改密码失败:', error)
        ElMessage.error(error.message || '修改密码失败')
      }
    } finally {
      pwdLoading.value = false
    }
  }

  /**
   * 取消修改密码
   */
  const cancelEditPwd = () => {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    isEditPwd.value = false
    pwdFormRef.value?.clearValidate()
  }
</script>
