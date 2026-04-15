<template>
  <v-app>
    <v-navigation-drawer permanent :width="collapsed ? 56 : 200" :rail="collapsed" rail-width="56" style="overflow: visible;" :class="{ 'nav-light': !isDark }">

      <!-- Collapse toggle on right edge -->
      <div class="nav-edge-toggle" :style="{ left: (collapsed ? 56 : 200) - 12 + 'px' }" @click="collapsed = !collapsed">
        <v-icon size="14">{{ collapsed ? 'mdi-chevron-right' : 'mdi-chevron-left' }}</v-icon>
      </div>

      <!-- Logo -->
      <div class="nav-logo-wrap">
        <Transition name="fade" mode="out-in">
          <img v-if="!collapsed" key="full" src="/logo.png" alt="Clamped!" class="nav-logo" />
          <img v-else key="small" src="/small_logo.png" alt="Clamped!" class="nav-logo-small" />
        </Transition>
      </div>

      <div class="nav-divider" />

      <!-- Nav links -->
      <v-list nav class="mt-2">
        <v-list-item prepend-icon="mdi-format-list-bulleted" title="Events" rounded="lg" @click="goEvents()" />
        <v-list-item to="/stats" prepend-icon="mdi-chart-bar" title="Stats" rounded="lg" />
      </v-list>

      <div class="nav-divider mt-2" />

      <!-- Quick filters -->
      <Transition name="fade"><div v-if="!collapsed" class="nav-section-label">Quick Filters</div></Transition>
      <v-list nav density="compact">
        <v-list-item prepend-icon="mdi-alert-circle-outline"  title="Open"        @click="go('/?status=OPEN')"        rounded="lg" />
        <v-list-item prepend-icon="mdi-progress-clock"        title="In Progress" @click="go('/?status=IN_PROGRESS')" rounded="lg" />
        <v-list-item prepend-icon="mdi-check-circle-outline"  title="Resolved"    @click="go('/?status=RESOLVED')"    rounded="lg" />
      </v-list>

      <div class="nav-divider mt-2" />

      <!-- Maintenance -->
      <Transition name="fade"><div v-if="!collapsed" class="nav-section-label">Maintenance</div></Transition>
      <v-list nav density="compact">
        <v-list-item prepend-icon="mdi-refresh"      title="Seed Sample Data" @click="doSeed"            rounded="lg" :disabled="seeding" />
        <v-list-item prepend-icon="mdi-trash-can-outline" title="Purge Resolved"   @click="purgeDialog = true" rounded="lg" class="text-error" />
      </v-list>

      <div class="nav-divider mt-2" />

      <!-- Appearance -->
      <Transition name="fade"><div v-if="!collapsed" class="nav-section-label">Appearance</div></Transition>
      <v-list nav density="compact">
        <v-list-item
          :prepend-icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
          :title="isDark ? 'Light Mode' : 'Dark Mode'"
          rounded="lg"
          @click="toggleTheme"
        />
      </v-list>

      <v-spacer />

      <!-- Footer -->
      <Transition name="fade">
        <div v-if="!collapsed" class="nav-footer">
          <div class="nav-footer-line">Clamped! Local</div>
          <div class="nav-footer-sub">v1.0.0</div>
        </div>
      </Transition>

    </v-navigation-drawer>

    <v-main>
      <router-view :key="eventsKey" />
    </v-main>

    <!-- Purge dialog -->
    <v-dialog v-model="purgeDialog" max-width="400">
      <v-card>
        <v-card-title>Purge Resolved Events</v-card-title>
        <v-card-text>
          <p class="mb-2">Permanently delete resolved events that were first seen more than:</p>
          <v-select v-model="purgeDays" :items="[{ title: '7 days ago', value: 7 }, { title: '30 days ago', value: 30 }, { title: '90 days ago', value: 90 }]" density="compact" hide-details class="mb-3" />
          <p class="text-caption text-medium-emphasis">Events resolved within the selected period will be kept. This cannot be undone.</p>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="purgeDialog = false">Cancel</v-btn>
          <v-btn color="error" :loading="purging" @click="doPurge">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from 'vuetify'
