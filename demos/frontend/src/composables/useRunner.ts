import { ref } from 'vue'
import { formatError } from '@/api/http'

export function useRunner<T>(fn: () => Promise<T>) {
  const loading = ref(false)
  const error = ref('')
  const data = ref<T | null>(null)
  const elapsedMs = ref(0)

  async function run() {
    loading.value = true
    error.value = ''
    const started = performance.now()
    try {
      data.value = await fn()
    } catch (e) {
      error.value = formatError(e)
      data.value = null
    } finally {
      elapsedMs.value = Math.round(performance.now() - started)
      loading.value = false
    }
  }

  return { loading, error, data, elapsedMs, run }
}
