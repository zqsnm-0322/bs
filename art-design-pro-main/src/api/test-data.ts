import request from '@/utils/http'

/**
 * 测试数据管理 API
 */

/**
 * 初始化测试数据
 */
export function initTestData() {
  return request.post<any>({
    url: '/api/test-data/init'
  })
}

/**
 * 清理测试数据
 */
export function cleanTestData() {
  return request.del<any>({
    url: '/api/test-data/clean'
  })
}
