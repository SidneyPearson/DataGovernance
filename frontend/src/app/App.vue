<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import SectionCard from './components/SectionCard.vue'
import UiButton from './components/ui/Button.vue'
import UiInput from './components/ui/Input.vue'
import UiTextarea from './components/ui/Textarea.vue'
import UiSelect from './components/ui/Select.vue'
import UiDialog from './components/ui/Dialog.vue'
import UiBadge from './components/ui/Badge.vue'
import UiScrollArea from './components/ui/ScrollArea.vue'
import EChart from './components/charts/EChart.vue'
import ArrowUp from './components/icons/ArrowUp.vue'
import ArrowDown from './components/icons/ArrowDown.vue'
import RefreshCw from './components/icons/RefreshCw.vue'
import SearchIcon from './components/icons/Search.vue'
import CalendarIcon from './components/icons/Calendar.vue'
import { api } from '@/api'

// 后端枚举到前端展示的映射
const TYPE_LABEL = { TABLE: '库表级联', API: '接口级联', FILE: '文件级联' }
const TYPE_COLOR = { TABLE: '#22D3EE', API: '#34D399', FILE: '#FBBF24' }

// 占位初始数据（加载完成后会被接口数据替换）
const monthlyData = ref([
  { month: '2025-06', value: 98000 }, { month: '2025-07', value: 105000 }, { month: '2025-08', value: 112000 },
  { month: '2025-09', value: 108000 }, { month: '2025-10', value: 115000 }, { month: '2025-11', value: 122000 },
  { month: '2025-12', value: 118000 }, { month: '2026-01', value: 125000 }, { month: '2026-02', value: 128563 },
  { month: '2026-03', value: 126000 }, { month: '2026-04', value: 124000 }, { month: '2026-05', value: 128563 }
])

const distributionData = ref([
  { name: '库表级联', value: 45, color: '#22D3EE', count: 57853, yesterdayChange: 1850, changePercent: 3.2 },
  { name: '接口级联', value: 35, color: '#34D399', count: 45000, yesterdayChange: -820, changePercent: -1.8 },
  { name: '文件级联', value: 20, color: '#FBBF24', count: 25710, yesterdayChange: 643, changePercent: 2.5 }
])

const cascadeWeeklyTrendData = ref([
  { day: '05-12', table: 52000, api: 46200, file: 23800 },
  { day: '05-13', table: 53500, api: 46800, file: 24200 },
  { day: '05-14', table: 54800, api: 46500, file: 24600 },
  { day: '05-15', table: 55200, api: 45900, file: 24900 },
  { day: '05-16', table: 56100, api: 45600, file: 25100 },
  { day: '05-17', table: 56800, api: 45800, file: 25400 },
  { day: '05-18', table: 57853, api: 45000, file: 25710 }
])

const cascadeDetails = ref([
  { name: 'grid_data_01', status: '成功', type: '库表级联', frequency: '每小时', timeRule: '整点执行', totalHistory: 125680, lastAmount: 1256, ratio: 2.1, time: '2024-05-18 09:30:25' },
  { name: 'population_api', status: '成功', type: '接口级联', frequency: '每30分钟', timeRule: '0/30 * * * *', totalHistory: 89420, lastAmount: 890, ratio: 1.8, time: '2024-05-18 09:28:15' },
  { name: 'enterprise_db', status: '失败', type: '库表级联', frequency: '每天', timeRule: '每日凌晨2点', totalHistory: 45230, lastAmount: 0, ratio: 0, time: '2024-05-18 09:25:48' },
  { name: 'geo_interface', status: '成功', type: '接口级联', frequency: '每15分钟', timeRule: '*/15 * * * *', totalHistory: 156790, lastAmount: 1580, ratio: 2.5, time: '2024-05-18 09:22:33' },
  { name: 'traffic_data', status: '成功', type: '文件级联', frequency: '每小时', timeRule: '整点执行', totalHistory: 34520, lastAmount: 345, ratio: 1.3, time: '2024-05-18 09:20:10' }
])

// 总量与质量指标（顶部卡片用）
const cascadeOverviewData = ref({
  total: 0, historyTotal: 0, todayChange: 0, todayChangePercent: 0
})
const todayWorkorderData = ref({
  total: 0, historyTotal: 0, change: 0, changePercent: 0,
  src12345: 0, srcZjb: 0,
  labelFillRate: 0, labelCodeRate: 0, gridInRate: 0, focusCount: 0
})

const dailyTrendData = ref([
  { day: '05-05', 增量: 1856, 总量: 14532 }, { day: '05-06', 增量: 1787, 总量: 15319 },
  { day: '05-07', 增量: 1698, 总量: 16017 }, { day: '05-08', 增量: 1603, 总量: 16620 },
  { day: '05-09', 增量: 1576, 总量: 17196 }, { day: '05-10', 增量: 1534, 总量: 17730 },
  { day: '05-11', 增量: 1465, 总量: 18195 }, { day: '05-12', 增量: 1398, 总量: 18593 },
  { day: '05-13', 增量: 1345, 总量: 18938 }, { day: '05-14', 增量: 1289, 总量: 19227 },
  { day: '05-15', 增量: 1212, 总量: 19439 }, { day: '05-16', 增量: 1176, 总量: 19615 },
  { day: '05-17', 增量: 1098, 总量: 19713 }, { day: '05-18', 增量: 1045, 总量: 19758 }
])

const weeklyTrendData = ref([
  { week: '第1周', 增量: 12456, 总量: 45230 }, { week: '第2周', 增量: 11987, 总量: 57217 },
  { week: '第3周', 增量: 11234, 总量: 68451 }, { week: '第4周', 增量: 10876, 总量: 79327 },
  { week: '第5周', 增量: 10543, 总量: 89870 }, { week: '第6周', 增量: 10234, 总量: 100104 },
  { week: '第7周', 增量: 9876, 总量: 109980 }, { week: '第8周', 增量: 9543, 总量: 119523 }
])

// 各区指标数据（从 /workorder/district 返回，结构与原 mock 字段对齐）
const districtRows = ref([])

function districtDetailedAnalysis(timeType) {
  // timeType 仅作为参数兼容原有调用，实际数据由 districtRows（来自 /workorder/district）提供
  const data = districtRows.value
  const cityTotal = data.reduce((sum, d) => sum + (d.total || 0), 0) || 1
  return data.map(d => ({ ...d, percentage: ((d.total / cityTotal) * 100).toFixed(1) }))
}

// state
const currentTime = new Date().toLocaleString('zh-CN')
const selectedDistrict = ref('all')
const showHistoryDialog = ref(false)
const showWorkOrderDialog = ref(false)
const showNoteDialog = ref(false)
const searchTerm = ref('')
const cascadeTypeFilter = ref('all')
const cascadeSearchTerm = ref('')
const startDate = ref('2024-05-01')
const endDate = ref('2024-05-18')
const showDateRangePicker = ref(false)
const dateRangeLabel = computed(() => `${startDate.value} → ${endDate.value}`)
function toggleDateRangePicker() { showDateRangePicker.value = !showDateRangePicker.value }
function closeDateRangePicker(e) {
  if (!e.target.closest('[data-date-range-picker]')) showDateRangePicker.value = false
}