import { maintenanceApi } from './api/client'
import { useAppTheme } from './theme'

const router = useRouter()
const theme = useTheme()
const { isDark } = useAppTheme()

function toggleTheme() {
  const next = isDark.value ? 'light' : 'dark'
  theme.global.name.value = next
  localStorage.setItem('theme', next)
}
const purgeDialog = ref(false)
const purging = ref(false)
const purgeDays = ref(30)
const seeding = ref(false)

// Incrementing eventsKey forces the router-view to remount EventsView and re-fetch,
// which is how seed and purge refresh the events list without a full page reload
const eventsKey = ref(0)
const collapsed = ref(false)

function goEvents() {
  router.push({ path: '/', query: {} })
  eventsKey.value++
}

// Splits a "/path?key=val" string into a structured router push so quick filters
// arrive at EventsView with the right query params pre-applied
function go(path: string) {
  const [pathname, search] = path.split('?')
  const query = search ? Object.fromEntries(new URLSearchParams(search)) : {}
  router.push({ path: pathname, query })
}

async function doSeed() {
  seeding.value = true
  try {
    const result = await maintenanceApi.seed()
    alert(`Seeded ${result.seeded} sample events`)
    eventsKey.value++
  } finally {
    seeding.value = false
  }
}

async function doPurge() {
  purging.value = true
  try {
    const result = await maintenanceApi.purge(purgeDays.value)
    purgeDialog.value = false
    alert(`Deleted ${result.deleted} resolved events`)
    // refresh whatever view is currently open
    eventsKey.value++
  } finally {
    purging.value = false
  }
}
</script>

<style>
:root {
  --clamped-border: rgba(255,255,255,0.12);
}
.v-theme--light {
  --clamped-border: rgba(0,0,0,0.12);
}

.v-theme--light .v-main {
  background: #f0f0f0 !important;
}
</style>

<style scoped>
.nav-logo-wrap {
  padding: 14px 12px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-logo-small {
  width: 28px;
  height: auto;
  filter: brightness(0) invert(1);
}

.nav-edge-toggle {
  position: fixed;
  top: 50vh;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #2a2a2a;
  border: 1px solid var(--clamped-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  opacity: 0.6;
  transition: left 0.225s ease, opacity 0.15s;
}

.nav-edge-toggle:hover {
  opacity: 1;
}

.nav-logo {
  width: 130px;
  height: auto;
  filter: brightness(0) invert(1);
}

.nav-section-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(255,255,255,0.25);
  padding: 8px 12px 2px;
}

:deep(.v-list-item__prepend) {
  margin-inline-end: -8px;
}

.nav-footer {
  padding: 10px 12px 16px;
  border-top: 1px solid var(--clamped-border);
}

.nav-footer-line {
  font-size: 0.75rem;
  font-weight: 600;
  color: rgba(255,255,255,0.35);
}

.nav-footer-sub {
  font-size: 0.68rem;
  color: rgba(255,255,255,0.2);
  margin-top: 2px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Light mode overrides for custom nav elements */
.nav-light :deep(.v-list-item__prepend .v-icon) {
  color: rgba(0, 0, 0, 0.6) !important;
  opacity: 1 !important;
}

.nav-light :deep(.v-list-item-title) {
  color: #000;
}

.nav-light :deep(.text-error .v-list-item-title),
.nav-light :deep(.text-error .v-icon) {
  color: rgb(var(--v-theme-error)) !important;
}

.nav-divider {
  border: none;
  border-top: 1px solid var(--clamped-border);
  margin-left: 0;
  margin-right: 0;
}

.nav-light .nav-section-label {
  color: rgba(0, 0, 0, 0.6);
}

.nav-light .nav-edge-toggle {
  background: #ebebeb;
}

.nav-light .nav-footer-line {
  color: rgba(0, 0, 0, 0.65);
}

.nav-light .nav-footer-sub {
  color: rgba(0, 0, 0, 0.5);
}

.nav-light .nav-logo,
.nav-light .nav-logo-small {
  filter: none;
}
</style>
