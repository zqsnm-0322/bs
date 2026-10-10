import request from '@/utils/http'

export function addBed(data: any) { return request.post({ url: '/api/bed', data }) }
export function updateBed(data: any) { return request.put({ url: '/api/bed', data }) }
export function deleteBed(id: number) { return request.del({ url: `/api/bed/${id}` }) }

/**
 * 调换床位
 */
export function swapBeds(data: {
  residentId1?: number
  roomBedNo1?: string
  roomBedNo2?: string
  careZone?: string
}) {
  return request.post({
    url: '/api/bed/swap',
    data
  })
}

/**
 * 获取可用床位列表
 */
export function getAvailableBeds(params?: {
  careZone?: string
  bedType?: string
}) {
  return request.get({
    url: '/api/bed/available',
    params
  })
}

/**
 * 获取床位列表（分页）
 */
export function getBedList(params: {
  current: number
  size: number
  careZone?: string
  bedNo?: string
  status?: string
  bedType?: string
  keyword?: string
  careManagerName?: string
  caregiverName?: string
}) {
  return request.get({
    url: '/api/bed/list',
    params
  })
}

/**
 * 获取床位详情
 */
export function getBedDetail(id: number) {
  return request.get({
    url: `/api/bed/${id}`
  })
}

/**
 * 分配床位给老人
 */
export function assignBed(data: {
  bedId: number
  residentId?: number
  residentName?: string
}) {
  return request.post({
    url: '/api/bed/assign',
    data
  })
}

/**
 * 老人离院，释放床位
 */
export function dischargeBed(bedId: number) {
  return request.post({
    url: `/api/bed/${bedId}/discharge`
  })
}

/**
 * 设置床位维护状态
 */
export function setBedMaintenance(bedId: number, maintenance: boolean) {
  return request.post({
    url: `/api/bed/${bedId}/maintenance`,
    data: { maintenance }
  })
}

/**
 * 获取床位统计信息
 */
export function getBedStatistics(careZone?: string) {
  return request.get({
    url: '/api/bed/statistics',
    params: { careZone }
  })
}

/**
 * 同步床位数据
 */
export function syncBedData() {
  return request.post({
    url: '/api/bed/sync'
  })
}
