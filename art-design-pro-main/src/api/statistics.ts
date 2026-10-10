import request from '@/utils/http'

/**
 * 统计分析相关 API
 */

/**
 * 获取统计报表数据
 * @param params 查询参数
 */
export function fetchStatisticsReport(params?: {
  startDate?: string
  endDate?: string
  careZone?: string
  scaleType?: string
}) {
  return request.get<any>({
    url: '/api/statistics/report',
    params
  })
}

/**
 * 获取风险分布统计
 * @param params 查询参数
 */
export function fetchRiskDistribution(params?: {
  startDate?: string
  endDate?: string
  careZone?: string
}) {
  return request.get<any>({
    url: '/api/statistics/risk-distribution',
    params
  })
}

/**
 * 获取护理区风险排行
 * @param params 查询参数
 */
export function fetchCareZoneRanking(params?: {
  startDate?: string
  endDate?: string
  limit?: number
}) {
  return request.get<any>({
    url: '/api/statistics/care-zone-ranking',
    params
  })
}

/**
 * 获取风险趋势数据
 * @param params 查询参数
 */
export function fetchRiskTrend(params?: {
  startDate?: string
  endDate?: string
  scaleType?: string
  granularity?: 'day' | 'week' | 'month'
}) {
  return request.get<any>({
    url: '/api/statistics/risk-trend',
    params
  })
}

/**
 * 获取评估统计数据
 * @param params 查询参数
 */
export function fetchAssessmentStats(params?: {
  pageNum?: number
  pageSize?: number
  startDate?: string
  endDate?: string
  evaluatorId?: number
}) {
  return request.get<any>({
    url: '/api/statistics/assessment-stats',
    params
  })
}

/**
 * 导出统计报表
 * @param params 导出参数
 */
export function exportStatisticsReport(params?: {
  startDate?: string
  endDate?: string
  careZone?: string
  format?: 'excel' | 'pdf'
}) {
  return request.get<any>({
    url: '/api/statistics/export',
    params,
    responseType: 'blob'
  })
}
