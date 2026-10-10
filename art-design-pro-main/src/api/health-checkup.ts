import request from '@/utils/http'

/** 获取健康检查记录列表 */
export function fetchHealthCheckupList(params: any) {
  return request.get({ url: '/api/health-checkup/list', params })
}

/** 添加健康检查记录 */
export function addHealthCheckup(data: any) {
  return request.post({ url: '/api/health-checkup', data })
}

/** 更新健康检查记录 */
export function updateHealthCheckup(data: any) {
  return request.put({ url: '/api/health-checkup', data })
}

/** 删除健康检查记录 */
export function deleteHealthCheckup(id: number) {
  return request.del({ url: `/api/health-checkup/${id}` })
}
