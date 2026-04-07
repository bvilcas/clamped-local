import { computed } from 'vue'
import { useTheme } from 'vuetify'

export function useAppTheme() {
  const theme = useTheme()
  const isDark = computed(() => theme.global.current.value.dark)

  const severityColors = computed(() =>
    isDark.value
      ? { LOW: '#64b5f6', MEDIUM: '#ffa726', HIGH: '#ff7043', CRITICAL: '#ef5350', DEFAULT: '#9e9e9e' }
      : { LOW: '#1565c0', MEDIUM: '#e08200', HIGH: '#e53935', CRITICAL: '#b71c1c', DEFAULT: '#616161' }
  )

  const statusColors = computed(() =>
    isDark.value
      ? { OPEN: 'default', IN_PROGRESS: 'blue',          RESOLVED: 'green' }
      : { OPEN: 'default', IN_PROGRESS: 'blue-darken-2', RESOLVED: 'green-darken-2' }
  )

  // Hex colors for non-Vuetify use (stat cards, etc.)
  const statusHex = computed(() =>
    isDark.value
      ? { OPEN: '#e0e0e0', IN_PROGRESS: '#42a5f5', RESOLVED: '#66bb6a' }
      : { OPEN: '#757575', IN_PROGRESS: '#1565c0', RESOLVED: '#388e3c' }
  )

  const resolveColor = computed(() => isDark.value ? 'green' : 'green-darken-2')

  function severityColor(s: string): string {
    return severityColors.value[s as keyof typeof severityColors.value] ?? severityColors.value.DEFAULT
  }

  function statusColor(s: string): string {
    return statusColors.value[s as keyof typeof statusColors.value] ?? 'default'
  }

  function statusColorHex(s: string): string {
    return statusHex.value[s as keyof typeof statusHex.value] ?? '#9e9e9e'
  }

  return { isDark, severityColor, statusColor, statusColorHex, resolveColor }
}
