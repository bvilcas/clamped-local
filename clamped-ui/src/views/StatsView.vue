<template>
  <v-container fluid>
    <div class="d-flex align-center mb-4">
      <div class="text-h5 font-weight-semibold">Stats</div>
    </div>

    <v-progress-circular v-if="loading" indeterminate class="mt-8 d-block mx-auto" />

    <template v-if="stats">

      <!-- Summary cards -->
      <v-row class="mb-3" dense>
        <v-col cols="6" sm="3">
          <div class="card-wrap">
            <v-card class="stat-card" variant="flat">
              <v-card-text class="pa-3 text-center">
                <div class="stat-number text-white">{{ total }}</div>
                <div class="stat-label">Total Events</div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
        <v-col v-for="item in statusItems" :key="item.status" cols="6" sm="3">
          <div class="card-wrap" style="cursor:pointer" @click="go(item.status)">
            <v-card class="stat-card clickable" variant="flat">
              <v-card-text class="pa-3 text-center">
                <div class="stat-number" :style="{ color: item.color }">{{ item.count }}</div>
                <div class="stat-label">{{ item.label }}</div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>

      <!-- Timeline -->
      <v-row class="mb-3" dense>
        <v-col cols="12">
          <div class="card-wrap">
            <v-card variant="flat">
              <v-card-text class="pa-3">
                <div class="section-title mb-2">Events — Last 24h</div>
                <div v-if="!stats.timeline || stats.timeline.length === 0" class="empty-state py-1">No events in the last 24 hours</div>
                <div style="height:90px;position:relative">
                  <Bar :data="chartData" :options="chartOptions" />
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>

      <!-- Severity + Tags -->
      <v-row class="mb-3" dense>
        <v-col cols="12" md="6">
          <div class="card-wrap h-100">
            <v-card variant="flat" height="100%">
              <v-card-text class="pa-3">
                <div class="section-title mb-2">By Severity</div>
                <div v-for="(count, severity) in severityOrdered" :key="severity" class="sev-row mb-2">
                  <div class="sev-dot" :style="{ background: severityColor(String(severity)) }" />
                  <span class="sev-label" :style="{ color: severityColor(String(severity)) }">{{ severity }}</span>
                  <div class="sev-bar-track">
                    <div class="sev-bar-fill" :style="{ width: (count / maxSeverity * 100) + '%', background: severityColor(String(severity)) }" />
                  </div>
                  <span class="sev-count">{{ count }}</span>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
        <v-col cols="12" md="6">
          <div class="card-wrap h-100">
            <v-card variant="flat" height="100%">
              <v-card-text class="pa-3">
                <div class="section-title mb-2">Top Tags</div>
                <div v-if="stats.topTags.length === 0" class="empty-state">No tags yet</div>
                <div class="tag-grid">
                  <div v-for="t in stats.topTags.slice(0, 12)" :key="t.tag" class="tag-pill" style="cursor:pointer" @click="goTag(t.tag)">
                    <span class="tag-pill-name">{{ t.tag }}</span>
                    <span class="tag-pill-count">{{ t.count }}</span>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>

      <!-- Exception classes + Hot spots -->
      <v-row dense>
        <v-col cols="12" md="6">
          <div class="card-wrap h-100">
            <v-card variant="flat" height="100%">
              <v-card-text class="pa-3">
                <div class="section-title mb-2">Top Exception Classes</div>
                <div v-if="!stats.topExceptionClasses || stats.topExceptionClasses.length === 0" class="empty-state">No exceptions captured</div>
                <div v-for="(t, i) in (stats.topExceptionClasses ?? []).slice(0, 5)" :key="t.tag" class="rank-row">
                  <span class="rank-index">{{ i + 1 }}</span>
                  <span class="rank-name mono">{{ shortClass(t.tag) }}</span>
                  <span class="rank-count">{{ t.count }}</span>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
        <v-col cols="12" md="6">
          <div class="card-wrap h-100">
            <v-card variant="flat" height="100%">
              <v-card-text class="pa-3">
                <div class="section-title mb-2">Hot Spots</div>
                <div v-if="!stats.topSourceLocations || stats.topSourceLocations.length === 0" class="empty-state">No source info captured</div>
                <div v-for="(t, i) in (stats.topSourceLocations ?? []).slice(0, 5)" :key="t.tag" class="rank-row">
                  <span class="rank-index">{{ i + 1 }}</span>
                  <span class="rank-name mono">{{ t.tag }}</span>
                  <span class="rank-count">{{ t.count }}</span>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>

    </template>

  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAppTheme } from '../theme'
import { Bar } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Tooltip } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import { statsApi } from '../api/client'
import type { StatsResponse } from '../types'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, ChartDataLabels)

const stats = ref<StatsResponse | null>(null)
const loading = ref(false)
const router = useRouter()
const { isDark, severityColor, statusColorHex } = useAppTheme()

onMounted(async () => {
  loading.value = true
  try { stats.value = await statsApi.get() }
  finally { loading.value = false }
})


const total = computed(() =>
  stats.value ? Object.values(stats.value.countByStatus).reduce((a, b) => a + b, 0) : 0
)

const statusItems = computed(() => {
  if (!stats.value) return []
  const meta = [
    { status: 'OPEN',        label: 'Open'        },
    { status: 'IN_PROGRESS', label: 'In Progress' },
    { status: 'RESOLVED',    label: 'Resolved'    },
  ]
  return meta.map(m => ({
    ...m,
    color: statusColorHex(m.status),
    count: stats.value!.countByStatus[m.status] ?? 0,
  }))
})

const SEVERITY_LEVEL: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }

