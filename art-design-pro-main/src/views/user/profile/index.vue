<template>
  <div class="user-profile-page">
    <ElRow :gutter="20">
      <!-- 左侧：个人信息卡片 -->
      <ElCol :span="8">
        <ElCard shadow="hover" class="profile-card">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>
          
          <div class="profile-info" v-loading="loading">
            <div class="avatar-section">
              <ElUpload
                class="avatar-uploader"
                :show-file-list="false"
                :http-request="handleAvatarUpload"
                :before-upload="beforeAvatarUpload"
                accept="image/jpeg,image/png,image/gif,image/webp"
                :disabled="avatarUploading"
              >
                <div class="avatar-wrapper" v-loading="avatarUploading">
                  <ElAvatar :size="100" :src="userProfile.avatar || defaultAvatar" />
                  <div class="avatar-mask">
                    <ElIcon><Camera /></ElIcon>
                    <span>更换头像</span>
                  </div>
                </div>
              </ElUpload>
              <div class="avatar-tip">点击头像上传，支持 JPG、PNG、GIF、WEBP，最大 2MB</div>
              <h3 class="mt-4">{{ userProfile.realName || '加载中...' }}</h3>
              <ElTag :type="getRoleType(userProfile.role)" class="mt-2">
                {{ getRoleText(userProfile.role) }}
              </ElTag>
            </div>

            <ElDivider />

            <div class="info-list">
              <div class="info-item">
                <span class="label">用户名：</span>
                <span class="value">{{ userProfile.username || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">护理区：</span>
                <span class="value">{{ userProfile.workZone || userProfile.careZone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">电话：</span>
                <span class="value">{{ userProfile.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱：</span>
                <span class="value">{{ userProfile.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册时间：</span>
                <span class="value">{{ userProfile.createTime || '-' }}</span>
              </div>
            </div>

            <ElDivider />

            <div class="action-buttons">
              <ElButton type="primary" @click="showEditDialog" block>编辑信息</ElButton>
              <ElButton type="warning" @click="showPasswordDialog" block class="mt-2">修改密码</ElButton>
            </div>
          </div>
        </ElCard>
      </ElCol>

      <!-- 右侧：统计信息 -->
      <ElCol :span="16">
        <ElCard shadow="hover" class="statistics-card">
          <template #header>
            <div class="card-header">
              <span>工作统计</span>
            </div>
          </template>
          
          <div class="statistics-content">
            <ElEmpty description="统计功能开发中..." />
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <!-- 编辑信息对话框 -->
    <ElDialog v-model="editDialogVisible" title="编辑个人信息" width="500px">
      <ElForm :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <ElFormItem label="真实姓名" prop="realName">
          <ElInput v-model="editForm.realName" placeholder="请输入真实姓名" />
        </ElFormItem>
        <ElFormItem label="电话" prop="phone">
          <ElInput v-model="editForm.phone" placeholder="请输入电话号码" />
        </ElFormItem>
        <ElFormItem label="邮箱" prop="email">
          <ElInput v-model="editForm.email" placeholder="请输入邮箱地址" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSaveProfile" :loading="saving">保存</ElButton>
      </template>
    </ElDialog>

    <!-- 修改密码对话框 -->
    <ElDialog v-model="passwordDialogVisible" title="修改密码" width="500px">
      <ElForm :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <ElFormItem label="旧密码" prop="oldPassword">
          <ElInput 
            v-model="passwordForm.oldPassword" 
            type="password" 
            placeholder="请输入旧密码"
            show-password
          />
        </ElFormItem>
        <ElFormItem label="新密码" prop="newPassword">
          <ElInput 
            v-model="passwordForm.newPassword" 
            type="password" 
            placeholder="请输入新密码"
            show-password
          />
        </ElFormItem>
        <ElFormItem label="确认密码" prop="confirmPassword">
          <ElInput 
            v-model="passwordForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码"
            show-password
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="passwordDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleChangePassword" :loading="passwordSaving">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { ElMessage, FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
  import { Camera } from '@element-plus/icons-vue'
  import {
    getUserProfile,
    updateUserProfile,
    changePassword,
    uploadProfileAvatar,
    UserProfile
  } from '@/api/user-profile'

  defineOptions({ name: 'UserProfile' })

  const loading = ref(false)
  const saving = ref(false)
  const passwordSaving = ref(false)
  const avatarUploading = ref(false)
  const userProfile = ref<UserProfile>({} as UserProfile)
  const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

  const beforeAvatarUpload = (file: File) => {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      ElMessage.error('仅支持 JPG、PNG、GIF、WEBP 图片')
      return false
    }
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.error('头像图片不能超过 2MB')
      return false
    }
    return true
  }

  const handleAvatarUpload = async (options: UploadRequestOptions) => {
    try {
      avatarUploading.value = true
      const result = await uploadProfileAvatar(options.file)
      userProfile.value.avatar = `${result.avatar}?t=${Date.now()}`
      options.onSuccess(result)
      ElMessage.success('头像上传成功')
    } catch (error: any) {
      options.onError(error)
      ElMessage.error(error.message || '头像上传失败')
    } finally {
      avatarUploading.value = false
    }
  }

  // 编辑信息对话框
  const editDialogVisible = ref(false)
  const editFormRef = ref<FormInstance>()
  const editForm = reactive({
    realName: '',
    phone: '',
    email: ''
  })

  const editRules: FormRules = {
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    phone: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    email: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ]
  }

  // 修改密码对话框
  const passwordDialogVisible = ref(false)
  const passwordFormRef = ref<FormInstance>()
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  const validateConfirmPassword = (rule: any, value: any, callback: any) => {
    if (value !== passwordForm.newPassword) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const passwordRules: FormRules = {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请再次输入新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }

  // 加载个人信息
  const loadProfile = async () => {
    try {
      loading.value = true
      const res = await getUserProfile()
      userProfile.value = res
    } catch (error) {
      console.error('加载个人信息失败:', error)
      ElMessage.error('加载个人信息失败')
    } finally {
      loading.value = false
    }
  }

  // 显示编辑对话框
  const showEditDialog = () => {
    editForm.realName = userProfile.value.realName
    editForm.phone = userProfile.value.phone || ''
    editForm.email = userProfile.value.email || ''
    editDialogVisible.value = true
  }

  // 保存个人信息
  const handleSaveProfile = async () => {
    try {
      await editFormRef.value?.validate()
      
      saving.value = true
      const res = await updateUserProfile(editForm)
      userProfile.value = res
      
      ElMessage.success('个人信息更新成功')
      editDialogVisible.value = false
      
    } catch (error: any) {
      if (error !== false) {
        console.error('更新个人信息失败:', error)
        ElMessage.error(error.message || '更新失败')
      }
    } finally {
      saving.value = false
    }
  }

  // 显示修改密码对话框
  const showPasswordDialog = () => {
    passwordFormRef.value?.resetFields()
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
  }

  // 修改密码
  const handleChangePassword = async () => {
    try {
      await passwordFormRef.value?.validate()
      
      passwordSaving.value = true
      await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      
      // 可以选择自动退出登录
      setTimeout(() => {
        // router.push('/login')
      }, 1500)
      
    } catch (error: any) {
      if (error !== false) {
        console.error('修改密码失败:', error)
        ElMessage.error(error.message || '修改密码失败')
      }
    } finally {
      passwordSaving.value = false
    }
  }

  // 工具方法
  const getRoleType = (role: string) => {
    const map: any = { 
      ADMIN: 'danger', 
      CARE_MANAGER: 'primary', 
      CAREGIVER: 'success' 
    }
    return map[role] || 'info'
  }

  const getRoleText = (role: string) => {
    const map: any = { 
      ADMIN: '管理员', 
      DIRECTOR: '院长',
      CARE_MANAGER: '照护主管', 
      CAREGIVER: '护理员'
    }
    return map[role] || role
  }

  onMounted(() => {
    loadProfile()
  })
</script>

<style scoped lang="scss">
.user-profile-page {
  padding: 20px;

  .profile-card {
    .card-header {
      font-weight: 600;
    }

    .profile-info {
      .avatar-section {
        text-align: center;
        padding: 20px 0;

        .avatar-uploader {
          display: inline-block;
        }

        .avatar-wrapper {
          position: relative;
          width: 100px;
          height: 100px;
          overflow: hidden;
          border-radius: 50%;
          cursor: pointer;

          .avatar-mask {
            position: absolute;
            inset: 0;
            display: flex;
            flex-direction: column;
            gap: 4px;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-size: 12px;
            background: rgb(0 0 0 / 55%);
            opacity: 0;
            transition: opacity 0.2s;
          }

          &:hover .avatar-mask {
            opacity: 1;
          }
        }

        .avatar-tip {
          margin-top: 8px;
          color: #909399;
          font-size: 12px;
        }

        h3 {
          margin: 0;
          font-size: 20px;
          font-weight: 600;
        }
      }

      .info-list {
        .info-item {
          display: flex;
          justify-content: space-between;
          padding: 12px 0;
          border-bottom: 1px solid #f0f0f0;

          &:last-child {
            border-bottom: none;
          }

          .label {
            color: #909399;
            font-size: 14px;
          }

          .value {
            color: #303133;
            font-size: 14px;
            font-weight: 500;
          }
        }
      }

      .action-buttons {
        margin-top: 20px;
      }
    }
  }

  .statistics-card {
    .card-header {
      font-weight: 600;
    }

    .statistics-content {
      min-height: 400px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}
</style>
