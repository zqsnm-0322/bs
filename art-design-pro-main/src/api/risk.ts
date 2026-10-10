import request from '@/utils/http'

/**
 * 风险评估相关 API
 */

/**
 * 获取风险评估记录列表
 * @param params 查询参数
 */
export function fetchRiskRecords(params: any) {
  return request.get<any>({
    url: '/api/risk/records',
    params
  })
}

/**
 * 提交风险评估
 * @param data 评估数据
 */
export function submitRiskAssessment(data: any) {
  return request.post<any>({
    url: '/api/risk/submit',
    data: data  // 修改为data,确保数据在请求体中
  })
}

/**
 * 获取启用的量表列表
 */
export function fetchEnabledScales() {
  return request.get<any>({
    url: '/api/scale/enabled'
  })
}

/**
 * 根据量表类型获取量表详情
 * @param scaleType 量表类型
 */
export function fetchScaleByType(scaleType: string) {
  return request.get<any>({
    url: `/api/scale/type/${scaleType}`
  })
}

/**
 * 获取老人信息（用于评估页面）
 * @param residentId 老人ID
 */
export function fetchResidentForAssessment(residentId: string | number) {
  return request.get<any>({
    url: `/api/resident/${residentId}`
  })
}

/**
 * 获取风险评估记录列表
 */
export function fetchRiskRecordList() {
  return request.get<any>({
    url: '/api/risk/list'
  })
}

/**
 * 获取待评估老人列表
 */
export function fetchPendingAssessmentList(params?: {
  pageNum?: number
  pageSize?: number
  status?: string
  keyword?: string
  careManagerName?: string
  caregiverName?: string
}) {
  return request.get<any>({
    url: '/api/risk/pending-assessment',
    params
  })
}

/**
 * 更新老人评估状态
 * @param residentId 老人ID
 * @param status 评估状态 (PENDING: 待评估, COMPLETED: 已评估)
 */
export function updateAssessmentStatus(residentId: number, status: string) {
  return request.put<any>({
    url: `/api/risk/assessment-status/${residentId}`,
    params: { status }
  })
}

/**
 * 老人离院，自动从评估列表移除
 */
export function removeFromAssessmentList(residentId: number) {
  return request.put<any>({
    url: `/api/risk/remove-assessment/${residentId}`
  })
}

/**
 * 发送评估报告给老人或家属端
 */
export function sendAssessmentToResident(data: {
  assessmentId: number | string
  residentId: number | string
}) {
  return request.post<any>({
    url: `/api/risk/send-to-resident?assessmentId=${data.assessmentId}&residentId=${data.residentId}`,
  })
}

/**
 * 获取老人的评估报告列表（小程序端使用）
 */
export function fetchResidentAssessmentReports(residentId: number | string) {
  return request.get<any>({
    url: `/api/risk/resident-reports/${residentId}`
  })
}
