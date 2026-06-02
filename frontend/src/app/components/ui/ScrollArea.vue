<script setup>
defineProps({
  height: { type: String, default: '' },
})
</script>

<template>
  <div class="ui-scrollarea overflow-auto" :style="height ? { height } : null">
    <slot />
  </div>
</template>

<style scoped>
.ui-scrollarea::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.ui-scrollarea::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}
.ui-scrollarea::-webkit-scrollbar-thumb {
  background: rgba(6, 182, 212, 0.4);
  border-radius: 3px;
}
.ui-scrollarea::-webkit-scrollbar-thumb:hover {
  background: rgba(6, 182, 212, 0.7);
}
</style>
