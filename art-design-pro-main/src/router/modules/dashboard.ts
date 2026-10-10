import { AppRouteRecord } from '@/types/router'

export const dashboardRoutes: AppRouteRecord = {
  name: 'Dashboard',
  path: '/dashboard',
  component: '/index/index',
  meta: {
    title: '工作台',
    icon: 'ri:dashboard-line',
    roles: ['R_SUPER', 'R_ADMIN', 'R_DIRECTOR']
  },
  children: [
    {
      path: 'analysis',
      name: 'Analysis',
      component: '/dashboard/analysis',
      meta: {
        title: '数据分析',
        icon: 'ri:bar-chart-box-line',
        keepAlive: true,
        fixedTab: true
      }
    },
    {
      path: 'notifications',
      name: 'Notifications',
      component: '/dashboard/notifications',
      meta: {
        title: '消息通知',
        icon: 'ri:notification-line',
        keepAlive: true
      }
    }
  ]
}
