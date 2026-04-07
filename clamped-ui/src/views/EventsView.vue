<template>
  <v-container fluid>
    <div class="d-flex align-center mb-4">
      <div class="text-h5 font-weight-semibold">Events</div>
      <v-spacer />
      <template v-if="selected.length > 0">
        <span class="text-caption text-medium-emphasis mr-3">{{ selected.length }} selected</span>
        <v-btn variant="outlined" size="small" :color="resolveColor" class="mr-2" @click="bulkResolve">Resolve</v-btn>
        <v-btn variant="outlined" size="small" color="error" class="mr-3" @click="bulkDelete">Delete</v-btn>
      </template>
      <v-btn variant="outlined" size="small" prepend-icon="mdi-download" @click="exportCsv">Export CSV</v-btn>
    </div>

    <FilterBar :initial-filters="initialFilters" @search="load" @update:search="search = $event" />

    <v-data-table
      :headers="headers"
      :items="events"
      :loading="loading"
      v-model:sort-by="sortBy"
      :search="search"
      :custom-filter="customFilter"
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
              <v-list-item prepend-icon="mdi-check-circle-outline" title="Resolve" @click="resolveEvent(item)" />
              <v-list-item prepend-icon="mdi-refresh" title="Reopen" @click="reopenEvent(item)" />
              <v-list-item prepend-icon="mdi-delete" title="Delete" @click="deleteEvent(item)" class="text-error" />
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
      <template #item.tag="{ item }">
        <span>{{ truncate(item.tag ?? '', 18) }}</span>
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

    <ResolveDialog
      :event="resolvingEvent"
      @close="resolvingEvent = null"
      @resolved="onResolved"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import FilterBar from '../components/FilterBar.vue'
import { useAppTheme } from '../theme'
import EventDetailDrawer from '../components/EventDetailDrawer.vue'
import EditEventDialog from '../components/EditEventDialog.vue'
import ResolveDialog from '../components/ResolveDialog.vue'
import { eventsApi, type EventFilters } from '../api/client'
import type { ClampedEvent } from '../types'

const route = useRoute()
const { severityColor, statusColor, resolveColor } = useAppTheme()
const initialFilters = computed<EventFilters>(() => ({
  status:   (route.query.status as string) || undefined,
  severity: (route.query.severity as string) || undefined,
  tag:      (route.query.tag as string) || undefined,
  limit:    50,
}))

const events = ref<ClampedEvent[]>([])
const loading = ref(false)
const selected = ref<number[]>([])
const search = ref('')
const sortBy = ref<{ key: string; order: 'asc' | 'desc' }[]>([])
const selectedEvent = ref<ClampedEvent | null>(null)
const editingEvent = ref<ClampedEvent | null>(null)
const resolvingEvent = ref<ClampedEvent | null>(null)
const lastFilters = ref<EventFilters>({ status: 'OPEN', limit: 50 })

const SEVERITY_ORDER: Record<string, number> = { LOW: 0, MEDIUM: 1, HIGH: 2, CRITICAL: 3 }
const STATUS_ORDER: Record<string, number>   = { OPEN: 0, IN_PROGRESS: 1, RESOLVED: 2 }

const headers = [
  { title: 'ID',          key: 'id',              width: 55,  sortable: true },
  { title: 'Actions',     key: 'actions',         width: 75,  sortable: false },
  { title: 'Status',      key: 'status',          width: 115, sortable: true,
    sort: (a: string, b: string) => (STATUS_ORDER[a] ?? 9) - (STATUS_ORDER[b] ?? 9) },
  { title: 'Created At',  key: 'firstSeen',       width: 160, sortable: true },
  { title: 'Severity',    key: 'severity',        width: 95,  sortable: true,
    sort: (a: string, b: string) => (SEVERITY_ORDER[a] ?? -1) - (SEVERITY_ORDER[b] ?? -1) },
  { title: 'Tag',         key: 'tag',             width: 140, sortable: true },
  { title: 'Message',     key: 'message',         width: 200, sortable: true },
  { title: 'Occurrences', key: 'occurrenceCount', width: 110, sortable: true, align: 'center' as const },
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

async function reopenEvent(item: ClampedEvent) {
  await eventsApi.revert(item.id)
  load(lastFilters.value)
}

function resolveEvent(item: ClampedEvent) {
  resolvingEvent.value = item
}

async function onResolved() {
  resolvingEvent.value = null
  load(lastFilters.value)
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

function customFilter(value: unknown, query: string) {
  if (!query) return true
  const q = query.toLowerCase()
  const qUnder = q.replace(/\s+/g, '_')
  const v = String(value ?? '').toLowerCase()
  return v.includes(q) || v.includes(qUnder)
}

function fmt(ts: string | null) {
  return ts ? ts.substring(0, 19).replace('T', ' ') : ''
}

function truncate(s: string, n: number) {
  return s && s.length > n ? s.substring(0, n) + '…' : s
}


function toLabel(s: string) {
  return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[s] ?? s
}
</script>
