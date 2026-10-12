import request from '@/utils/http'

/**
 * 系统管理 API
 */

export interface ElderlyCareUser {
  id: number
  username: string
  realName: string
  role: string
  careZone?: string
  employeeNo?: string
  hireDate?: string
  createTime: string
}

export interface UserPage {
  records: ElderlyCareUser[]
  total: number
  current: number
  size: number
}

/**
 * 获取用户列表
 * @param params { current, size }
 */
export function fetchGetUserList(params: {
  current?: number
  size?: number
  username?: string
  realName?: string
  role?: string
  careZone?: string
}) {
  return request.get<UserPage>({
    url: '/api/user/list',
    params
  })
}

/**
 * 获取角色列表
 * @param params 角色搜索及分页参数
 */
export function fetchGetRoleList(params: Api.SystemManage.RoleSearchParams) {
  return request.get<Api.SystemManage.RoleList>({
    url: '/api/role/list',
    params
  })
}

/**
 * 添加用户
 * @param data 用户数据
 */
export function addUser(data: any) {
  return request.post<any>({
    url: '/api/user',
    params: data
  })
}

/**
 * 更新用户
 * @param data 用户数据
 */
export function updateUser(data: any) {
  return request.put<any>({
    url: '/api/user',
    params: data
  })
}

/**
 * 删除用户
 * @param id 用户ID
 */
export function deleteUser(id: number) {
  return request.del<any>({
    url: `/api/user/${id}`
  })
}

/**
 * 重置用户密码
 * @param id 用户ID
 * @param newPassword 新密码
 */
export function resetUserPassword(id: number, newPassword: string) {
  return request.put<any>({
    url: `/api/user/${id}/password`,
    params: { password: newPassword }
  })
}

/**
 * 启用/禁用用户
 * @param id 用户ID
 * @param enabled 是否启用
 */
export function toggleUserStatus(id: number, enabled: boolean) {
  return request.put<any>({
    url: `/api/user/${id}/status`,
    params: { enabled }
  })
}

/**
 * 获取当前用户详细信息
 */
export function fetchCurrentUserDetail() {
  return request.get<any>({
    url: '/api/user/current'
  })
}

/**
 * 更新当前用户信息
 * @param data 用户信息
 */
export function updateCurrentUser(data: any) {
  return request.put<any>({
    url: '/api/user/current',
    params: data
  })
}

/**
 * 修改当前用户密码
 * @param data { oldPassword, newPassword }
 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<any>({
    url: '/api/user/change-password',
    params: data
  })
}

/**
 * 上传用户头像
 * @param file 头像文件
 */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any>({
    url: '/api/user/avatar',
    params: formData
  })
}