// 双月份日历状态
const calendarLeftMonth = ref(new Date(2024, 3, 1)) // 4月
const calendarRightMonth = ref(new Date(2024, 4, 1)) // 5月
const rangeAnchor = ref(null) // 当前选择的锚点：第一次点击时记下，第二次点击决定终点
const hoverDate = ref(null)

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function parseDate(s) {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}
function buildCalendarDays(monthDate) {
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth()
  const firstDay = new Date(year, month, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells = []
  for (let i = 0; i < startWeekday; i++) cells.push(null)
  for (let i = 1; i <= daysInMonth; i++) cells.push(new Date(year, month, i))
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}
const leftDays = computed(() => buildCalendarDays(calendarLeftMonth.value))
const rightDays = computed(() => buildCalendarDays(calendarRightMonth.value))
const leftMonthLabel = computed(() => `${calendarLeftMonth.value.getFullYear()}年${calendarLeftMonth.value.getMonth() + 1}月`)
const rightMonthLabel = computed(() => `${calendarRightMonth.value.getFullYear()}年${calendarRightMonth.value.getMonth() + 1}月`)
function shiftLeftMonth(delta) {
  const d = new Date(calendarLeftMonth.value)
  d.setMonth(d.getMonth() + delta)
  calendarLeftMonth.value = d
  const r = new Date(d)
  r.setMonth(r.getMonth() + 1)
  calendarRightMonth.value = r
}
function isSameDay(a, b) {
  return a && b && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
function isInRange(d) {
  if (!d) return false
  const s = parseDate(startDate.value)
  const e = parseDate(endDate.value)
  return d >= s && d <= e
}
function isInHoverRange(d) {
  if (!d || !rangeAnchor.value || !hoverDate.value) return false
  const a = rangeAnchor.value
  const h = hoverDate.value
  const lo = a < h ? a : h
  const hi = a < h ? h : a
  return d >= lo && d <= hi
}
function pickDate(d) {
  if (!d) return
  if (!rangeAnchor.value) {
    rangeAnchor.value = d
    startDate.value = formatDate(d)
    endDate.value = formatDate(d)
  } else {
    const a = rangeAnchor.value
    if (d < a) { startDate.value = formatDate(d); endDate.value = formatDate(a) }
    else { startDate.value = formatDate(a); endDate.value = formatDate(d) }
    rangeAnchor.value = null
    hoverDate.value = null
    showDateRangePicker.value = false
  }
}
const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六']
const workOrderType = ref('all')
const selectedWorkOrder = ref(null)
const districtAnalysisTime = ref('today')
const districtCollectionStatus = ref('all')
const districtUpdateStatus = ref('all')
const trendViewType = ref('daily')
const noteText = ref('')
const activeTab = ref(localStorage.getItem('dashboardActiveTab') || 'cascade')
watch(activeTab, v => localStorage.setItem('dashboardActiveTab', v))

function deferLoad(fn) {
  requestAnimationFrame(() => setTimeout(fn, 0))
}

// 切换 tab 时懒加载对应数据
watch(activeTab, async (tab) => {
  if (tab === 'cascade' && cascadeOverviewData.value.total === 0) {
    deferLoad(() => { loadCascade() })
  } else if (tab === 'workOrder' && todayWorkorderData.value.total === 0) {
    deferLoad(() => { loadWorkorder() })
  }
})

watch(selectedDistrict, () => { loadWorkorder() })

// select options
const cascadeTypeOptions = [
  { value: 'all', label: '全部类型' },
  { value: '库表级联', label: '库表级联' },
  { value: '接口级联', label: '接口级联' },
  { value: '文件级联', label: '文件级联' },
]
const districtOptions = ref([
  { value: 'all', label: '全部区域' },
])
const workOrderTypeOptions = [
  { value: 'all', label: '全部类型' },
  { value: 'hotline', label: '12345市民热线工单' },
  { value: 'cityMgmt', label: '城市综合管理部事业工单' },
]
const collectionStatusOptions = [
  { value: 'all', label: '全部归集状态' },
  { value: 'collected', label: '已归集' },
  { value: 'notCollected', label: '未归集' },
]
const analysisTimeOptions = [
  { value: 'today', label: '今日' },
  { value: 'yesterday', label: '昨日' },
  { value: 'history', label: '历史' },
]
const updateStatusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'updated', label: '已更新' },
  { value: 'notUpdated', label: '未更新' },
]

// filtered cascade list
const filteredCascade = computed(() => {
  return cascadeDetails.value.filter(row => {
    const matchesType = cascadeTypeFilter.value === 'all' || row.type === cascadeTypeFilter.value
    const matchesSearch = cascadeSearchTerm.value === '' ||
      row.name.toLowerCase().includes(cascadeSearchTerm.value.toLowerCase())
    return matchesType && matchesSearch
  })
})

// quality metrics（来自后端 /workorder/today）
const qualityMetrics = computed(() => {
  const t = todayWorkorderData.value
  return {
    tagFillRate: Number(t.labelFillRate || 0).toFixed(1),
    tagCodeRate: Number(t.labelCodeRate || 0).toFixed(1),
    orderStandardRate: Number(t.gridInRate || 0).toFixed(1),
    focusCount: Number(t.focusCount || 0),
    tagFillChange: '+128',
    tagFillTrend: '+128',
    tagCodeChange: '+96',
    tagCodeTrend: '+96',
    orderStandardChange: '+112',
    orderStandardTrend: '+112',
    focusChange: '-8',
    focusTrend: '-8',
  }
})

// district table
const districtTableRows = computed(() => {
  const data = districtDetailedAnalysis(districtAnalysisTime.value)
  const processed = data.map(d => {
    const count = workOrderType.value === 'hotline' ? d.hotline
      : workOrderType.value === 'cityMgmt' ? d.cityMgmt
      : d.total
    return { ...d, displayCount: count }
  })
  const totalCount = processed.reduce((s, d) => s + d.displayCount, 0)
  return processed
    .filter(d =>
      districtCollectionStatus.value === 'all' ? true :
      districtCollectionStatus.value === 'collected' ? d.collected : !d.collected
    )
    .map(row => ({ ...row, percentage: ((row.displayCount / totalCount) * 100).toFixed(1) }))
})

const districtAnalysisRows = computed(() => {
  return districtDetailedAnalysis(districtAnalysisTime.value)
    .filter(d => selectedDistrict.value === 'all' || d.areaCode === selectedDistrict.value)
    .filter(d => {
      if (districtUpdateStatus.value === 'all') return true
      return districtUpdateStatus.value === 'updated' ? d.collected : !d.collected
    })
})

const selectedDistrictRow = computed(() => {
  if (selectedDistrict.value === 'all') return null
  return districtRows.value.find(d => d.areaCode === selectedDistrict.value) || null
})

// chart options
const distributionPieOption = computed(() => ({
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15,26,46,0.95)', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#F1F5F9' }, formatter: '{b}: {c}%' },
  series: [{
    type: 'pie',
    radius: ['58%', '88%'],
    center: ['50%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderColor: '#0F1A2E', borderWidth: 3, borderRadius: 4 },
    label: { show: true, position: 'inside', formatter: '{c}%', color: '#fff', fontSize: 14, fontWeight: 600, fontFamily: 'JetBrains Mono' },
    labelLine: { show: false },
    data: distributionData.value.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } }))
  }]
}))

const cascadeTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,26,46,0.95)', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#F1F5F9' } },
  grid: { top: 12, right: 16, left: 60, bottom: 40 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: cascadeWeeklyTrendData.value.map(d => d.day),
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
    axisTick: { show: false },
    axisLabel: { color: '#64748B', fontSize: 12 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748B', fontSize: 12, fontFamily: 'JetBrains Mono' },
    splitLine: { lineStyle: { color: 'rgba(148,163,184,0.06)' } }
  },
  series: [
    {
      name: '库表级联', type: 'line', smooth: true,
      data: cascadeWeeklyTrendData.value.map(d => d.table),
      lineStyle: { color: '#22D3EE', width: 2 },
      itemStyle: { color: '#22D3EE' },
      symbol: 'circle', symbolSize: 6
    },
    {
      name: '接口级联', type: 'line', smooth: true,
      data: cascadeWeeklyTrendData.value.map(d => d.api),
      lineStyle: { color: '#34D399', width: 2 },
      itemStyle: { color: '#34D399' },
      symbol: 'circle', symbolSize: 6
    },
    {
      name: '文件级联', type: 'line', smooth: true,
      data: cascadeWeeklyTrendData.value.map(d => d.file),
      lineStyle: { color: '#FBBF24', width: 2 },
      itemStyle: { color: '#FBBF24' },
      symbol: 'circle', symbolSize: 6
    },
  ]
}))

const monthlyTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,26,46,0.95)', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#F1F5F9' } },
  grid: { top: 8, right: 36, left: 56, bottom: 36 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: monthlyData.value.map(d => d.month),
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
    axisTick: { show: false },
    axisLabel: { color: '#64748B', fontSize: 12 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748B', fontSize: 12, fontFamily: 'JetBrains Mono' },
    splitLine: { lineStyle: { color: 'rgba(148,163,184,0.06)' } }
  },
  series: [{
    type: 'line', smooth: true,
    data: monthlyData.value.map(d => d.value),
    lineStyle: { color: '#22D3EE', width: 2 },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(34,211,238,0.25)' }, { offset: 1, color: 'rgba(34,211,238,0)' }] } },
    itemStyle: { color: '#0F1A2E', borderColor: '#22D3EE', borderWidth: 2 },
    symbol: 'circle', symbolSize: 7
  }]
}))

const workOrderTypePieOption = computed(() => {
  const t = todayWorkorderData.value.typeDistribution || {}
  const total = Number(t.total || 1)
  const p1 = Number(t['12345Pct'] || 0)
  const p2 = Number(t['ZJBPct'] || 0)
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15,26,46,0.95)', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#F1F5F9' },
      formatter: ({ data }) => `${data.name}: ${data.count.toLocaleString()}条 (${data.value}%)`
    },
    series: [{
      type: 'pie',
      radius: ['52%', '80%'],
      center: ['50%', '50%'],
      itemStyle: { borderColor: '#131E32', borderWidth: 3, borderRadius: 4 },
      label: { show: true, position: 'inside', formatter: '{c}%', color: '#fff', fontSize: 13, fontWeight: 600, fontFamily: 'JetBrains Mono' },
      labelLine: { show: false },
      data: [
        { name: '12345市民热线工单', value: p1, count: Number(t['12345'] || 0), itemStyle: { color: '#34D399' } },
        { name: '城市综合管理部事业工单', value: p2, count: Number(t['ZJB'] || 0), itemStyle: { color: '#22D3EE' } },
      ]
    }]
  }
})

