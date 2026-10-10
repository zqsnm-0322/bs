import { AppRouteRecord } from '@/types/router'

// 需求17: 只有一个子菜单时改为一级菜单
export const statisticsRoutes: AppRouteRecord = {
  path: '/statistics',
  name: 'Statistics',
  component: '/index/index',
  meta: {
    title: '工作量统计',
    icon: 'ri:bar-chart-grouped-line',
    roles: ['R_SUPER', 'R_ADMIN']
  },
  children: [
    {
      path: 'report',
      name: 'StatisticsReport',
      component: '/statistics/report',
      meta: {
        title: '工作量统计',
        icon: 'ri:bar-chart-grouped-line',
        keepAlive: true
      }
    }
  ]
}
