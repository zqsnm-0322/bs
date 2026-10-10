import request from '@/utils/http'

/**
 * 登录
 * @param params 登录参数 { username, password }
 * @returns 登录响应 { token, userInfo }
 */
export function fetchLogin(params: { username: string; password: string }) {
  return request.post<any>({
    url: '/api/login',
    params
  })
}

/**
 * 获取当前用户信息
 * @returns 用户信息
 */
export function fetchGetUserInfo() {
  return request.get<any>({
    url: '/api/user/info',
    showErrorMessage: false
  })
}
