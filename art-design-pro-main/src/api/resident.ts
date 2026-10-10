import request from '@/utils/http'

/** 获取在住老人列表 */
export function fetchResidentList(params: any) {
  return request.get({ url: '/api/resident/list', params })
}

/** 获取老人详情 */
export function fetchResidentDetail(id: number) {
  return request.get({ url: `/api/resident/${id}` })
}

/** 入住登记 */
export function addResident(data: any) {
  return request.post({ url: '/api/resident', data })
}

/** 办理离院 */
export function leaveResident(id: number, data: any) {
  return request.post({ url: `/api/resident/leave/${id}`, data })
}

/** 更新老人信息 */
export function updateResident(data: any) {
  return request.put({ url: '/api/resident', data })
}
