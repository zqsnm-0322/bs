import request from '@/utils/http'

export interface VitalSigns {
  id?: number
  residentId: number
  measurementTime: string
  measurementType: 'ROUTINE' | 'CONTINUOUS' | 'EMERGENCY'
  systolicBp?: number
  diastolicBp?: number
  heartRate?: number
  respiratoryRate?: number
  temperature?: number
  oxygenSaturation?: number
  measuredBy?: string
  notes?: string
}

/**
 * 获取老人生命体征
 */
export const getVitalSigns = (residentId: number, hours?: number) => {
  return request.get<VitalSigns[]>({
    url: `/api/enhanced-risk/vital-signs/${residentId}`,
    params: { hours }
  })
}

/**
 * 添加生命体征记录
 */
export const addVitalSigns = (data: VitalSigns) => {
  return request.post({
    url: '/api/enhanced-risk/vital-signs',
    data
  })
}

/**
 * 更新生命体征记录
 */
export const updateVitalSigns = (id: number, data: VitalSigns) => {
  return request.put({
    url: `/api/enhanced-risk/vital-signs/${id}`,
    data
  })
}

/**
 * 删除生命体征记录
 */
export const deleteVitalSigns = (id: number) => {
  return request.del({
    url: `/api/enhanced-risk/vital-signs/${id}`
  })
}
