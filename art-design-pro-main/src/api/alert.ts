import request from '@/utils/http'

/**
 * 预警消息 API
 */

/**
 * 获取我的预警消息
 * @param userId 用户ID（可选）
 */
export function fetchMyAlerts(userId?: number) {
  return request.get<any>({
    url: '/api/alert/my',
    params: userId ? { userId } : undefined
  })
}

/**
 * 标记预警消息为已读
 * @param id 消息ID
 */
export function markAlertAsRead(id: number) {
  return request.put<any>({
    url: `/api/alert/${id}/read`
  })
}
