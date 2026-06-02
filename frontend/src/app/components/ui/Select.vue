<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: [String, Number],
  options: { type: Array, required: true },
  placeholder: { type: String, default: '请选择' },
  size: { type: String, default: 'md' },
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const rootRef = ref(null)

const currentLabel = computed(() => {
  const found = props.options.find(o => o.value === props.modelValue)
  return found ? found.label : props.placeholder
})

function toggle() { open.value = !open.value }
function pick(o) {
  emit('update:modelValue', o.value)
  open.value = false
}

function onDocClick(e) {
  if (rootRef.value && !rootRef.value.contains(e.target)) open.value = false
}
onMounted(() => document.addEventListener('mousedown', onDocClick))
onUnmounted(() => document.removeEventListener('mousedown', onDocClick))
</script>

<template>
  <div ref="rootRef" class="relative inline-block w-full">
    <button
      type="button"
      class="flex w-full items-center justify-between rounded-md border border-gray-600 bg-white/10 px-3 text-white text-xs"
      :class="size === 'sm' ? 'h-7' : 'h-8'"
      @click="toggle"
    >
      <span :class="!modelValue ? 'text-gray-400' : ''">{{ currentLabel }}</span>
      <svg class="w-3 h-3 ml-2 opacity-70" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>
    <transition name="fade">
      <ul
        v-if="open"
        class="absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-md border border-[#06B6D4] bg-[#232E3F] shadow-lg py-1"
      >
        <li
          v-for="o in options"
          :key="o.value"
          class="cursor-pointer px-3 py-1.5 text-xs text-[#E2E8F0] hover:bg-[#06B6D4]/20"
          :class="o.value === modelValue ? 'bg-[#06B6D4]/30 text-white' : ''"
          @click="pick(o)"
        >
          {{ o.label }}
        </li>
      </ul>
    </transition>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.12s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
