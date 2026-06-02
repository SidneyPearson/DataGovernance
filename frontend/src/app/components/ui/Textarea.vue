<script setup>
defineProps({
  modelValue: [String, Number],
  placeholder: String,
  maxlength: [Number, String],
})
defineEmits(['update:modelValue'])
</script>

<template>
  <textarea
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    class="flex w-full rounded-md border border-gray-600 bg-white/10 px-3 py-2 text-sm text-white placeholder:text-gray-400 focus:outline-none focus:ring-1 focus:ring-[#06B6D4] resize-none"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>
