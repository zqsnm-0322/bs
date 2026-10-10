import request from '@/utils/http'

/**
 * 用药记录管理 API
 */

export interface Medication {
  id?: number
  residentId: number
  residentName?: string
  medicationName: string
  medicationCode?: string
  dosage: string
  unit?: string
  frequency: string
  route: string
  startDate: string
  endDate?: string
  duration?: number
  purpose?: string
  notes?: string
  prescribedBy?: string
  prescribedById?: number
  status?: 'ACTIVE' | 'COMPLETED' | 'STOPPED'
  stopReason?: string
  stopTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * 查询老人用药记录
 * @param residentId 老人ID
 * @param status 状态筛选（可选）
 */
export function getMedications(residentId: number, status?: string) {
  return request.get<Medication[]>({
    url: `/api/medication/resident/${residentId}`,
    params: { status }
  })
}

/**
 * 添加用药记录
 * @param data 用药记录数据
 */
export function addMedication(data: Medication) {
  return request.post<Medication>({
    url: '/api/medication',
    data
  })
}

/**
 * 更新用药记录
 * @param id 用药记录ID
 * @param data 用药记录数据
 */
export function updateMedication(id: number, data: Medication) {
  return request.put<Medication>({
    url: `/api/medication/${id}`,
    data
  })
}

/**
 * 删除用药记录
 * @param id 用药记录ID
 */
export function deleteMedication(id: number) {
  return request.del<any>({
    url: `/api/medication/${id}`
  })
}

/**
 * 停药
 * @param id 用药记录ID
 * @param stopReason 停药原因
 */
export function stopMedication(id: number, stopReason?: string) {
  return request.put<Medication>({
    url: `/api/medication/${id}/stop`,
    params: { stopReason }
  })
}

/**
 * 获取用药记录详情
 * @param id 用药记录ID
 */
export function getMedicationDetail(id: number) {
  return request.get<Medication>({
    url: `/api/medication/${id}`
  })
}
