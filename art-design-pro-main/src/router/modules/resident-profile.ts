import { AppRouteRecord } from '@/types/router'

export const residentProfileRoute: AppRouteRecord = {
  path: '/my-resident-profile',
  name: 'MyResidentProfile',
  component: '/resident/detail',
  meta: {
    title: '我的养老档案',
    icon: 'ri:user-heart-line',
    keepAlive: false,
    roles: ['R_RESIDENT']
  }
}
