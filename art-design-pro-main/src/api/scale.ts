import request from '@/utils/http'

/**
 * 量表管理相关 API
 */

/**
 * 获取量表列表
 * @param params 查询参数
 */
export function fetchScaleList(params?: any) {
  return request.get<Api.Common.PaginatedResponse<Record<string, any>>>({
    url: '/api/scale/list',
    params
  })
}

/**
 * 创建量表
 * @param data 量表数据
 */
export function createScale(data: any) {
  return request.post<any>({
    url: '/api/scale',
    data: data
  })
}

/**
 * 更新量表
 * @param data 量表数据
 */
export function updateScale(data: any) {
  return request.put<any>({
    url: '/api/scale',
    data: data
  })
}

/**
 * 删除量表
 * @param id 量表ID
 */
export function deleteScale(id: number) {
  return request.del<any>({
    url: `/api/scale/${id}`
  })
}

/**
 * 获取量表详情（用于设计器）
 * @param id 量表ID
 */
export function fetchScaleDetail(id: number | string) {
  return request.get<any>({
    url: `/api/scale/${id}`
  })
}

/**
 * 启用/禁用量表
 * @param id 量表ID
 * @param enabled 是否启用
 */
export function toggleScaleStatus(id: number, enabled: boolean) {
  return request.put<any>({
    url: `/api/scale/${id}/status`,
    data: { enabled }
  })
}

/**
 * 预览量表
 * @param id 量表ID
 */
export function previewScale(id: number) {
  return request.get<any>({
    url: `/api/scale/${id}/preview`
  })
}