const workOrderTrendOption = computed(() => {
  const list = trendViewType.value === 'daily' ? dailyTrendData.value : weeklyTrendData.value
  const xKey = trendViewType.value === 'daily' ? 'day' : 'week'
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,26,46,0.95)', borderColor: 'rgba(34,211,238,0.3)', borderWidth: 1, textStyle: { color: '#F1F5F9' } },
    grid: { top: 8, right: 50, left: 56, bottom: 36 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: list.map(d => d[xKey]),
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 13 }
    },
    yAxis: [
      { type: 'value', name: '总量', nameTextStyle: { color: '#64748B', fontSize: 13 }, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#64748B', fontSize: 13, fontFamily: 'JetBrains Mono' }, splitLine: { lineStyle: { color: 'rgba(148,163,184,0.06)' } } },
      { type: 'value', name: '增量', nameTextStyle: { color: '#64748B', fontSize: 13 }, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#64748B', fontSize: 13, fontFamily: 'JetBrains Mono' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '总量', type: 'line', smooth: true,
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(34,211,238,0.25)' }, { offset: 1, color: 'rgba(34,211,238,0)' }] } },
        data: list.map(d => d.总量),
        lineStyle: { color: '#22D3EE', width: 2 },
        itemStyle: { color: '#22D3EE' },
        symbol: 'circle', symbolSize: 4
      },
      {
        name: '增量', type: 'line', yAxisIndex: 1,
        data: list.map(d => d.增量),
        lineStyle: { color: '#67E8F9', width: 1.5, type: 'dashed' },
        itemStyle: { color: '#67E8F9' },
        symbol: 'circle', symbolSize: 4
      }
    ]
  }
})

// work order list (used in dialog) - 从 /workorder/list 加载
const workOrderList = ref([])

const filteredWorkOrders = computed(() => {
  if (!searchTerm.value) return workOrderList.value
  const kw = searchTerm.value.toLowerCase()
  return workOrderList.value.filter(w => String(w.id || '').toLowerCase().includes(kw))
})

// history records - 从 /cascade/history 加载
const historyRecords = ref([])
const currentHistoryTask = ref(null)

function selectWorkOrder(w) { selectedWorkOrder.value = w }
function openNoteDialog() {
  showWorkOrderDialog.value = false
  showNoteDialog.value = true
}

async function openHistory(row) {
  currentHistoryTask.value = row
  showHistoryDialog.value = true
  if (row && row.taskCode) {
    const list = await safe(() => api.cascadeHistory(row.taskCode), [])
    historyRecords.value = (list || []).map(r => ({
      time: r.startTime ? formatDateTime(new Date(r.startTime)) : '-',
      amount: Number(r.cascadeCount || 0).toLocaleString(),
      status: r.cascadeStatus,
      duration: r.durationMs ? `${Math.round(Number(r.durationMs) / 1000)}s` : '-',
      raw: r,
    }))
  } else {
    historyRecords.value = []
  }
}

