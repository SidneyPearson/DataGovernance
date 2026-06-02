<script setup>
defineProps({
  title: String,
  showBorder: { type: Boolean, default: false },
})
</script>

<template>
  <div
    class="rounded-xl p-4 transition-colors"
    :class="showBorder ? 'border border-[#22D3EE]/40' : 'border border-white/[0.06]'"
    style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; box-shadow: 0 1px 0 rgba(255,255,255,0.03) inset, 0 8px 24px rgba(0,0,0,0.25)"
  >
    <div v-if="title" class="flex items-center gap-2 mb-3">
      <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
      <h3 class="text-[13px] font-medium tracking-wide" style="color: #CBD5E1">{{ title }}</h3>
    </div>
    <slot />
  </div>
</template>
