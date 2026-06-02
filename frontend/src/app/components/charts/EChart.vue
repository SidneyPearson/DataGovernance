<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '100%' },
  width: { type: String, default: '100%' },
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

function init() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value, null, { renderer: 'canvas' })
  chart.setOption(props.option)
}

function handleResize() {
  if (chart) chart.resize()
}

onMounted(() => {
  init()
  resizeObserver = new ResizeObserver(handleResize)
  resizeObserver.observe(chartRef.value)
})

onBeforeUnmount(() => {
  if (resizeObserver) resizeObserver.disconnect()
  if (chart) chart.dispose()
  chart = null
})

watch(
  () => props.option,
  (val) => {
    if (chart) chart.setOption(val, true)
  },
  { deep: true }
)
</script>

<template>
  <div ref="chartRef" :style="{ width, height }"></div>
</template>
