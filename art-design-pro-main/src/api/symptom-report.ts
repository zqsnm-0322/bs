import request from '@/utils/http'

/**
 * 获取症状上报列表
 */
export function getSymptomReportList(params: any) {
  return request.get({
    url: '/api/symptom/list',
    params
  })
}

/**
 * 获取症状上报详情
 */
export function getSymptomReportDetail(id: number) {
  return request.get({
    url: `/api/symptom/detail/${id}`
  })
}

/**
 * 处理症状上报
 */
export function handleSymptomReport(id: number, data: any) {
  return request.put({
    url: `/api/symptom/${id}/handle`,
    data
  })
}

/**
 * 老人查看自己的上报历史
 */
export function getMySymptomReports(params: any) {
  return request.get({
    url: '/api/symptom/my',
    params
  })
}
