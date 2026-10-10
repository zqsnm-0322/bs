import request from '@/utils/http'

/**
 * 护理记录管理 API
 */

export interface NursingRecord {
  id?: number
  residentId: number
  residentName?: string
  title: string
  content: string
  recordTime: string
  recorder?: string
  recorderId?: number
  recordType?: 'ROUTINE' | 'SPECIAL' | 'EMERGENCY' | 'HANDOVER'
  createTime?: string
  updateTime?: string
}

/**
 * 查询老人护理记录
 * @param residentId 老人ID
 * @param params 查询参数
 */
export function getNursingRecords(residentId: number, params?: {
  startDate?: string
  endDate?: string
  recordType?: string
  current?: number
  size?: number
}) {
  return request.get<NursingRecord[]>({
    url: `/api/nursing-record/resident/${residentId}`,
    params
  })
}

/**
 * 添加护理记录
 * @param data 护理记录数据
 */
export function addNursingRecord(data: NursingRecord) {
  return request.post<NursingRecord>({
    url: '/api/nursing-record',
    data
  })
}

/**
 * 更新护理记录
 * @param id 护理记录ID
 * @param data 护理记录数据
 */
export function updateNursingRecord(id: number, data: NursingRecord) {
  return request.put<NursingRecord>({
    url: `/api/nursing-record/${id}`,
    data
  })
}

/**
 * 删除护理记录
 * @param id 护理记录ID
 */
export function deleteNursingRecord(id: number) {
  return request.del<any>({
    url: `/api/nursing-record/${id}`
  })
}

/**
 * 获取护理记录详情
 * @param id 护理记录ID
 */
export function getNursingRecordDetail(id: number) {
  return request.get<NursingRecord>({
    url: `/api/nursing-record/${id}`
  })
}

/**
 * 获取最近的护理记录
 * @param residentId 老人ID
 * @param limit 数量限制
 */
export function getRecentNursingRecords(residentId: number, limit: number = 10) {
  return request.get<NursingRecord[]>({
    url: `/api/nursing-record/resident/${residentId}/recent`,
    params: { limit }
  })
}