function exportHistory() {
  const task = currentHistoryTask.value
  if (!task) return
  const headers = ['级联时间', '级联量', '状态', '耗时']
  const rows = historyRecords.value.map(r => [
    r.time, r.amount, r.status === 'SUCCESS' ? '成功' : '失败', r.duration
  ])
  const csv = [headers, ...rows].map(row => row.map(cell => `"${cell}"`).join(',')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `级联记录_${task.taskCode}_${formatYmd(new Date())}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

async function openWorkOrderList() {
  showWorkOrderDialog.value = true
  const district = selectedDistrict.value && selectedDistrict.value !== 'all' ? selectedDistrict.value : ''
  const list = await safe(() => api.workorderList({
    areaCode: district, startDate: startDate.value, endDate: endDate.value, pageSize: 50,
  }), [])
  workOrderList.value = (list || []).map((w, i) => ({
    id: w.taskId || `WO-${i}`,
    type: w.srcType === '12345' ? '12345市民热线工单' : '城市综合管理部事业工单',
    form: w.wpSource || '-',
    scope: w.areaName || '-',
    orderCount: w.orderCount || 1,
    processCount: w.processCount || 1,
    time: w.createTime ? formatDateTime(new Date(w.createTime)) : (w.discoverTime ? formatDateTime(new Date(w.discoverTime)) : '-'),
    raw: w,
  }))
}

async function saveRemark() {
  if (!selectedWorkOrder.value) { showNoteDialog.value = false; return }
  const w = selectedWorkOrder.value
  await safe(() => api.saveRemark({
    taskId: w.id,
    srcType: (w.raw && w.raw.srcType) || (w.type === '12345市民热线工单' ? '12345' : 'ZJB'),
    targetType: '工单',
    targetDesc: w.scope || '',
    remarkContent: noteText.value || '',
    statDate: new Date(),
    createBy: 'dashboard',
  }), null)
  noteText.value = ''
  showNoteDialog.value = false
}

// ==================== API 数据加载 ====================
const loading = ref(false)
const lastRefresh = ref(new Date())

function safe(fn, fallback) {
  return fn().catch(err => {
    console.warn('[API]', err && err.message)
    return fallback
  })
}

function pad(n) { return String(n).padStart(2, '0') }
function formatYmd(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }
function formatHm(d) { return `${pad(d.getHours())}:${pad(d.getMinutes())}` }
function formatDateTime(d) { return `${formatYmd(d)} ${formatHm(d)}` }

async function loadCascade() {
  const [overview, trend, monthly, list] = await Promise.all([
    safe(() => api.cascadeOverview(), null),
    safe(() => api.cascadeTrend(7), []),
    safe(() => api.cascadeMonthlyTrend(12), []),
    safe(() => api.cascadeList({ days: 20 }), []),
  ])

  if (overview) {
    cascadeOverviewData.value = {
      total: Number(overview.todayTotal || 0),
      historyTotal: Number(overview.historyTotal || 0),
      todayChange: Number(overview.changeCount || 0),
      todayChangePercent: Number(overview.changePercent || 0),
    }
    const byType = overview.byType || {}
    const total = ['TABLE', 'API', 'FILE'].reduce((s, k) => s + Number(byType[k] || 0), 0) || 1
    const make = (key, color) => {
      const count = Number(byType[key] || 0)
      const pct = (count / total) * 100
      return {
        name: TYPE_LABEL[key] || key,
        value: Number(pct.toFixed(1)),
        color,
        count,
        yesterdayChange: Number(byType[key + 'Change'] || 0),
        changePercent: Number(byType[key + 'ChangePct'] || 0),
      }
    }
    distributionData.value = [make('TABLE', '#22D3EE'), make('API', '#34D399'), make('FILE', '#FBBF24')]
  }

  if (Array.isArray(trend) && trend.length > 0) {
    cascadeWeeklyTrendData.value = trend.map(d => ({
      day: String(d.statDate || '').slice(5, 10),
      table: Number(d.table || 0),
      api: Number(d.api || 0),
      file: Number(d.file || 0),
    }))
  }

  if (Array.isArray(monthly) && monthly.length > 0) {
    monthlyData.value = monthly.map(d => ({ month: d.month, value: Number(d.value || 0) }))
  }

  if (Array.isArray(list) && list.length > 0) {
    console.log('[cascade] list:', JSON.stringify(list))
    cascadeDetails.value = list.map(row => {
      const freq = (row.frequency === 'daily' ? '每天' : row.frequency) || '-'
      console.log('[cascade] frequency:', row.frequency, '->', freq)
      return {
        taskCode: row.taskCode,
        name: row.taskName || row.taskCode,
        status: row.cascadeStatus === 'SUCCESS' ? '成功' : row.cascadeStatus === 'FAIL' ? '失败' : '部分成功',
        type: TYPE_LABEL[row.taskType] || row.taskType,
        frequency: freq,
        timeRule: row.scheduleRule || '-',
        totalHistory: Number(row.totalCount || 0),
        lastAmount: Number(row.last7DaysTotal || 0),
        ratio: Number(row.totalCount) > 0
          ? Number(((Number(row.last7DaysTotal || 0) / Number(row.totalCount)) * 100).toFixed(1))
          : 0,
        time: row.startTime ? formatDateTime(new Date(row.startTime)) : '-',
      }
    })
  }
}

async function loadWorkorderAreas() {
  const list = await safe(() => api.workorderAreas(), [])
  if (Array.isArray(list) && list.length > 0) {
    districtOptions.value = list.map(item => {
      const code = String(item.areaCode || '')
      const name = String(item.areaName || '')
      return {
        value: code.toLowerCase() === 'city' ? 'all' : code,
        label: code.toLowerCase() === 'city' ? '全部区域' : (name || code),
      }
    })
    if (!districtOptions.value.some(o => o.value === 'all')) {
      districtOptions.value.unshift({ value: 'all', label: '全部区域' })
    }
  }
}

function formatMonthDay(date) {
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function formatWeekRange(weekStart) {
  const start = new Date(weekStart)
  if (Number.isNaN(start.getTime())) return String(weekStart || '-')
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  return `${formatMonthDay(start)}-${formatMonthDay(end)}`
}

async function loadWorkorder() {
  const districtParam = selectedDistrict.value && selectedDistrict.value !== 'all'
    ? selectedDistrict.value : ''

  const [today, districts, trendDaily, trendWeekly] = await Promise.all([
    safe(() => api.workorderToday(districtParam), null),
    safe(() => api.workorderDistrict(), []),
    safe(() => api.workorderTrend(districtParam, 14), []),
    safe(() => api.workorderTrendWeekly(districtParam, 8), []),
  ])

  if (today) {
    todayWorkorderData.value = {
      total: Number(today.total || 0),
      historyTotal: Number(today.historyTotal || 0),
      change: Number(today.changeCount || 0),
      changePercent: Number(today.changePercent || 0),
      src12345: Number(today.src12345 || 0),
      srcZjb: Number(today.srcZjb || 0),
      labelFillRate: Number(today.labelFillRate || 0),
      labelCodeRate: Number(today.labelCodeRate || 0),
      gridInRate: Number(today.gridInRate || 0),
      focusCount: Number(today.focusCount || 0),
      cityRatio: Number(today.cityRatio || 0),
      typeDistribution: today.typeDistribution || {},
    }
  }

  if (Array.isArray(districts) && districts.length > 0) {
    districtRows.value = districts.map(d => ({
      district: d.areaName || d.areaCode,
      areaCode: d.areaCode,
      total: Number(d.totalCount || 0),
      hotline: Number(d.src12345Count || 0),
      cityMgmt: Number(d.srcZjbCount || 0),
      standardRate: Number(Number(d.gridInRate || 0).toFixed(2)),
      tagFillRate: Number(Number(d.labelFillRate || 0).toFixed(2)),
      tagCodeRate: Number(Number(d.labelCodeRate || 0).toFixed(2)),
      focusCount: Number(d.focusCount || 0),
      collected: d.collectStatus === 'DONE',
      collected_update: d.updateStatus === 'UPDATED',
      collectionTime: d.collectTime ? formatYmd(new Date(d.collectTime)) : '-',
    }))
  }

  if (Array.isArray(trendDaily) && trendDaily.length > 0) {
    dailyTrendData.value = trendDaily.map(d => ({
      day: String(d.statDate || '').slice(5, 10),
      增量: Number(d.newCount || 0),
      总量: Number(d.totalCount || 0),
    }))
  }
  if (Array.isArray(trendWeekly) && trendWeekly.length > 0) {
    weeklyTrendData.value = trendWeekly.map(d => ({
      week: formatWeekRange(d.weekStart),
      增量: Number(d.newCount || 0),
      总量: Number(d.totalCount || 0),
    }))
  }
}

async function reloadAll() {
  loading.value = true
  try {
    if (activeTab.value === 'cascade') {
      await loadCascade()
    } else {
      await loadWorkorder()
    }
    lastRefresh.value = new Date()
  } finally {
    loading.value = false
  }
}

function reload() { reloadAll() }

// 1920x1080 大屏按视口等比缩放
const scale = ref(1)
function updateScale() {
  const sx = window.innerWidth / 1920
  const sy = window.innerHeight / 1080
  scale.value = Math.min(sx, sy)
}
onMounted(() => {
  updateScale()
  window.addEventListener('resize', updateScale)
  document.addEventListener('click', closeDateRangePicker)
  // 首屏先渲染静态框架，再异步加载当前 tab 数据
  deferLoad(() => {
    loadWorkorderAreas()
    if (activeTab.value === 'cascade') {
      loadCascade()
    } else {
      loadWorkorder()
    }
  })
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateScale)
  document.removeEventListener('click', closeDateRangePicker)
})
</script>

<template>
  <div class="w-screen h-screen overflow-hidden bg-[#0B1220] flex items-center justify-center">
    <div
      class="w-[1920px] h-[1080px] overflow-hidden flex flex-col origin-center flex-shrink-0"
      :style="{
        transform: `scale(${scale})`,
        fontFamily: 'Noto Sans SC',
        background: 'radial-gradient(ellipse at top, #122036 0%, #0B1220 60%)'
      }"
    >
    <!-- 顶部总览区 -->
    <div
      class="h-[88px] px-8 flex items-center justify-center flex-shrink-0 relative"
      style="background: linear-gradient(180deg, rgba(34,211,238,0.04) 0%, rgba(11,18,32,0) 100%); border-bottom: 1px solid rgba(148,163,184,0.1)"
    >
      <div class="text-center">
        <h1
          class="text-white"
          style="font-size: 30px; font-weight: 600; line-height: 1.2; letter-spacing: 8px"
        >
          数智网格数据运营监控
        </h1>
        <div class="flex items-center justify-center gap-2 mt-2">
          <div class="w-10 h-px" style="background: linear-gradient(90deg, transparent, #22D3EE)"></div>
          <div class="w-1 h-1 rounded-full bg-[#22D3EE]"></div>
          <div class="w-10 h-px" style="background: linear-gradient(90deg, #22D3EE, transparent)"></div>
        </div>
      </div>

      <div class="absolute left-8 flex items-center gap-1 rounded-full p-1" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(148,163,184,0.12)">
        <button
          class="px-5 py-1.5 rounded-full text-[13px] font-medium transition-all"
          :class="activeTab === 'cascade'
            ? 'text-white'
            : 'text-[#94A3B8] hover:text-[#CBD5E1]'"
          :style="activeTab === 'cascade' ? 'background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%); box-shadow: 0 4px 16px rgba(34,211,238,0.25)' : ''"
          @click="activeTab = 'cascade'"
        >数据级联监控</button>
        <button
          class="px-5 py-1.5 rounded-full text-[13px] font-medium transition-all"
          :class="activeTab === 'workOrder'
            ? 'text-white'
            : 'text-[#94A3B8] hover:text-[#CBD5E1]'"
          :style="activeTab === 'workOrder' ? 'background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%); box-shadow: 0 4px 16px rgba(34,211,238,0.25)' : ''"
          @click="activeTab = 'workOrder'"
        >工单汇集分析</button>
      </div>

      <div class="absolute right-8 flex items-center gap-3">
        <div class="flex items-center gap-2 px-3 py-1.5 rounded-full" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(148,163,184,0.1)">
          <div class="w-1.5 h-1.5 rounded-full bg-[#34D399] animate-pulse"></div>
          <p class="text-[12px]" style="color: #94A3B8">{{ currentTime }}</p>
        </div>
        <UiButton size="icon" class="bg-transparent hover:bg-white/5 rounded-full" @click="reload">
          <RefreshCw :size="18" class="text-[#94A3B8]" />
        </UiButton>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="flex flex-1 overflow-hidden gap-5 px-8 py-5">
      <!-- 左侧：数据级联监控 -->
      <div
        v-if="activeTab === 'cascade'"
        class="flex-1 rounded-2xl p-6 overflow-hidden flex flex-col"
        style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #0F1A2E; border: 1px solid rgba(148,163,184,0.08); box-shadow: 0 1px 0 rgba(255,255,255,0.03) inset, 0 12px 32px rgba(0,0,0,0.3)"
      >
        <div class="mb-5 flex items-baseline gap-3">
          <h2 class="text-white text-[22px] font-semibold tracking-wide">数据级联监控</h2>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="rounded-xl p-4 relative overflow-hidden h-[200px] flex flex-col justify-between" style="background: linear-gradient(135deg, rgba(34,211,238,0.12) 0%, rgba(34,211,238,0.02) 100%); border: 1px solid rgba(34,211,238,0.2)">
            <div class="absolute top-0 left-0 w-full h-px" style="background: linear-gradient(90deg, transparent, #22D3EE, transparent)"></div>
            <p class="text-[12px] tracking-wide" style="color: #94A3B8">今日级联总量</p>
            <div>
              <p class="text-white font-mono-tabular" style="font-size: 44px; font-weight: 600; line-height: 1; letter-spacing: -0.02em">{{ cascadeOverviewData.total.toLocaleString() }}</p>
            </div>
            <div class="flex items-center flex-wrap gap-1.5">
              <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" :style="Number(cascadeOverviewData.todayChange) >= 0 ? 'background: rgba(52,211,153,0.12); color: #34D399' : 'background: rgba(251,146,60,0.12); color: #FB923C'">
                <ArrowUp v-if="Number(cascadeOverviewData.todayChange) >= 0" :size="14" />
                <ArrowDown v-else :size="14" />
                <span class="text-[15px] font-mono-tabular font-semibold">{{ Number(cascadeOverviewData.todayChange) >= 0 ? '+' : '' }}{{ Number(cascadeOverviewData.todayChange).toLocaleString() }}</span>
              </div>
              <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" :style="Number(cascadeOverviewData.todayChangePercent) >= 0 ? 'background: rgba(52,211,153,0.12); color: #34D399' : 'background: rgba(251,146,60,0.12); color: #FB923C'">
                <ArrowUp v-if="Number(cascadeOverviewData.todayChangePercent) >= 0" :size="14" />
                <ArrowDown v-else :size="14" />
                <span class="text-[15px] font-mono-tabular font-semibold">{{ Number(cascadeOverviewData.todayChangePercent) >= 0 ? '+' : '' }}{{ Number(cascadeOverviewData.todayChangePercent).toFixed(1) }}%</span>
              </div>
              <span class="text-[15px] ml-1" style="color: #64748B">较昨日</span>
            </div>
          </div>

          <div class="rounded-xl p-4 relative overflow-hidden h-[200px] flex flex-col justify-between" style="background: linear-gradient(135deg, rgba(103,232,249,0.08) 0%, rgba(103,232,249,0.02) 100%); border: 1px solid rgba(103,232,249,0.15)">
            <div class="absolute top-0 left-0 w-full h-px" style="background: linear-gradient(90deg, transparent, #67E8F9, transparent)"></div>
            <p class="text-[12px] tracking-wide" style="color: #94A3B8">历史级联总量</p>
            <div>
              <p class="text-white font-mono-tabular" style="font-size: 44px; font-weight: 600; line-height: 1; letter-spacing: -0.02em">{{ cascadeOverviewData.historyTotal.toLocaleString() }}</p>
            </div>
            <div class="text-[12px]" style="color: #64748B">累计</div>
          </div>
          </div>

          <SectionCard title="级联类型分布" class="!h-[200px] !p-4">
            <div class="flex gap-5 h-full items-center">
              <div class="flex items-center justify-center">
                <EChart :option="distributionPieOption" width="150px" height="150px" />
              </div>
              <div class="flex-1 grid grid-cols-3 gap-3">
                <div
                  v-for="(item, idx) in distributionData"
                  :key="idx"
                  class="rounded-lg p-3 relative overflow-hidden"
                  style="background: rgba(255,255,255,0.02); border: 1px solid rgba(148,163,184,0.08)"
                >
                  <div class="absolute top-0 left-0 h-full w-[2px]" :style="{ background: item.color }"></div>
                  <p class="text-[13px] tracking-wide" style="color: #94A3B8">{{ item.name }}</p>
                  <p class="text-white font-mono-tabular my-1.5" style="font-size: 26px; font-weight: 600; letter-spacing: -0.02em">
                    {{ item.count.toLocaleString() }}
                  </p>
                  <div class="flex items-center gap-1.5">
                    <template v-if="Number(item.changePercent) >= 0">
                      <ArrowUp :size="13" class="text-[#34D399]" />
                      <span class="text-[#34D399] text-[13px] font-mono-tabular font-semibold">+{{ Number(item.changePercent).toFixed(2) }}%</span>
                    </template>
                    <template v-else>
                      <ArrowDown :size="13" class="text-[#FB923C]" />
                      <span class="text-[#FB923C] text-[13px] font-mono-tabular font-semibold">{{ Number(item.changePercent).toFixed(2) }}%</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </SectionCard>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div
            class="rounded-xl p-4 h-[280px] flex flex-col"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center gap-2 mb-3">
              <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
              <h3 class="text-[15px] font-medium tracking-wide" style="color: #CBD5E1">近7日变化趋势</h3>
            </div>
            <div class="flex items-center gap-4 mb-1">
              <div class="flex items-center gap-1.5">
                <div class="w-3 h-[2px] bg-[#22D3EE]"></div>
                <span class="text-[13px]" style="color: #94A3B8">库表级联</span>
              </div>
              <div class="flex items-center gap-1.5">
                <div class="w-3 h-[2px] bg-[#34D399]"></div>
                <span class="text-[13px]" style="color: #94A3B8">接口级联</span>
              </div>
              <div class="flex items-center gap-1.5">
                <div class="w-3 h-[2px] bg-[#FBBF24]"></div>
                <span class="text-[13px]" style="color: #94A3B8">文件级联</span>
              </div>
            </div>
            <EChart :option="cascadeTrendOption" height="210px" />
          </div>

          <div
            class="rounded-xl p-4 h-[280px] flex flex-col"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center gap-2 mb-3">
              <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
              <h3 class="text-[15px] font-medium tracking-wide" style="color: #CBD5E1">月度级联趋势</h3>
            </div>
            <EChart :option="monthlyTrendOption" height="230px" />
          </div>
        </div>

        <div class="flex-1 min-h-0 flex flex-col">
          <div
            class="rounded-xl p-4 flex-1 min-h-0 flex flex-col"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
                <h3 class="text-[13px] font-medium tracking-wide" style="color: #CBD5E1">级联详情列表</h3>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-[150px]">
                  <UiSelect v-model="cascadeTypeFilter" :options="cascadeTypeOptions" />
                </div>
                <div class="relative">
                  <SearchIcon :size="14" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-[#64748B]" />
                  <UiInput
                    v-model="cascadeSearchTerm"
                    placeholder="搜索表名/接口名"
                    class="!pl-8 !w-[200px] !h-8 !text-xs"
                  />
                </div>
              </div>
            </div>

            <UiScrollArea class="flex-1 min-h-0">
              <table class="w-full text-[12px]">
                <thead>
                  <tr style="border-bottom: 1px solid rgba(148,163,184,0.1)">
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">表名/接口名</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">类型</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">状态</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">频次</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">时间规则</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">历史总量</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">最近级联量</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">占比</th>
                    <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">最近级联时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in filteredCascade"
                    :key="index"
                    class="hover:bg-[#22D3EE]/[0.04] cursor-pointer transition-colors"
                    style="border-bottom: 1px solid rgba(148,163,184,0.06)"
                    @click="openHistory(row)"
                  >
                    <td class="p-2 font-medium" style="color: #E2E8F0">{{ row.name }}</td>
                    <td class="p-2">
                      <UiBadge :color="row.type === '库表级联' ? '#22D3EE' : row.type === '接口级联' ? '#34D399' : '#FBBF24'">
                        {{ row.type }}
                      </UiBadge>
                    </td>
                    <td class="p-2">
                      <div class="flex items-center gap-1.5">
                        <div class="w-1.5 h-1.5 rounded-full" :class="row.status === '成功' ? 'bg-[#34D399]' : 'bg-[#F87171]'"></div>
                        <span style="color: #CBD5E1">{{ row.status }}</span>
                      </div>
                    </td>
                    <td class="p-2" style="color: #94A3B8">{{ row.frequency }}</td>
                    <td class="p-2" style="color: #94A3B8">{{ row.timeRule }}</td>
                    <td class="p-2 font-mono-tabular" style="color: #E2E8F0">{{ row.totalHistory.toLocaleString() }}</td>
                    <td class="p-2 font-mono-tabular" style="color: #E2E8F0">{{ row.lastAmount.toLocaleString() }}</td>
                    <td class="p-2 font-mono-tabular" style="color: #22D3EE">{{ row.ratio }}%</td>
                    <td class="p-2 text-[11px]" style="color: #94A3B8">{{ row.time }}</td>
                  </tr>
                </tbody>
              </table>
            </UiScrollArea>
          </div>
        </div>
      </div>

      <!-- 右侧：工单汇集分析 -->
      <div
        v-if="activeTab === 'workOrder'"
        class="flex-1 rounded-2xl p-6 overflow-hidden flex flex-col"
        style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #0F1A2E; border: 1px solid rgba(148,163,184,0.08); box-shadow: 0 1px 0 rgba(255,255,255,0.03) inset, 0 12px 32px rgba(0,0,0,0.3)"
      >
        <div class="mb-5 flex items-center justify-between">
          <div class="flex items-baseline gap-3">
            <h2 class="text-white text-[22px] font-semibold tracking-wide">工单汇集分析</h2>
          </div>
          <div class="flex gap-3 items-center">
            <div class="w-[160px]">
              <UiSelect v-model="selectedDistrict" :options="districtOptions" />
            </div>
            <div class="flex items-center gap-2 px-3 py-1.5 rounded-lg" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(148,163,184,0.08)">
              <span class="text-[11px] tracking-wide" style="color: #94A3B8">归集时间</span>
              <div class="relative" data-date-range-picker>
                <button
                  class="flex items-center gap-2 pl-2 pr-3 h-7 rounded-md text-[11px] font-mono-tabular transition-colors"
                  style="background: rgba(255,255,255,0.03); border: 1px solid rgba(148,163,184,0.12); color: #CBD5E1"
                  @click.stop="toggleDateRangePicker"
                >
                  <CalendarIcon :size="12" class="text-[#64748B]" />
                  <span>{{ dateRangeLabel }}</span>
                </button>
                <div
                  v-if="showDateRangePicker"
                  class="absolute right-0 mt-2 p-4 rounded-lg z-50"
                  style="background: #131E32; border: 1px solid rgba(148,163,184,0.15); box-shadow: 0 12px 32px rgba(0,0,0,0.4); width: 540px"
                  @click.stop
                >
                  <div class="flex items-center justify-between mb-3 px-1">
                    <button class="px-2 py-0.5 rounded text-[12px] hover:bg-white/5 transition-colors" style="color: #94A3B8" @click="shiftLeftMonth(-1)">‹</button>
                    <div class="flex-1 flex justify-around">
                      <span class="text-[13px] font-medium" style="color: #CBD5E1">{{ leftMonthLabel }}</span>
                      <span class="text-[13px] font-medium" style="color: #CBD5E1">{{ rightMonthLabel }}</span>
                    </div>
                    <button class="px-2 py-0.5 rounded text-[12px] hover:bg-white/5 transition-colors" style="color: #94A3B8" @click="shiftLeftMonth(1)">›</button>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <div class="grid grid-cols-7 gap-1 mb-1">
                        <div v-for="w in weekdayLabels" :key="w" class="text-center text-[10px] py-1" style="color: #64748B">{{ w }}</div>
                      </div>
                      <div class="grid grid-cols-7 gap-1">
                        <div
                          v-for="(d, i) in leftDays"
                          :key="i"
                          class="text-center text-[12px] py-1 rounded cursor-pointer font-mono-tabular transition-colors"
                          :class="d ? 'hover:bg-[#22D3EE]/10' : ''"
                          :style="!d ? '' : (
                            isSameDay(d, parseDate(startDate)) || isSameDay(d, parseDate(endDate))
                              ? 'background: #22D3EE; color: #0B1220; font-weight: 600'
                              : (isInRange(d) || isInHoverRange(d))
                                ? 'background: rgba(34,211,238,0.18); color: #E2E8F0'
                                : 'color: #CBD5E1'
                          )"
                          @click="pickDate(d)"
                          @mouseenter="hoverDate = d"
                        >{{ d ? d.getDate() : '' }}</div>
                      </div>
                    </div>
                    <div>
                      <div class="grid grid-cols-7 gap-1 mb-1">
                        <div v-for="w in weekdayLabels" :key="w" class="text-center text-[10px] py-1" style="color: #64748B">{{ w }}</div>
                      </div>
                      <div class="grid grid-cols-7 gap-1">
                        <div
                          v-for="(d, i) in rightDays"
                          :key="i"
                          class="text-center text-[12px] py-1 rounded cursor-pointer font-mono-tabular transition-colors"
                          :class="d ? 'hover:bg-[#22D3EE]/10' : ''"
                          :style="!d ? '' : (
                            isSameDay(d, parseDate(startDate)) || isSameDay(d, parseDate(endDate))
                              ? 'background: #22D3EE; color: #0B1220; font-weight: 600'
                              : (isInRange(d) || isInHoverRange(d))
                                ? 'background: rgba(34,211,238,0.18); color: #E2E8F0'
                                : 'color: #CBD5E1'
                          )"
                          @click="pickDate(d)"
                          @mouseenter="hoverDate = d"
                        >{{ d ? d.getDate() : '' }}</div>
                      </div>
                    </div>
                  </div>
                  <div class="flex items-center justify-between mt-3 pt-3" style="border-top: 1px solid rgba(148,163,184,0.1)">
                    <span class="text-[11px] font-mono-tabular" style="color: #94A3B8">{{ startDate }} → {{ endDate }}</span>
                    <button
                      class="px-3 py-1 rounded text-[11px] font-medium text-white"
                      style="background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)"
                      @click="showDateRangePicker = false; rangeAnchor = null"
                    >确定</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="grid gap-4 mb-4" style="grid-template-columns: 1fr 1fr 2fr 2fr">
          <div class="rounded-xl p-4 relative overflow-hidden h-[200px] flex flex-col justify-between" style="background: linear-gradient(135deg, rgba(34,211,238,0.12) 0%, rgba(34,211,238,0.02) 100%); border: 1px solid rgba(34,211,238,0.2)">
            <div class="absolute top-0 left-0 w-full h-px" style="background: linear-gradient(90deg, transparent, #22D3EE, transparent)"></div>
            <p class="text-[13px] tracking-wide" style="color: #CBD5E1">今日汇集工单</p>
            <div>
              <p class="text-white font-mono-tabular" style="font-size: 44px; font-weight: 600; line-height: 1; letter-spacing: -0.02em">
                {{ todayWorkorderData.total.toLocaleString() }}
              </p>
              <p v-if="selectedDistrict !== 'all'" class="text-[11px] mt-1" style="color: #22D3EE">占全市 {{ Number(todayWorkorderData.cityRatio || 0).toFixed(1) }}%</p>
            </div>
            <div class="flex items-center flex-wrap gap-1.5">
              <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" :style="`background: ${todayWorkorderData.change >= 0 ? 'rgba(52,211,153,0.12); color: #34D399' : 'rgba(251,146,60,0.12); color: #FB923C'}`">
                <component :is="todayWorkorderData.change >= 0 ? ArrowUp : ArrowDown" :size="14" />
                <span class="text-[15px] font-mono-tabular font-semibold">{{ (todayWorkorderData.change >= 0 ? '+' : '') + todayWorkorderData.change.toLocaleString() }}</span>
              </div>
              <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" :style="`background: ${todayWorkorderData.changePercent >= 0 ? 'rgba(52,211,153,0.12); color: #34D399' : 'rgba(251,146,60,0.12); color: #FB923C'}`">
                <component :is="todayWorkorderData.changePercent >= 0 ? ArrowUp : ArrowDown" :size="14" />
                <span class="text-[15px] font-mono-tabular font-semibold">{{ (todayWorkorderData.changePercent >= 0 ? '+' : '') + Number(todayWorkorderData.changePercent).toFixed(1) }}%</span>
              </div>
              <span class="text-[15px] ml-1" style="color: #64748B">较昨日</span>
            </div>
          </div>

          <div class="rounded-xl p-4 relative overflow-hidden h-[200px] flex flex-col justify-between" style="background: linear-gradient(135deg, rgba(103,232,249,0.08) 0%, rgba(103,232,249,0.02) 100%); border: 1px solid rgba(103,232,249,0.15)">
            <div class="absolute top-0 left-0 w-full h-px" style="background: linear-gradient(90deg, transparent, #67E8F9, transparent)"></div>
            <p class="text-[13px] tracking-wide" style="color: #CBD5E1">历史汇集总量</p>
            <div>
              <p class="text-white font-mono-tabular" style="font-size: 44px; font-weight: 600; line-height: 1; letter-spacing: -0.02em">
                {{ todayWorkorderData.historyTotal.toLocaleString() }}
              </p>
              <p v-if="selectedDistrict !== 'all'" class="text-[11px] mt-1" style="color: #67E8F9">占全市 {{ Number(todayWorkorderData.cityRatio || 0).toFixed(1) }}%</p>
            </div>
            <div class="text-[12px]" style="color: #64748B">累计</div>
          </div>

          <div
            class="rounded-xl p-4 h-[200px] flex flex-col"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center gap-2 mb-2">
              <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
              <h3 class="text-[13px] font-medium tracking-wide" style="color: #CBD5E1">工单类型分布</h3>
            </div>
            <div class="flex items-center gap-4 flex-1 min-h-0">
              <EChart :option="workOrderTypePieOption" width="160px" height="150px" />
              <div class="flex-1 space-y-3">
                <div class="flex items-center justify-between text-[18px] pl-6 pr-2">
                  <div class="flex items-center gap-2.5">
                    <div class="w-3 h-3 rounded-sm bg-[#34D399]"></div>
                    <span style="color: #94A3B8">12345市民热线工单</span>
                  </div>
                  <span class="text-white font-mono-tabular font-semibold">{{ (todayWorkorderData.typeDistribution['12345'] || 0).toLocaleString() }}</span>
                </div>
                <div class="flex items-center justify-between text-[18px] pl-6 pr-2">
                  <div class="flex items-center gap-2.5">
                    <div class="w-3 h-3 rounded-sm bg-[#22D3EE]"></div>
                    <span style="color: #94A3B8">城市综合管理部事业工单</span>
                  </div>
                  <span class="text-white font-mono-tabular font-semibold">{{ (todayWorkorderData.typeDistribution['ZJB'] || 0).toLocaleString() }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 工单质量指标 -->
          <div class="grid grid-cols-2 gap-3 h-[200px]">
            <div class="rounded-xl p-3 relative overflow-hidden flex flex-col" style="background: linear-gradient(180deg, rgba(34,211,238,0.06) 0%, rgba(34,211,238,0.01) 100%); border: 1px solid rgba(34,211,238,0.12)">
              <div class="absolute top-0 left-0 h-full w-[2px] bg-[#22D3EE]"></div>
              <p class="text-[13px] tracking-wide" style="color: #CBD5E1">标签填报率</p>
              <div class="flex-1 flex items-center justify-center">
                <p class="text-white font-mono-tabular" style="font-size: 30px; font-weight: 600; line-height: 1.1; letter-spacing: -0.02em; text-shadow: 0 0 18px rgba(34,211,238,0.35)">
                  {{ qualityMetrics.tagFillRate }}<span class="text-[15px] font-normal ml-0.5" style="color: #94A3B8">%</span>
                </p>
              </div>
              <div class="flex items-center justify-between pt-2" style="border-top: 1px dashed rgba(148,163,184,0.14)">
                <div class="flex items-center gap-1" style="color: #34D399">
                  <ArrowUp :size="14" />
                  <span class="text-[14px] font-mono-tabular font-semibold">{{ qualityMetrics.tagFillTrend }}</span>
                </div>
                <span class="text-[11px]" style="color: #64748B">较昨日</span>
              </div>
            </div>

            <div class="rounded-xl p-3 relative overflow-hidden flex flex-col" style="background: linear-gradient(180deg, rgba(34,211,238,0.06) 0%, rgba(34,211,238,0.01) 100%); border: 1px solid rgba(34,211,238,0.12)">
              <div class="absolute top-0 left-0 h-full w-[2px] bg-[#22D3EE]"></div>
              <p class="text-[13px] tracking-wide" style="color: #CBD5E1">标签编码率</p>
              <div class="flex-1 flex items-center justify-center">
                <p class="text-white font-mono-tabular" style="font-size: 30px; font-weight: 600; line-height: 1.1; letter-spacing: -0.02em; text-shadow: 0 0 18px rgba(34,211,238,0.35)">
                  {{ qualityMetrics.tagCodeRate }}<span class="text-[15px] font-normal ml-0.5" style="color: #94A3B8">%</span>
                </p>
              </div>
              <div class="flex items-center justify-between pt-2" style="border-top: 1px dashed rgba(148,163,184,0.14)">
                <div class="flex items-center gap-1" style="color: #34D399">
                  <ArrowUp :size="14" />
                  <span class="text-[14px] font-mono-tabular font-semibold">{{ qualityMetrics.tagCodeTrend }}</span>
                </div>
                <span class="text-[11px]" style="color: #64748B">较昨日</span>
              </div>
            </div>

            <div class="rounded-xl p-3 relative overflow-hidden flex flex-col" style="background: linear-gradient(180deg, rgba(34,211,238,0.06) 0%, rgba(34,211,238,0.01) 100%); border: 1px solid rgba(34,211,238,0.12)">
              <div class="absolute top-0 left-0 h-full w-[2px] bg-[#22D3EE]"></div>
              <p class="text-[13px] tracking-wide" style="color: #CBD5E1">工单入格率</p>
              <div class="flex-1 flex items-center justify-center">
                <p class="text-white font-mono-tabular" style="font-size: 30px; font-weight: 600; line-height: 1.1; letter-spacing: -0.02em; text-shadow: 0 0 18px rgba(34,211,238,0.35)">
                  {{ qualityMetrics.orderStandardRate }}<span class="text-[15px] font-normal ml-0.5" style="color: #94A3B8">%</span>
                </p>
              </div>
              <div class="flex items-center justify-between pt-2" style="border-top: 1px dashed rgba(148,163,184,0.14)">
                <div class="flex items-center gap-1" style="color: #FB923C">
                  <ArrowDown :size="14" />
                  <span class="text-[14px] font-mono-tabular font-semibold">{{ qualityMetrics.orderStandardTrend }}</span>
                </div>
                <span class="text-[11px]" style="color: #64748B">较昨日</span>
              </div>
            </div>

            <div class="rounded-xl p-3 relative overflow-hidden flex flex-col" style="background: linear-gradient(180deg, rgba(34,211,238,0.06) 0%, rgba(34,211,238,0.01) 100%); border: 1px solid rgba(34,211,238,0.12)">
              <div class="absolute top-0 left-0 h-full w-[2px] bg-[#22D3EE]"></div>
              <p class="text-[13px] tracking-wide" style="color: #CBD5E1">重点关注数</p>
              <div class="flex-1 flex items-center justify-center">
                <p class="text-white font-mono-tabular" style="font-size: 30px; font-weight: 600; line-height: 1.1; letter-spacing: -0.02em; text-shadow: 0 0 18px rgba(34,211,238,0.35)">
                  {{ qualityMetrics.focusCount.toLocaleString() }}
                </p>
              </div>
              <div class="flex items-center justify-between pt-2" style="border-top: 1px dashed rgba(148,163,184,0.14)">
                <div class="flex items-center gap-1" style="color: #34D399">
                  <ArrowUp :size="14" />
                  <span class="text-[14px] font-mono-tabular font-semibold">{{ qualityMetrics.focusTrend }}</span>
                </div>
                <span class="text-[11px]" style="color: #64748B">较昨日</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 各区工单统计 + 工单汇集趋势 各占一半 -->
        <div class="grid grid-cols-2 gap-4 mb-4">
          <div
            class="rounded-xl p-4 h-[300px] min-w-0 overflow-hidden"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <template v-if="selectedDistrict === 'all'">
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
                  <h3 class="text-[15px] font-medium tracking-wide" style="color: #CBD5E1">各区工单统计</h3>
                </div>
                <div class="flex gap-2">
                  <div class="w-[120px]">
                    <UiSelect v-model="workOrderType" :options="workOrderTypeOptions" size="sm" />
                  </div>
                  <div class="w-[140px]">
                    <UiSelect v-model="districtCollectionStatus" :options="collectionStatusOptions" size="sm" />
                  </div>
                </div>
              </div>
              <UiScrollArea height="220px">
                <table class="w-full text-[14px]" style="table-layout: fixed">
                  <colgroup>
                    <col style="width: 20%" />
                    <col style="width: 20%" />
                    <col style="width: 20%" />
                    <col style="width: 20%" />
                    <col style="width: 20%" />
                  </colgroup>
                  <thead>
                    <tr style="border-bottom: 1px solid rgba(148,163,184,0.1)">
                      <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B">区县名称</th>
                      <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B">归集状态</th>
                      <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B">工单数</th>
                      <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B">全市占比</th>
                      <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B">归集时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, idx) in districtTableRows" :key="idx" class="hover:bg-white/[0.02] transition-colors" style="border-bottom: 1px solid rgba(148,163,184,0.06)">
                      <td class="p-2" style="color: #E2E8F0">{{ row.district }}</td>
                      <td class="p-2">
                        <UiBadge :color="row.collected ? '#34D399' : '#F87171'">
                          {{ row.collected ? '已归集' : '未归集' }}
                        </UiBadge>
                      </td>
                      <td class="p-2 font-mono-tabular" style="color: #E2E8F0">{{ row.displayCount.toLocaleString() }}</td>
                      <td class="p-2 font-mono-tabular font-semibold" style="color: #22D3EE">{{ row.percentage }}%</td>
                      <td class="p-2 text-[13px]" style="color: #94A3B8">{{ row.collectionTime }}</td>
                    </tr>
                  </tbody>
                </table>
              </UiScrollArea>
            </template>
            <template v-else>
              <div class="flex items-center gap-2 mb-3">
                <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
                <h3 class="text-[13px] font-medium tracking-wide" style="color: #CBD5E1">本区工单详细统计</h3>
              </div>
              <div class="space-y-3">
                <div class="rounded-lg p-3 relative overflow-hidden" style="background: linear-gradient(135deg, rgba(103,232,249,0.08) 0%, rgba(103,232,249,0.02) 100%); border: 1px solid rgba(103,232,249,0.15)">
                  <div class="absolute top-0 left-0 h-full w-[2px] bg-[#67E8F9]"></div>
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-[11px]" style="color: #94A3B8">12345市民服务热线</span>
                    <span class="text-[10px] font-medium" style="color: #67E8F9">当前区划</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-white font-mono-tabular" style="font-size: 26px; font-weight: 600; letter-spacing: -0.02em">{{ (selectedDistrictRow?.hotline || 0).toLocaleString() }}</span>
                    <div class="text-right">
                      <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" style="background: rgba(52,211,153,0.12); color: #34D399">
                        <ArrowUp :size="14" />
                        <span class="text-[15px] font-mono-tabular font-semibold">+89 · +8.3%</span>
                      </div>
                      <p class="text-[15px] mt-0.5" style="color: #64748B">较昨日</p>
                    </div>
                  </div>
                </div>
                <div class="rounded-lg p-3 relative overflow-hidden" style="background: linear-gradient(135deg, rgba(34,211,238,0.08) 0%, rgba(34,211,238,0.02) 100%); border: 1px solid rgba(34,211,238,0.15)">
                  <div class="absolute top-0 left-0 h-full w-[2px] bg-[#22D3EE]"></div>
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-[11px]" style="color: #94A3B8">城市综合管理部事件</span>
                    <span class="text-[10px] font-medium" style="color: #22D3EE">当前区划</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-white font-mono-tabular" style="font-size: 26px; font-weight: 600; letter-spacing: -0.02em">{{ (selectedDistrictRow?.cityMgmt || 0).toLocaleString() }}</span>
                    <div class="text-right">
                      <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md" style="background: rgba(52,211,153,0.12); color: #34D399">
                        <ArrowUp :size="14" />
                        <span class="text-[15px] font-mono-tabular font-semibold">+98 · +7.9%</span>
                      </div>
                      <p class="text-[15px] mt-0.5" style="color: #64748B">较昨日</p>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <div
            class="rounded-xl p-4 h-[300px] min-w-0 overflow-hidden"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
                <h3 class="text-[15px] font-medium tracking-wide" style="color: #CBD5E1">工单汇集趋势</h3>
              </div>
              <div class="flex gap-1 p-0.5 rounded-md" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(148,163,184,0.08)">
                <button
                  class="px-3 py-1 rounded text-[13px] font-medium transition-all"
                  :class="trendViewType === 'daily' ? 'text-white' : 'text-[#94A3B8] hover:text-[#CBD5E1]'"
                  :style="trendViewType === 'daily' ? 'background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)' : ''"
                  @click="trendViewType = 'daily'"
                >按日</button>
                <button
                  class="px-3 py-1 rounded text-[13px] font-medium transition-all"
                  :class="trendViewType === 'weekly' ? 'text-white' : 'text-[#94A3B8] hover:text-[#CBD5E1]'"
                  :style="trendViewType === 'weekly' ? 'background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)' : ''"
                  @click="trendViewType = 'weekly'"
                >按周</button>
              </div>
            </div>
            <div class="flex items-center justify-end gap-4 mb-1">
              <div class="flex items-center gap-1.5">
                <div class="w-3 h-[2px] bg-[#22D3EE]"></div>
                <span class="text-[13px]" style="color: #94A3B8">总量</span>
              </div>
              <div class="flex items-center gap-1.5">
                <div class="w-3 h-0" style="border-top: 1.5px dashed #67E8F9"></div>
                <span class="text-[13px]" style="color: #94A3B8">增量</span>
              </div>
            </div>
            <div class="cursor-pointer" @click="openWorkOrderList()">
              <EChart :option="workOrderTrendOption" height="210px" />
            </div>
          </div>
        </div>

        <!-- 各区工单分析 单独一行 -->
        <div class="mb-4">
          <div
            class="rounded-xl p-4 h-[300px] flex flex-col"
            style="background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0) 100%), #131E32; border: 1px solid rgba(148,163,184,0.08)"
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-[3px] h-[14px] rounded-full" style="background: linear-gradient(180deg, #22D3EE 0%, rgba(34,211,238,0) 100%)"></div>
                <h3 class="text-[15px] font-medium tracking-wide" style="color: #CBD5E1">各区工单分析</h3>
              </div>
              <div class="flex gap-2">
                <div class="w-[120px]">
                  <UiSelect v-model="districtAnalysisTime" :options="analysisTimeOptions" />
                </div>
                <div class="w-[120px]">
                  <UiSelect v-model="districtUpdateStatus" :options="updateStatusOptions" />
                </div>
              </div>
            </div>
            <table class="w-full text-[14px]" style="border-collapse: separate; border-spacing: 0; table-layout: fixed">
              <colgroup>
                <col style="width: 80px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 90px" />
                <col style="width: 80px" />
              </colgroup>
              <thead>
                <tr>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">区划</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">汇集总量</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">12345市民热线工单</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">城市综合管理部事业工单</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">入格率</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">填报率</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">编码率</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">重点关注</th>
                  <th class="text-left p-2 font-medium text-[13px] tracking-wide" style="color: #64748B; border-bottom: 1px solid rgba(148,163,184,0.1)">更新状态</th>
                </tr>
              </thead>
            </table>
            <UiScrollArea class="flex-1 min-h-0">
                <table class="w-full text-[14px]" style="border-collapse: separate; border-spacing: 0; table-layout: fixed">
                  <colgroup>
                    <col style="width: 80px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 90px" />
                    <col style="width: 80px" />
                  </colgroup>
                  <tbody>
                    <tr v-for="(row, index) in districtAnalysisRows" :key="index" class="hover:bg-white/[0.02] transition-colors">
                      <td class="p-2 font-medium" style="color: #E2E8F0; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.district }}</td>
                      <td class="p-2 font-mono-tabular" style="color: #E2E8F0; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.total.toLocaleString() }}</td>
                      <td class="p-2 font-mono-tabular" style="color: #94A3B8; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.hotline.toLocaleString() }}</td>
                      <td class="p-2 font-mono-tabular" style="color: #94A3B8; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.cityMgmt.toLocaleString() }}</td>
                      <td class="p-2 font-mono-tabular" style="color: #22D3EE; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.standardRate }}%</td>
                      <td class="p-2 font-mono-tabular" style="color: #67E8F9; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.tagFillRate }}%</td>
                      <td class="p-2 font-mono-tabular" style="color: #67E8F9; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.tagCodeRate }}%</td>
                      <td class="p-2 font-mono-tabular" style="color: #FBBF24; border-bottom: 1px solid rgba(148,163,184,0.06)">{{ row.focusCount.toLocaleString() }}</td>
                      <td class="p-2" style="border-bottom: 1px solid rgba(148,163,184,0.06)">
                        <UiBadge :color="row.collected ? '#34D399' : '#64748B'">
                          {{ row.collected ? '已更新' : '未更新' }}
                        </UiBadge>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </UiScrollArea>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史级联记录弹窗 -->
    <UiDialog v-model="showHistoryDialog" max-width="600px">
      <h2 class="text-white text-[16px] font-semibold mb-1 tracking-wide">历史级联记录</h2>
      <p class="text-[12px] mb-4" style="color: #64748B">表名：dws_grid_cascade_log</p>
      <UiScrollArea height="300px">
        <table class="w-full text-[12px]">
          <thead>
            <tr style="border-bottom: 1px solid rgba(148,163,184,0.1)">
              <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">级联时间</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">级联量</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">状态</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide" style="color: #64748B">耗时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(rec, i) in historyRecords" :key="i" style="border-bottom: 1px solid rgba(148,163,184,0.06)">
              <td class="p-2" style="color: #94A3B8">{{ rec.time }}</td>
              <td class="p-2 font-mono-tabular" style="color: #E2E8F0">{{ rec.amount }}</td>
              <td class="p-2"><UiBadge :color="rec.status === 'SUCCESS' ? '#34D399' : '#F87171'">{{ rec.status === 'SUCCESS' ? '成功' : '失败' }}</UiBadge></td>
              <td class="p-2 font-mono-tabular" style="color: #94A3B8">{{ rec.duration }}</td>
            </tr>
          </tbody>
        </table>
      </UiScrollArea>
      <div class="flex justify-end mt-4">
        <UiButton class="text-white" style="background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)" @click="exportHistory">导出记录</UiButton>
      </div>
    </UiDialog>

    <!-- 当日工单清单弹窗 -->
    <UiDialog v-model="showWorkOrderDialog" max-width="800px">
      <h2 class="text-white text-[16px] font-semibold mb-1 tracking-wide">2024-05-18 工单清单</h2>
      <p class="text-[12px] mb-4" style="color: #64748B">浦东新区 · 1,245 条</p>
      <div class="mb-4 relative">
        <SearchIcon :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B]" />
        <UiInput v-model="searchTerm" placeholder="搜索工单编号" class="!pl-9" />
      </div>
      <UiScrollArea height="320px" class="mb-4">
        <table class="w-full text-[12px]">
          <thead>
            <tr style="border-bottom: 1px solid rgba(148,163,184,0.1)">
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[105px]" style="color: #64748B">工单编号</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[85px]" style="color: #64748B">类型</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[65px]" style="color: #64748B">形式</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[75px]" style="color: #64748B">归集范围</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[85px]" style="color: #64748B">归集工单数</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[85px]" style="color: #64748B">归集进程数</th>
              <th class="text-left p-2 font-medium text-[11px] tracking-wide min-w-[135px]" style="color: #64748B">归集时间</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="w in filteredWorkOrders"
              :key="w.id"
              class="hover:bg-[#22D3EE]/[0.04] cursor-pointer transition-colors"
              :class="selectedWorkOrder && selectedWorkOrder.id === w.id ? 'bg-[#22D3EE]/[0.08]' : ''"
              style="border-bottom: 1px solid rgba(148,163,184,0.06)"
              @click="selectWorkOrder(w)"
            >
              <td class="p-2 whitespace-nowrap font-mono-tabular" style="color: #E2E8F0">{{ w.id }}</td>
              <td class="p-2">
                <UiBadge :color="w.type === '12345热线' ? '#67E8F9' : '#22D3EE'">{{ w.type }}</UiBadge>
              </td>
              <td class="p-2 whitespace-nowrap" style="color: #94A3B8">{{ w.form }}</td>
              <td class="p-2 whitespace-nowrap" style="color: #94A3B8">{{ w.scope }}</td>
              <td class="p-2 font-mono-tabular text-center" style="color: #E2E8F0">{{ w.orderCount }}</td>
              <td class="p-2 font-mono-tabular text-center" style="color: #E2E8F0">{{ w.processCount }}</td>
              <td class="p-2 whitespace-nowrap text-[11px]" style="color: #94A3B8">{{ w.time }}</td>
            </tr>
          </tbody>
        </table>
      </UiScrollArea>
      <div class="flex justify-end">
        <UiButton
          class="text-white"
          style="background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)"
          :disabled="!selectedWorkOrder"
          @click="openNoteDialog"
        >添加备注</UiButton>
      </div>
    </UiDialog>

    <!-- 备注编辑弹窗 -->
    <UiDialog v-model="showNoteDialog" max-width="500px">
      <h2 class="text-white text-[16px] font-semibold mb-4 tracking-wide">添加工单备注</h2>
      <div v-if="selectedWorkOrder" class="rounded-lg p-4 mb-4" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(148,163,184,0.08)">
        <div class="grid grid-cols-2 gap-3 text-[12px]">
          <div>
            <span style="color: #64748B">工单编号</span>
            <p class="text-white font-mono-tabular font-semibold mt-0.5">{{ selectedWorkOrder.id }}</p>
          </div>
          <div>
            <span style="color: #64748B">类型</span>
            <p class="mt-1">
              <UiBadge :color="selectedWorkOrder.type === '12345热线' ? '#67E8F9' : '#22D3EE'">{{ selectedWorkOrder.type }}</UiBadge>
            </p>
          </div>
          <div>
            <span style="color: #64748B">形式</span>
            <p class="text-white mt-0.5">{{ selectedWorkOrder.form }}</p>
          </div>
          <div>
            <span style="color: #64748B">归集范围</span>
            <p class="text-white mt-0.5">{{ selectedWorkOrder.scope }}</p>
          </div>
          <div>
            <span style="color: #64748B">归集工单数</span>
            <p class="text-white font-mono-tabular mt-0.5">{{ selectedWorkOrder.orderCount }}</p>
          </div>
          <div>
            <span style="color: #64748B">归集进程数</span>
            <p class="text-white font-mono-tabular mt-0.5">{{ selectedWorkOrder.processCount }}</p>
          </div>
          <div class="col-span-2">
            <span style="color: #64748B">归集时间</span>
            <p class="text-white mt-0.5">{{ selectedWorkOrder.time }}</p>
          </div>
        </div>
      </div>

      <UiTextarea
        v-model="noteText"
        placeholder="请输入备注内容（最多200字）"
        :maxlength="200"
        class="!h-[120px]"
      />
      <div class="flex gap-3 justify-end mt-4">
        <UiButton class="text-white" style="background: rgba(148,163,184,0.15); border: 1px solid rgba(148,163,184,0.2)" @click="showNoteDialog = false">取消</UiButton>
        <UiButton class="text-white" style="background: linear-gradient(180deg, #22D3EE 0%, #0891B2 100%)" @click="saveRemark">确认保存</UiButton>
      </div>
    </UiDialog>
  </div>
</template>
