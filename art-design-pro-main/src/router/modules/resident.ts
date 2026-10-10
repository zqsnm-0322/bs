import { AppRouteRecord } from '@/types/router'

export const residentRoutes: AppRouteRecord = {
  path: '/resident',
  name: 'Resident',
  component: '/index/index',
  meta: {
    title: '老人管理中心',
    icon: 'ri:folder-user-line',
    roles: ['R_SUPER', 'R_ADMIN', 'R_DIRECTOR', 'R_CARE_MANAGER', 'R_CAREGIVER']
  },
  children: [
    {
      path: 'list',
      name: 'ResidentList',
      component: '/resident/list',
      meta: {
        title: '老人列表',
        icon: 'ri:file-list-3-line',
        keepAlive: true
      }
    },
    {
      path: 'detail/:id',
      name: 'ResidentDetail',
      component: '/resident/detail',
      meta: {
        title: '老人档案',
        icon: 'ri:user-voice-line',
        isHide: true,
        activeMenu: '/resident/list',
        keepAlive: false
      }
    },
    {
      path: 'visit',
      name: 'VisitManagement',
      component: '/resident/visit',
      meta: {
        title: '探访预约',
        icon: 'ri:calendar-line',
        keepAlive: true
      }
    },
    {
      path: 'bed',
      name: 'ResidentBedManagement',
      component: '/resident/bed',
      meta: {
        title: '房间床位',
        icon: 'ri:hotel-bed-line',
        keepAlive: true,
        roles: ['R_SUPER', 'R_ADMIN', 'R_DIRECTOR', 'R_CAREGIVER']
      }
    },
    {
      path: 'care-plan',
      name: 'ResidentCarePlan',
      component: '/resident/care-plan',
      meta: {
        title: '护理计划',
        icon: 'ri:heart-pulse-line',
        keepAlive: true
      }
    },
    {
      path: 'risk-scale',
      name: 'ElderlyRiskScaleManagement',
      component: '/risk/scale',
      meta: {
        title: '养老风险量表',
        icon: 'ri:survey-line',
        keepAlive: true,
        roles: ['R_SUPER', 'R_ADMIN', 'R_DIRECTOR', 'R_CARE_MANAGER']
      }
    },
    {
      path: 'assessment/:id',
      name: 'ResidentAssessment',
      component: '/resident/assessment',
      meta: {
        title: '养老风险评估',
        icon: 'ri:survey-line',
        isHide: true,
        activeMenu: '/resident/list',
        keepAlive: false
      }
    }
  ]
}
