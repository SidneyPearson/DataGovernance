<script setup>
import { onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  maxWidth: { type: String, default: '600px' },
})
const emit = defineEmits(['update:modelValue'])

function close() { emit('update:modelValue', false) }

function onKey(e) {
  if (e.key === 'Escape' && props.modelValue) close()
}
onMounted(() => document.addEventListener('keydown', onKey))
onUnmounted(() => document.removeEventListener('keydown', onKey))

watch(() => props.modelValue, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
})
</script>

<template>
  <teleport to="body">
    <transition name="dialog-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm"
        @click.self="close"
      >
        <div
          class="relative bg-[#232E3F] border border-[#06B6D4] rounded-lg p-6 mx-4 w-full"
          :style="{ maxWidth }"
        >
          <button
            class="absolute top-3 right-3 text-[#E2E8F0] hover:text-white"
            @click="close"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
          <slot />
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity 0.18s; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; }
</style>
