<template>
  <v-dialog v-model="open" max-width="560" persistent>
    <v-card v-if="form" :class="{ 'dialog-light': !isDark }">
      <v-card-title class="pt-4 px-4">Edit Event #{{ event?.id }}</v-card-title>

      <!-- Status buttons -->
      <v-card-text class="pb-0">
        <div class="text-caption text-medium-emphasis mb-1">Status</div>
        <div class="status-track mb-4">
          <div class="status-indicator" :style="indicatorStyle" />
          <button
            v-for="s in statuses" :key="s.value"
            class="status-btn"
            :class="{ active: form.status === s.value }"
            @click="setStatus(s.value)"
          >{{ s.label }}</button>
        </div>

        <!-- Severity -->
        <v-select
          v-model="form.severity"
          :items="['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']"
          label="Severity"
          density="compact"
          variant="outlined"
          hide-details
          class="mb-4"
        />

        <!-- Message -->
        <v-textarea
          v-model="form.message"
          label="Message"
          density="compact"
          variant="outlined"
          rows="4"
          hide-details
        />
      </v-card-text>

      <v-card-actions class="px-4 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :loading="saving" @click="save">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { ClampedEvent } from '../types'
import { eventsApi } from '../api/client'
import { useAppTheme } from '../theme'

const { isDark } = useAppTheme()

const props = defineProps<{ event: ClampedEvent | null }>()
const emit = defineEmits<{ close: []; saved: [] }>()

const saving = ref(false)
const form = ref<{ message: string; status: string; severity: string } | null>(null)

const statuses = [
  { value: 'OPEN',         label: 'Open',        color: '#616161' },
  { value: 'IN_PROGRESS',  label: 'In Progress', color: '#1976d2' },
  { value: 'RESOLVED',     label: 'Resolved',    color: '#388e3c' },
]

const statusIndex = computed(() =>
  statuses.findIndex(s => s.value === form.value?.status)
)

// Slides the colored highlight to the active status segment.
// Each segment is 1/3 of the track width, so translateX(n * 100%) moves by exactly one segment.
const indicatorStyle = computed(() => ({
  width: `${100 / statuses.length}%`,
  transform: `translateX(${statusIndex.value * 100}%)`,
  backgroundColor: statuses[statusIndex.value]?.color ?? '#fff',
}))

function setStatus(value: string) {
  if (form.value) form.value.status = value
}

const open = computed({
  get: () => props.event !== null,
  set: (v) => { if (!v) emit('close') },
})

watch(() => props.event, (e) => {
  if (e) form.value = { message: e.message, status: e.status, severity: e.severity }
}, { immediate: true })

async function save() {
  if (!props.event || !form.value) return
  saving.value = true
  try {
    await eventsApi.update(props.event.id, form.value)
    emit('saved')
    emit('close')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.status-track {
  position: relative;
  display: flex;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  height: 28px;
  border: thin solid rgba(255,255,255,0.3);
}

.status-indicator {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: calc(100% / 3);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.2s ease;
  pointer-events: none;
}

.status-btn {
  flex: 1;
  white-space: nowrap;
  text-align: center;
  position: relative;
  z-index: 1;
  background: none;
  border: none;
  border-left: thin solid rgba(255,255,255,0.3);
  cursor: pointer;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  font-family: inherit;
  color: rgba(255,255,255,0.5);
  transition: color 0.2s ease;
  padding: 0 12px;
}

.status-btn:first-of-type {
  border-left: none;
}

.status-btn.active {
  color: #fff;
  font-weight: 700;
}

.status-btn:hover:not(.active) {
  background: rgba(255,255,255,0.05);
}

/* Light mode overrides */
.dialog-light .status-track {
  border-color: rgba(0, 0, 0, 0.2);
}

.dialog-light .status-btn {
  border-left-color: rgba(0, 0, 0, 0.2);
  color: rgba(0, 0, 0, 0.45);
}

.dialog-light .status-btn.active {
  color: #fff;
}

.dialog-light .status-btn:hover:not(.active) {
  background: rgba(0, 0, 0, 0.04);
}
</style>
