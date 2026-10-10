import request from '@/utils/http'

/**
 * 仪表盘 API
 */

/**
 * 获取仪表盘统计数据
 * @returns 统计数据
 * {
 *   residentCount: number,       // 在住老人总数
 *   highRiskCount: number,        // 今日高危预警数
 *   evalCount: number,            // 累计评估次数
 *   newAdmissions: number,       // 本月新增入住
 *   leavingCount: number,        // 本月离院人数
 *   zoneDistribution: Array,     // 护理区分布
 *   healthLevelDistribution: Array,
 *   bedOccupancyRate: number
 * }
 */
export function fetchDashboardStats() {
  return request.get<any>({
    url: '/api/dashboard/stats'
  })
}

/**
 * 扫描高危老人
 * @returns 扫描报告
 */
export function scanHighRiskResidents() {
  return request.get<any>({
    url: '/api/dashboard/scan'
  })
}