const severityOrdered = computed(() => {
  if (!stats.value) return {}
  return Object.fromEntries(
    Object.entries(stats.value.countBySeverity)
      .sort(([a], [b]) => (SEVERITY_LEVEL[a] ?? 9) - (SEVERITY_LEVEL[b] ?? 9))
  )
})

const maxSeverity = computed(() => stats.value ? Math.max(...Object.values(stats.value.countBySeverity)) : 1)

const chartData = computed(() => ({
  labels: stats.value?.timeline.map(p => p.hour) ?? [],
  datasets: [{
    data: stats.value?.timeline.map(p => p.count) ?? [],
    backgroundColor: 'rgba(121, 134, 203, 0.65)',
    borderRadius: 4,
  }]
}))

const chartOptions = computed(() => {
  const tickColor = isDark.value ? 'rgba(255,255,255,0.45)' : 'rgba(0,0,0,0.45)'
  const gridColor = isDark.value ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'
  const labelColor = isDark.value ? 'rgba(255,255,255,0.9)' : 'rgba(0,0,0,0.75)'
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      datalabels: {
        anchor: 'end' as const,
        align: 'end' as const,
        color: labelColor,
        font: { size: 10, weight: 'bold' as const },
        formatter: (value: number) => value > 0 ? value : '',
      },
    },
    scales: {
      x: { grid: { color: gridColor }, ticks: { color: tickColor, font: { size: 10 } } },
      y: { grid: { color: gridColor }, ticks: { color: tickColor, stepSize: 1, font: { size: 10 } }, grace: '15%' },
    },
  }
})

function go(status: string) {
  router.push({ path: '/', query: { status } })
}

function goTag(tag: string) {
  router.push({ path: '/', query: { tag } })
}


// Trims a fully qualified class name down to the last two segments for display.
// e.g. "java.lang.NullPointerException" → "…lang.NullPointerException"
function shortClass(fqn: string) {
  if (!fqn) return fqn
  const parts = fqn.split('.')
  return parts.length > 2 ? '…' + parts.slice(-2).join('.') : fqn
}
</script>

<style>
.v-theme--light .stat-label,
.v-theme--light .section-title,
.v-theme--light .empty-state {
  color: rgba(0, 0, 0, 0.6) !important;
}

.v-theme--light .stat-number.text-white {
  color: rgba(0, 0, 0, 0.87) !important;
}

.v-theme--light .sev-bar-track {
  background: rgba(0, 0, 0, 0.08) !important;
}

.v-theme--light .sev-count {
  color: rgba(0, 0, 0, 0.87) !important;
}

.v-theme--light .tag-pill {
  background: rgba(0, 0, 0, 0.04) !important;
}

.v-theme--light .tag-pill:hover {
  background: rgba(0, 0, 0, 0.08) !important;
}

.v-theme--light .tag-pill-name {
  color: rgba(0, 0, 0, 0.87) !important;
}

.v-theme--light .tag-pill-count {
  color: rgba(0, 0, 0, 0.87) !important;
  background: rgba(0, 0, 0, 0.08) !important;
}


.v-theme--light .rank-index {
  color: rgba(0, 0, 0, 0.45) !important;
}

.v-theme--light .rank-name {
  color: rgba(0, 0, 0, 0.87) !important;
}

.v-theme--light .rank-count {
  color: rgba(0, 0, 0, 0.87) !important;
}

</style>

<style scoped>
/* ── Card wrappers (border owner) ── */
.card-wrap {
  border: 1px solid var(--clamped-border);
  border-radius: 4px;
  overflow: hidden;
}

.h-100 {
  height: 100%;
}

/* ── Stat cards ── */
.stat-card.clickable {
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.stat-card.clickable:hover {
  background: rgba(255,255,255,0.03);
}
.stat-number {
  font-size: 1.9rem;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  color: rgba(255,255,255,0.55);
}

/* ── Section titles ── */
.section-title {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(255,255,255,0.5);
}

.empty-state {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.5);
  padding: 8px 0;
}

/* ── Severity rows ── */
.sev-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sev-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.sev-label {
  font-size: 0.75rem;
  font-weight: 700;
  width: 64px;
  flex-shrink: 0;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  opacity: 1;
}
.sev-bar-track {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: rgba(255,255,255,0.06);
  overflow: hidden;
}
.sev-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}
.sev-count {
  font-size: 0.8rem;
  color: rgba(255,255,255,0.9);
  width: 24px;
  text-align: right;
  flex-shrink: 0;
}

/* ── Tag pills ── */
.tag-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 90px;
  overflow: hidden;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid var(--clamped-border);
  background: rgba(255,255,255,0.04);
  transition: background 0.15s ease, border-color 0.15s ease;
}
.tag-pill:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.22);
}
.tag-pill-name {
  font-size: 0.8rem;
  color: rgba(255,255,255,0.95);
}
.tag-pill-count {
  font-size: 0.72rem;
  font-weight: 700;
  color: rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 1px 6px;
}

/* ── Ranked lists ── */
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px solid var(--clamped-border);
}
.rank-row:last-child {
  border-bottom: none;
}
.rank-index {
  font-size: 0.7rem;
  font-weight: 700;
  color: rgba(255,255,255,0.35);
  width: 16px;
  flex-shrink: 0;
  text-align: right;
}
.rank-name {
  flex: 1;
  font-size: 0.82rem;
  color: rgba(255,255,255,0.95);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-count {
  font-size: 0.78rem;
  font-weight: 600;
  color: rgba(255,255,255,0.75);
  flex-shrink: 0;
}
.mono {
  font-family: monospace;
}

</style>
