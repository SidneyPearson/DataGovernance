<script setup>
defineProps({
  size: { type: String, default: 'md' },
  variant: { type: String, default: 'default' },
  disabled: { type: Boolean, default: false },
})
</script>

<template>
  <button
    class="ui-button inline-flex items-center justify-center rounded-md transition-colors disabled:opacity-50 disabled:pointer-events-none"
    :class="[
      size === 'sm' ? 'h-7 px-2 text-xs' : size === 'icon' ? 'h-9 w-9 p-0' : 'h-9 px-3 text-sm',
    ]"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>
