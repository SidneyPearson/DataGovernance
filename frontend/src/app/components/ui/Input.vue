<script setup>
defineProps({
  modelValue: [String, Number],
  type: { type: String, default: 'text' },
  placeholder: String,
  maxlength: [Number, String],
})
defineEmits(['update:modelValue'])
</script>

<template>
  <input
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    class="ui-input flex w-full rounded-md border border-gray-600 bg-white/10 px-3 py-1 text-sm text-white placeholder:text-gray-400 focus:outline-none focus:ring-1 focus:ring-[#06B6D4] disabled:opacity-50"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<style scoped>
.ui-input::-webkit-calendar-picker-indicator {
  filter: invert(0.8);
  cursor: pointer;
}
</style>
