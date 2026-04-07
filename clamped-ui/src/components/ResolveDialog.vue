<template>
  <v-dialog :model-value="open" max-width="480" persistent @update:model-value="v => { if (!v) emit('close') }">
    <v-card>
      <v-card-title>Resolve Event #{{ event?.id }}</v-card-title>
      <v-card-text>
        <v-textarea
          v-model="notes"
          label="Resolution note (optional)"
          placeholder="How was this resolved?"
          rows="4"
          variant="outlined"
          density="compact"
          hide-details
        />
      </v-card-text>
      <v-card-actions class="px-4 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="emit('close')">Cancel</v-btn>
        <v-btn :color="resolveColor" variant="flat" :loading="saving" @click="confirm">Resolve</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { ClampedEvent } from '../types'
import { eventsApi } from '../api/client'
import { useAppTheme } from '../theme'

const { resolveColor } = useAppTheme()

const props = defineProps<{ event: ClampedEvent | null }>()
const emit = defineEmits<{ close: [], resolved: [] }>()

const notes = ref('')
const saving = ref(false)

const open = computed(() => props.event !== null)

watch(() => props.event, () => { notes.value = '' })

async function confirm() {
  if (!props.event) return
  saving.value = true
  try {
    await eventsApi.resolve(props.event.id, notes.value || undefined)
    emit('resolved')
    emit('close')
  } finally {
    saving.value = false
  }
}
</script>
