<template>
  <v-app>
    <v-navigation-drawer permanent width="220">

      <!-- Logo -->
      <div class="nav-logo-wrap">
        <img src="/logo.png" alt="Clamped!" class="nav-logo" />
      </div>

      <v-divider />

      <!-- Nav links -->
      <v-list nav class="mt-2">
        <v-list-item prepend-icon="mdi-format-list-bulleted" title="Events" rounded="lg" @click="goEvents()" />
        <v-list-item to="/stats" prepend-icon="mdi-chart-bar"            title="Stats"   rounded="lg" />
      </v-list>

      <v-divider class="mt-2" />

      <!-- Quick filters -->
      <div class="nav-section-label">Quick Filters</div>
      <v-list nav density="compact">
        <v-list-item prepend-icon="mdi-alert-circle-outline"  title="Open"        @click="go('/?status=OPEN')"         rounded="lg" />
        <v-list-item prepend-icon="mdi-progress-clock"        title="In Progress" @click="go('/?status=IN_PROGRESS')" rounded="lg" />
        <v-list-item prepend-icon="mdi-check-circle-outline"  title="Resolved"    @click="go('/?status=RESOLVED')"     rounded="lg" />
      </v-list>

      <v-divider class="mt-2" />

      <!-- Maintenance -->
      <div class="nav-section-label">Maintenance</div>
      <v-list nav density="compact">
        <v-list-item prepend-icon="mdi-refresh" title="Seed Sample Data" @click="doSeed" rounded="lg" :disabled="seeding" />
        <v-list-item prepend-icon="mdi-delete-sweep" title="Purge Resolved" @click="purgeDialog = true" rounded="lg" class="text-error" />
      </v-list>

      <v-spacer />

      <!-- Footer -->
      <div class="nav-footer">
        <div class="nav-footer-line">Clamped! Local</div>
        <div class="nav-footer-sub">v1.0.0</div>
      </div>

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
import { maintenanceApi } from './api/client'

const router = useRouter()
const purgeDialog = ref(false)
const purging = ref(false)
const purgeDays = ref(30)
const seeding = ref(false)

const eventsKey = ref(0)

function goEvents() {
  router.push({ path: '/', query: {} })
  eventsKey.value++
}

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

<style scoped>
.nav-logo-wrap {
  padding: 20px 16px 16px;
  display: flex;
  align-items: center;
}

.nav-logo {
  width: 130px;
  height: auto;
  filter: invert(1) brightness(0.85);
}

.nav-section-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(255,255,255,0.25);
  padding: 12px 16px 4px;
}

.nav-footer {
  padding: 12px 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.07);
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
</style>
