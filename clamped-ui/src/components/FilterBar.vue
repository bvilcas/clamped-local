<template>
  <v-row dense class="mb-2">
    <v-col cols="12" style="max-width: 12%; flex: 0 0 12%; min-width: 12%;">
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
    <v-col cols="12" style="max-width: 12%; flex: 0 0 12%; min-width: 12%;">
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
    <v-col cols="12" style="max-width: 15%; flex: 0 0 15%; min-width: 15%;">
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
    <v-col cols="12" style="max-width: 12%; flex: 0 0 12%; min-width: 12%;">
      <v-text-field
        v-model.number="filters.limit"
        label="Limit"
        type="number"
        density="compact"
        variant="outlined"
        hide-details
        class="no-spinner"
      >
        <template #append-inner>
          <div class="limit-controls">
            <v-icon
              size="12"
              :class="['limit-btn', { pressed: upPressed }]"
              @mousedown="upPressed = true"
              @mouseup="upPressed = false; filters.limit = (filters.limit ?? 50) + 1"
              @mouseleave="upPressed = false"
            >mdi-chevron-up</v-icon>
            <v-icon
              size="12"
              :class="['limit-btn', { pressed: downPressed }]"
              @mousedown="downPressed = true"
              @mouseup="downPressed = false; filters.limit = Math.max(1, (filters.limit ?? 50) - 1)"
              @mouseleave="downPressed = false"
            >mdi-chevron-down</v-icon>
          </div>
        </template>
      </v-text-field>
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
const upPressed = ref(false)
const downPressed = ref(false)

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

<style scoped>
.no-spinner :deep(input[type=number]) {
  -moz-appearance: textfield;
}
.no-spinner :deep(input[type=number]::-webkit-outer-spin-button),
.no-spinner :deep(input[type=number]::-webkit-inner-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}
.limit-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  gap: 0;
  line-height: 1;
}
.limit-btn {
  transition: opacity 0.1s, transform 0.1s;
  opacity: 0.6;
}
.limit-btn:hover {
  opacity: 1;
}
.limit-btn.pressed {
  opacity: 1;
  transform: scale(0.75);
}
</style>
