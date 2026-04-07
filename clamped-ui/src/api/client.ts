import axios from 'axios'
import type { ClampedEvent, StatsResponse } from '../types'

const api = axios.create({ baseURL: '/api' })

export interface EventFilters {
  status?: string
  severity?: string
  tag?: string
  app?: string
  since?: string
  limit?: number
}

export const eventsApi = {
  list(filters: EventFilters = {}): Promise<ClampedEvent[]> {
    const params = Object.fromEntries(
      Object.entries(filters).filter(([, v]) => v != null && v !== '')
    )
    return api.get('/events', { params }).then(r => r.data)
  },

  get(id: number): Promise<ClampedEvent> {
    return api.get(`/events/${id}`).then(r => r.data)
  },

  update(id: number, data: { message: string; status: string; severity: string }) {
    return api.put(`/events/${id}`, data)
  },
  resolve(id: number, notes?: string) {
    return api.post(`/events/${id}/resolve`, notes ? { notes } : undefined)
  },
  ack(id: number)     { return api.post(`/events/${id}/ack`) },
  revert(id: number)  { return api.post(`/events/${id}/revert`) },
  delete(id: number)  { return api.delete(`/events/${id}`) },
  bulkByTag(action: 'resolve' | 'ack' | 'ignore' | 'revert', tag: string): Promise<{ updated: number }> {
    return api.post(`/events/bulk/${action}`, null, { params: { tag } }).then(r => r.data)
  },
}

export const statsApi = {
  get(): Promise<StatsResponse> {
    return api.get('/stats').then(r => r.data)
  },
}

export const maintenanceApi = {
  purge(days: number): Promise<{ deleted: number }> {
    return api.delete('/events/purge', { params: { days } }).then(r => r.data)
  },
  seed(): Promise<{ seeded: number }> {
    return api.post('/seed').then(r => r.data)
  },
}
