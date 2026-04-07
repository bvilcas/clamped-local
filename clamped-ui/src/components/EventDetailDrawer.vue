<template>
  <v-navigation-drawer v-model="open" location="right" width="520" temporary :class="{ 'drawer-light': !isDark }">
    <template v-if="event">
      <v-toolbar color="surface" elevation="0" density="compact">
        <v-toolbar-title class="text-subtitle-1 font-weight-bold">Event #{{ event.id }}</v-toolbar-title>
        <template #append>
          <v-btn icon size="small" variant="text" @click="open = false"><v-icon>mdi-close</v-icon></v-btn>
        </template>
      </v-toolbar>

      <div class="ev-divider" />

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

        <div class="ev-divider" style="margin-bottom: 16px" />

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
          <div class="ev-divider" style="margin: 16px 0" />
          <div class="field-label mb-1">Source</div>
          <div class="field-value mono">{{ event.sourceFile }}:{{ event.sourceLine }} ({{ event.sourceMethod }})</div>
        </template>

        <!-- Stack trace -->
        <template v-if="event.stacktrace">
          <div class="ev-divider" style="margin: 16px 0" />
          <div class="field-label mb-2">Stack Trace</div>
          <pre class="code-pre">{{ event.stacktrace }}</pre>
        </template>

        <!-- Metadata -->
        <template v-if="event.metadata && event.metadata !== '{}'">
          <div class="ev-divider" style="margin: 16px 0" />
          <div class="field-label mb-2">Metadata</div>
          <pre class="code-pre">{{ prettyMetadata }}</pre>
        </template>

        <!-- Resolution note -->
        <template v-if="event.resolutionNotes">
          <div class="ev-divider" style="margin: 16px 0" />
          <div class="field-label mb-2">{{ event.status === 'RESOLVED' ? 'Resolution Note' : 'Previous Resolution Note' }}</div>
          <div class="field-value" style="white-space: pre-wrap;">{{ event.resolutionNotes }}</div>
        </template>

      </v-container>
    </template>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClampedEvent } from '../types'
import { useAppTheme } from '../theme'

const { isDark, severityColor, statusColor } = useAppTheme()

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

</script>

<style scoped>
.field-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(255,255,255,0.25);
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


.ev-divider {
  border: none;
  border-top: 1px solid var(--clamped-border);
  margin: 0;
}

.drawer-light .field-label {
  color: rgba(0, 0, 0, 0.6);
}

.drawer-light .field-value {
  color: rgba(0, 0, 0, 0.87);
}

.drawer-light .code-pre {
  background: rgba(0, 0, 0, 0.04);
  border-color: rgba(0, 0, 0, 0.1);
  color: rgba(0, 0, 0, 0.75);
}
</style>
