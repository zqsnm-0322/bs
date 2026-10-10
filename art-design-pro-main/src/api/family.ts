import request from '@/utils/http'

/** 获取家属列表 */
export function fetchFamilyList(residentId: number) {
  return request.get({ url: `/api/family/list/${residentId}` })
}

/** 添加家属 */
export function addFamily(data: any) {
  return request.post({ url: '/api/family', data })
}

/** 更新家属 */
export function updateFamily(data: any) {
  return request.put({ url: '/api/family', data })
}

/** 删除家属 */
export function deleteFamily(id: number) {
  return request.del({ url: `/api/family/${id}` })
}
