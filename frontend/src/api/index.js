// 统一 API 客户端，所有请求走相对路径，由 Spring Boot 的 context-path /DataGovernance 提供
const BASE = (import.meta.env && import.meta.env.VITE_API_BASE) || '/DataGovernance'

async function request(path, { method = 'GET', params, body, headers } = {}) {
  let url = BASE + path
  if (params) {
    const usp = new URLSearchParams()
    Object.keys(params).forEach(k => {
      const v = params[k]
      if (v !== undefined && v !== null && v !== '') usp.append(k, v)
    })
    const qs = usp.toString()
    if (qs) url += (url.includes('?') ? '&' : '?') + qs
  }

  const opts = {
    method,
    headers: {
      'Accept': 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(headers || {}),
    },
  }
  if (body) opts.body = typeof body === 'string' ? body : JSON.stringify(body)

  const resp = await fetch(url, opts)
  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status} ${resp.statusText}`)
  }
  const json = await resp.json()
  // 后端 BaseController.success() 通常返回 { code, msg, data }
  if (json && typeof json === 'object' && 'code' in json) {
    if (json.code !== 200 && json.code !== 0) {
      throw new Error(json.msg || `业务错误 ${json.code}`)
    }
    return json.data === undefined ? json : json.data
  }
  return json
}

export const api = {
  cascadeOverview: () => request('/grid/monitor/cascade/overview'),
  cascadeList: (params) => request('/grid/monitor/cascade/list', { params }),
  cascadeTrend: (days = 7) => request('/grid/monitor/cascade/trend', { params: { days } }),
  cascadeMonthlyTrend: (months = 12) => request('/grid/monitor/cascade/trend/monthly', { params: { months } }),
  cascadeHistory: (taskCode) => request('/grid/monitor/cascade/history', { params: { taskCode } }),
  addCascadeLog: (log) => request('/grid/monitor/cascade/log', { method: 'POST', body: log }),

  workorderCalendar: (month) => request('/grid/monitor/workorder/calendar', { params: { month } }),
  workorderAreas: () => request('/grid/monitor/workorder/areas'),
  workorderToday: (areaCode) => request('/grid/monitor/workorder/today', { params: { areaCode } }),
  workorderDistrict: (statDate) => request('/grid/monitor/workorder/district', { params: { statDate } }),
  workorderTrend: (areaCode, days = 7) => request('/grid/monitor/workorder/trend', { params: { areaCode, days } }),
  workorderTrendWeekly: (areaCode, weeks = 8) => request('/grid/monitor/workorder/trend/weekly', { params: { areaCode, weeks } }),
  workorderFocus: (statDate, srcType) => request('/grid/monitor/workorder/focus', { params: { statDate, srcType } }),
  workorderList: (params) => request('/grid/monitor/workorder/list', { params }),

  saveRemark: (remark) => request('/grid/monitor/remark', { method: 'POST', body: remark }),
  listRemark: (taskId) => request(`/grid/monitor/remark/${encodeURIComponent(taskId)}`),
}

export default api
