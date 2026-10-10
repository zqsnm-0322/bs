import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { systemRoutes } from './system'
import { residentRoutes } from './resident'
import { residentProfileRoute } from './resident-profile'

export const routeModules: AppRouteRecord[] = [
  dashboardRoutes,
  systemRoutes,
  residentRoutes,
  residentProfileRoute
]
