import request from '@/utils/http'

export function fetchCarePlanList(params: any) {
  return request.get<any>({ url: '/api/care-plan/list', params })
}

export function fetchCarePlanDetail(id: number) {
  return request.get<any>({ url: `/api/care-plan/${id}` })
}

export function fetchCarePlanResidents(keyword?: string) {
  return request.get<any[]>({ url: '/api/care-plan/residents', params: { keyword } })
}

export function fetchCarePlanCaregivers(careZone?: string) {
  return request.get<any[]>({ url: '/api/care-plan/caregivers', params: { careZone } })
}

export function createCarePlan(data: any) {
  return request.post<number>({ url: '/api/care-plan', data })
}

export function updateCarePlan(id: number, data: any) {
  return request.put({ url: `/api/care-plan/${id}`, data })
}

export function updateCarePlanStatus(id: number, status: string) {
  return request.put({ url: `/api/care-plan/${id}/status`, params: { status } })
}

export function deleteCarePlan(id: number) {
  return request.del({ url: `/api/care-plan/${id}` })
}

export function fetchResidentCareTasks(residentId: number) {
  return request.get<any[]>({ url: `/api/care-plan/resident/${residentId}/tasks` })
}

export function completeCareTask(taskId: number, data: { userId: number; userName: string }) {
  return request.put({ url: `/api/care-plan/task/${taskId}/complete`, data })
}
