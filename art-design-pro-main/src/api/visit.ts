import request from '@/utils/http'

/** 获取探访预约列表 */
export function fetchVisitList(params: any) {
  return request.get({ url: '/api/visit/list', params })
}

/** 提交探访预约 */
export function submitVisitAppointment(data: any) {
  return request.post({ url: '/api/visit', data })
}

/** 更新待确认预约 */
export function updateVisit(data: any) {
  return request.put({ url: '/api/visit', data })
}

/** 删除未生效预约 */
export function deleteVisit(id: number) {
  return request.del({ url: `/api/visit/${id}` })
}

/** 审批通过 */
export function approveVisit(id: number, data: any) {
  return request.post({ url: `/api/visit/approve/${id}`, data })
}

/** 拒绝 */
export function rejectVisit(id: number, data: any) {
  return request.post({ url: `/api/visit/reject/${id}`, data })
}

/** 取消待确认或已批准预约 */
export function cancelVisit(id: number) {
  return request.post({ url: `/api/visit/cancel/${id}` })
}

/** 标记已批准的探访为完成 */
export function completeVisit(id: number) {
  return request.post({ url: `/api/visit/complete/${id}` })
}
