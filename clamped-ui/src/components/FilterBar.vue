<template>
  <v-row dense class="mb-2">
    <v-col cols="12" style="max-width: 16%; flex: 0 0 16%; min-width: 16%;">
      <v-select
        v-model="filters.status"
        :items="[{ title: 'Open', value: 'OPEN' }, { title: 'In Progress', value: 'IN_PROGRESS' }, { title: 'Resolved', value: 'RESOLVED' }]"
        label="Status"
        density="compact"
        variant="outlined"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" style="max-width: 16%; flex: 0 0 16%; min-width: 16%;">
      <v-select
        v-model="filters.severity"
        :items="['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']"
        label="Severity"
        density="compact"
        variant="outlined"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" style="max-width: 19%; flex: 0 0 19%; min-width: 19%;">
      <v-text-field
        v-model="filters.tag"
        label="Tag"
        density="compact"
        variant="outlined"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" style="max-width: 12%; flex: 0 0 12%; min-width: 12%;">
      <v-select
        v-model="filters.since"
        :items="['1h', '24h', '7d', '30d']"
        label="Since"
        density="compact"
        variant="outlined"
        hide-details
        clearable
      />
    </v-col>
    <v-col cols="12" style="max-width: 37%; flex: 0 0 37%; min-width: 37%;">
      <v-text-field
        v-model="searchText"
        label="Search all"
        density="compact"
        variant="outlined"
        hide-details
        clearable
        prepend-inner-icon="mdi-magnify"
        @update:model-value="emit('update:search', $event ?? '')"
      />
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { EventFilters } from '../api/client'

const props = defineProps<{ initialFilters?: EventFilters }>()
const emit = defineEmits<{ search: [filters: EventFilters], 'update:search': [value: string] }>()

const searchText = ref('')

const filters = reactive<EventFilters>({
  status:   props.initialFilters?.status,
  severity: props.initialFilters?.severity,
  tag:      props.initialFilters?.tag,
  since:    props.initialFilters?.since,
})

watch(() => props.initialFilters, (val) => {
  filters.status   = val?.status
  filters.severity = val?.severity
  filters.tag      = val?.tag
  filters.since    = val?.since
}, { deep: true })

watch(filters, (newFilters) => {
  emit('search', { ...newFilters })
}, { deep: true, immediate: true })
</script>

<style scoped>
</style>
