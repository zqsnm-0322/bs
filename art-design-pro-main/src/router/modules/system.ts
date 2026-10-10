import { AppRouteRecord } from '@/types/router'

// 系统管理仅面向管理员和院长，护理人员不可见
export const systemRoutes: AppRouteRecord = {
  path: '/system',
  name: 'System',
  component: '/index/index',
  meta: {
    title: '系统管理',
    icon: 'ri:settings-3-line',
    roles: ['R_SUPER', 'R_ADMIN']
  },
  children: [
    {
      path: 'user',
      name: 'User',
      component: '/system/user',
      meta: {
        title: '用户管理',
        icon: 'ri:user-line',
        keepAlive: true,
        roles: ['R_SUPER', 'R_ADMIN']
      }
    },
    {
      path: 'user-center',
      name: 'UserCenter',
      component: '/user/profile',
      meta: {
        title: '个人中心',
        icon: 'ri:user-line',
        isHide: true,
        keepAlive: true,
        isHideTab: true
      }
    },
    {
      path: 'developing',
      name: 'SystemDeveloping',
      component: '/system/developing',
      meta: {
        title: '正在开发中',
        icon: 'ri:tools-line',
        isHide: true,
        keepAlive: false
      }
    }
    // 需求15: 删除日志监控
  ]
}
