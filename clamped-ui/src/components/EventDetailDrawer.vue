<template>
  <v-navigation-drawer v-model="open" location="right" width="520" temporary>
    <template v-if="event">
      <v-toolbar color="surface" elevation="0" density="compact">
        <v-toolbar-title class="text-subtitle-1 font-weight-bold">Event #{{ event.id }}</v-toolbar-title>
        <template #append>
          <v-btn icon size="small" variant="text" @click="open = false"><v-icon>mdi-close</v-icon></v-btn>
        </template>
      </v-toolbar>

      <v-divider />

      <v-container class="pa-4">

        <!-- Status + Severity + Occurrences -->
        <v-row dense class="mb-4">
          <v-col cols="4">
            <div class="field-label">Status</div>
            <v-chip :color="statusColor(event.status)" size="small" variant="outlined">{{ toLabel(event.status) }}</v-chip>
          </v-col>
          <v-col cols="4">
            <div class="field-label">Severity</div>
            <span class="field-value" :style="{ color: severityColor(event.severity), fontWeight: 700 }">{{ event.severity }}</span>
          </v-col>
          <v-col cols="4">
            <div class="field-label">Occurrences</div>
            <span class="field-value">{{ event.occurrenceCount }}×</span>
          </v-col>
        </v-row>

        <!-- Message -->
        <div class="mb-4">
          <div class="field-label">Message</div>
          <div class="field-value">{{ event.message }}</div>
        </div>

        <v-divider class="mb-4" />

        <!-- Two-column grid -->
        <v-row dense class="mb-2">
          <v-col cols="6">
            <div class="field-label">App</div>
            <div class="field-value">{{ event.appName }}</div>
          </v-col>
          <v-col cols="6">
            <div class="field-label">Environment</div>
            <div class="field-value">{{ event.environment }}</div>
          </v-col>
          <v-col cols="6" class="mt-3">
            <div class="field-label">Tag</div>
            <div class="field-value">{{ event.tag ?? '—' }}</div>
          </v-col>
          <v-col cols="6" class="mt-3">
            <div class="field-label">First Seen</div>
            <div class="field-value">{{ fmt(event.firstSeen) }}</div>
          </v-col>
          <v-col cols="6" class="mt-3">
            <div class="field-label">Thread</div>
            <div class="field-value">{{ event.threadName ?? '—' }}</div>
          </v-col>
          <v-col cols="6" class="mt-3">
            <div class="field-label">Host</div>
            <div class="field-value">{{ event.host ?? '—' }}</div>
          </v-col>
        </v-row>

        <!-- Source -->
        <template v-if="event.sourceFile">
          <v-divider class="my-4" />
          <div class="field-label mb-1">Source</div>
          <div class="field-value mono">{{ event.sourceFile }}:{{ event.sourceLine }} ({{ event.sourceMethod }})</div>
        </template>

        <!-- Stack trace -->
        <template v-if="event.stacktrace">
          <v-divider class="my-4" />
          <div class="field-label mb-2">Stack Trace</div>
          <pre class="code-pre">{{ event.stacktrace }}</pre>
        </template>

        <!-- Metadata -->
        <template v-if="event.metadata && event.metadata !== '{}'">
          <v-divider class="my-4" />
          <div class="field-label mb-2">Metadata</div>
          <pre class="code-pre">{{ prettyMetadata }}</pre>
        </template>

      </v-container>
    </template>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClampedEvent } from '../types'

const props = defineProps<{ event: ClampedEvent | null }>()
const emit = defineEmits<{ close: [] }>()

const open = computed({
  get: () => props.event !== null,
  set: (v) => { if (!v) emit('close') },
})

const prettyMetadata = computed(() => {
  if (!props.event?.metadata) return ''
  try { return JSON.stringify(JSON.parse(props.event.metadata), null, 2) }
  catch { return props.event.metadata }
})

function toLabel(status: string) {
  return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[status] ?? status
}

function fmt(ts: string | null) {
  return ts ? ts.substring(0, 19).replace('T', ' ') : '—'
}

function severityColor(s: string) {
  return { LOW: '#64b5f6', MEDIUM: '#ffa726', HIGH: '#ff7043', CRITICAL: '#ef5350' }[s] ?? '#9e9e9e'
}

function statusColor(s: string) {
  return { OPEN: 'default', IN_PROGRESS: 'blue', RESOLVED: 'green' }[s] ?? 'default'
}
</script>

<style scoped>
.field-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  color: rgba(255,255,255,0.3);
  margin-bottom: 3px;
}

.field-value {
  font-size: 0.875rem;
  color: rgba(255,255,255,0.85);
  line-height: 1.4;
}

.mono {
  font-family: monospace;
  font-size: 0.8rem;
}

.code-pre {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  padding: 12px;
  border-radius: 6px;
  font-size: 11px;
  font-family: monospace;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  color: rgba(255,255,255,0.7);
  line-height: 1.6;
}
</style>
