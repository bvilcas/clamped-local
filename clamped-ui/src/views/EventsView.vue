<template>
  <v-container fluid>
    <div class="d-flex align-center mb-4">
      <div class="text-h5">Events</div>
      <v-spacer />
      <template v-if="selected.length > 0">
        <span class="text-caption text-medium-emphasis mr-3">{{ selected.length }} selected</span>
        <v-btn variant="outlined" size="small" color="green" class="mr-2" @click="bulkResolve">Resolve</v-btn>
        <v-btn variant="outlined" size="small" color="error" class="mr-3" @click="bulkDelete">Delete</v-btn>
      </template>
      <v-btn variant="outlined" size="small" prepend-icon="mdi-download" @click="exportCsv">Export CSV</v-btn>
    </div>

    <FilterBar :initial-filters="initialFilters" @search="load" />

    <v-data-table
      :headers="headers"
      :items="events"
      :loading="loading"
      :sort-by="[]"
      v-model="selected"
      item-value="id"
      show-select
      hover
      @click:row="(_: any, { item }: any) => viewEvent(item)"
    >
      <template #item.actions="{ item }">
        <div>
          <v-menu>
            <template #activator="{ props }">
              <v-btn icon size="small" variant="text" v-bind="props" @click.stop>
                <v-icon>mdi-menu</v-icon>
              </v-btn>
            </template>
            <v-list density="compact">
              <v-list-item prepend-icon="mdi-eye" title="View" @click="viewEvent(item)" />
              <v-list-item prepend-icon="mdi-pencil" title="Edit" @click="editEvent(item)" />
              <v-list-item prepend-icon="mdi-delete" title="Delete" @click="deleteEvent(item)" />
            </v-list>
          </v-menu>
        </div>
      </template>
      <template #item.status="{ item }">
        <v-chip :color="statusColor(item.status)" size="small" variant="outlined">{{ toLabel(item.status) }}</v-chip>
      </template>
      <template #item.severity="{ item }">
        <span :style="{ color: severityColor(item.severity), fontWeight: 600, fontSize: '0.8rem' }">{{ item.severity }}</span>
      </template>
      <template #item.message="{ item }">
        <span>{{ truncate(item.message, 80) }}</span>
      </template>
      <template #item.firstSeen="{ item }">
        {{ fmt(item.firstSeen) }}
      </template>
    </v-data-table>

    <EventDetailDrawer
      :event="selectedEvent"
      @close="selectedEvent = null"
    />

    <EditEventDialog
      :event="editingEvent"
      @close="editingEvent = null"
      @saved="load(lastFilters)"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import FilterBar from '../components/FilterBar.vue'
import EventDetailDrawer from '../components/EventDetailDrawer.vue'
import EditEventDialog from '../components/EditEventDialog.vue'
import { eventsApi, type EventFilters } from '../api/client'
import type { ClampedEvent } from '../types'

const route = useRoute()
const initialFilters = computed<EventFilters>(() => ({
  status:   (route.query.status as string) || undefined,
  severity: (route.query.severity as string) || undefined,
  tag:      (route.query.tag as string) || undefined,
  limit:    50,
}))

const events = ref<ClampedEvent[]>([])
const loading = ref(false)
const selected = ref<number[]>([])
const selectedEvent = ref<ClampedEvent | null>(null)
const editingEvent = ref<ClampedEvent | null>(null)
const lastFilters = ref<EventFilters>({ status: 'OPEN', limit: 50 })

const SEVERITY_ORDER: Record<string, number> = { LOW: 0, MEDIUM: 1, HIGH: 2, CRITICAL: 3 }
const STATUS_ORDER: Record<string, number>   = { OPEN: 0, IN_PROGRESS: 1, RESOLVED: 2 }

const headers = [
  { title: 'ID',         key: 'id',              width: 70,  sortable: true },
  { title: 'Actions',    key: 'actions',         width: 100, sortable: false },
  { title: 'Status',     key: 'status',          width: 130, sortable: true,
    sort: (a: string, b: string) => (STATUS_ORDER[a] ?? 9) - (STATUS_ORDER[b] ?? 9) },
  { title: 'Created At', key: 'firstSeen',       width: 160, sortable: true },
  { title: 'Severity',   key: 'severity',        width: 110, sortable: true,
    sort: (a: string, b: string) => (SEVERITY_ORDER[a] ?? -1) - (SEVERITY_ORDER[b] ?? -1) },
  { title: 'Tag',        key: 'tag',             width: 140, sortable: true },
  { title: 'Message',    key: 'message',         width: 200, sortable: true },
  { title: '#',          key: 'occurrenceCount', width: 60,  sortable: true },
]

async function load(filters: EventFilters) {
  lastFilters.value = filters
  loading.value = true
  selected.value = []
  try {
    events.value = await eventsApi.list(filters)
  } finally {
    loading.value = false
  }
}

async function viewEvent(item: ClampedEvent) {
  selectedEvent.value = await eventsApi.get(item.id)
}

function editEvent(item: ClampedEvent) {
  editingEvent.value = item
}

async function deleteEvent(item: ClampedEvent) {
  await eventsApi.delete(item.id)
  load(lastFilters.value)
}

async function bulkResolve() {
  await Promise.all(selected.value.map(id => eventsApi.resolve(id)))
  load(lastFilters.value)
}

async function bulkDelete() {
  await Promise.all(selected.value.map(id => eventsApi.delete(id)))
  load(lastFilters.value)
}

function exportCsv() {
  const cols = ['id','status','severity','tag','message','occurrenceCount','firstSeen','sourceFile','sourceLine','sourceMethod','exceptionClass']
  const rows = events.value.map(e => cols.map(h => {
    const v = (e as any)[h]
    return v == null ? '' : `"${String(v).replace(/"/g, '""')}"`
  }).join(','))
  const csv = [cols.join(','), ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `clamped-events-${new Date().toISOString().slice(0,10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function fmt(ts: string | null) {
  return ts ? ts.substring(0, 19).replace('T', ' ') : ''
}

function truncate(s: string, n: number) {
  return s && s.length > n ? s.substring(0, n) + '…' : s
}

function severityColor(s: string) {
  return { LOW: '#64b5f6', MEDIUM: '#ffa726', HIGH: '#ff7043', CRITICAL: '#ef5350' }[s] ?? '#9e9e9e'
}

function statusColor(s: string) {
  return { OPEN: 'default', IN_PROGRESS: 'blue', RESOLVED: 'green' }[s] ?? 'default'
}

function toLabel(s: string) {
  return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[s] ?? s
}
</script>
