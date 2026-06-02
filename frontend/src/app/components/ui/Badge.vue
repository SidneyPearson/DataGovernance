<script setup>
defineProps({
  color: { type: String, default: '#06B6D4' },
  textColor: { type: String, default: '#fff' },
})
</script>

<template>
  <span
    class="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap"
    :style="{ backgroundColor: color, color: textColor }"
  >
    <slot />
  </span>
</template>
