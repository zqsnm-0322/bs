import request from '@/utils/http'

/**
 * 个人信息管理 API
 */

export interface UserProfile {
  id: number
  username: string
  realName: string
  role: string
  careZone?: string
  avatar?: string
  phone?: string
  email?: string
  createTime?: string
}

export interface PasswordChangeData {
  oldPassword: string
  newPassword: string
  confirmPassword?: string
}

/**
 * 获取当前用户个人信息
 */
export function getUserProfile() {
  return request.get<UserProfile>({
    url: '/api/user/profile'
  })
}

/**
 * 更新个人信息
 * @param data 更新数据（realName, phone, email）
 */
export function updateUserProfile(data: Partial<UserProfile>) {
  return request.put<UserProfile>({
    url: '/api/user/profile',
    data
  })
}

/**
 * 修改密码
 * @param data 密码数据（oldPassword, newPassword）
 */
export function changePassword(data: PasswordChangeData) {
  return request.put<any>({
    url: '/api/user/password',
    data
  })
}

/**
 * 获取用户统计信息
 */
export function getUserStatistics() {
  return request.get<any>({
    url: '/api/user/statistics'
  })
}

/** 上传当前用户头像 */
export function uploadProfileAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ avatar: string }>({
    url: '/api/user/avatar',
    data: formData
  })
}

