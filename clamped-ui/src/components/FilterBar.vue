<template>
  <v-row dense class="mb-2">
    <v-col cols="12" sm="2">
      <v-select
        v-model="filters.status"
        :items="[{ title: 'Open', value: 'OPEN' }, { title: 'In Progress', value: 'IN_PROGRESS' }, { title: 'Resolved', value: 'RESOLVED' }]"
        label="Status"
        density="compact"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" sm="2">
      <v-select
        v-model="filters.severity"
        :items="['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']"
        label="Severity"
        density="compact"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" sm="2">
      <v-text-field
        v-model="filters.tag"
        label="Tag"
        density="compact"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" sm="2">
      <v-select
        v-model="filters.since"
        :items="['1h', '24h', '7d', '30d']"
        label="Since"
        density="compact"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" sm="2">
      <v-text-field
        v-model.number="filters.limit"
        label="Limit"
        type="number"
        density="compact"
        hide-details
      />
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { EventFilters } from '../api/client'

const props = defineProps<{ initialFilters?: EventFilters }>()
const emit = defineEmits<{ search: [filters: EventFilters] }>()

const filters = reactive<EventFilters>({
  status:   props.initialFilters?.status,
  severity: props.initialFilters?.severity,
  tag:      props.initialFilters?.tag,
  since:    props.initialFilters?.since,
  limit:    props.initialFilters?.limit ?? 50,
})

watch(() => props.initialFilters, (val) => {
  filters.status   = val?.status
  filters.severity = val?.severity
  filters.tag      = val?.tag
  filters.since    = val?.since
  filters.limit    = val?.limit ?? 50
}, { deep: true })

watch(filters, (newFilters) => {
  emit('search', { ...newFilters })
}, { deep: true, immediate: true })
</script>
