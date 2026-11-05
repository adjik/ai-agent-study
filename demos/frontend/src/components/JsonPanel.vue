<script setup lang="ts">
defineProps<{
  value: unknown
}>()

function pretty(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}
</script>

<template>
  <pre class="json-block">{{ pretty(value) }}</pre>
</template>
