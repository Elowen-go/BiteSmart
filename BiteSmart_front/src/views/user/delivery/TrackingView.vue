<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getDeliveryTracking } from '../../../api/user/delivery'
import { AMAP_JS_KEY, AMAP_SCRIPT_URL } from '../../../config/amap'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const tracking = ref<any>(null)
let timer: ReturnType<typeof setInterval> | undefined

const statusMap: Record<number, string> = { 10: '待分配', 20: '待取餐', 30: '已取餐', 40: '配送中', 50: '已送达', 60: '配送异常', 70: '已取消' }
const vehicleMap: Record<number, string> = { 10: '电动车', 20: '自行车', 30: '汽车' }
const vehicleText = (type?: number) => (type && vehicleMap[type]) || '配送中'

/* ---------- 高德地图：key 为空时展示占位卡，不加载脚本 ---------- */
const hasMapKey = Boolean(AMAP_JS_KEY)
const mapFailed = ref(false)
const showMap = computed(() => hasMapKey && !mapFailed.value)
const mapEl = ref<HTMLElement | null>(null)
let map: any = null
let riderMarker: any = null
let pathLine: any = null
let hasFitView = false
let scriptLoading: Promise<any> | null = null

/** 骑手实时位置（GCJ-02，高德系坐标直接用）。兼容 riderLat/riderLng 与 currentLat/currentLng 两种字段 */
const riderPos = computed<[number, number] | null>(() => {
  const t = tracking.value
  if (!t) return null
  const lng = Number(t.riderLng ?? t.driver?.currentLng ?? t.currentLng)
  const lat = Number(t.riderLat ?? t.driver?.currentLat ?? t.currentLat)
  return Number.isFinite(lng) && Number.isFinite(lat) && lng !== 0 && lat !== 0 ? [lng, lat] : null
})

/** 轨迹点数组：兼容 JSON 字符串、[lng,lat] 数组、{lng,lat} 对象三种形态 */
const pathPoints = computed<[number, number][]>(() => {
  const raw = tracking.value?.path
  if (!raw) return []
  let arr: any[] = []
  if (typeof raw === 'string') {
    try { arr = JSON.parse(raw) } catch { return [] }
  } else if (Array.isArray(raw)) {
    arr = raw
  }
  return arr
    .map((p: any): [number, number] => {
      if (Array.isArray(p)) return [Number(p[0]), Number(p[1])]
      return [Number(p?.lng ?? p?.lon ?? p?.longitude), Number(p?.lat ?? p?.latitude)]
    })
    .filter(([lng, lat]) => Number.isFinite(lng) && Number.isFinite(lat))
})

const loadAmap = (): Promise<any> => {
  if ((window as any).AMap) return Promise.resolve((window as any).AMap)
  if (!scriptLoading) {
    scriptLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = AMAP_SCRIPT_URL
      script.onload = () => resolve((window as any).AMap)
      script.onerror = () => { scriptLoading = null; reject(new Error('amap script load failed')) }
      document.head.appendChild(script)
    })
  }
  return scriptLoading
}

const renderMap = async () => {
  if (!showMap.value) return
  await nextTick()
  if (!mapEl.value) return
  try {
    const AMap = await loadAmap()
    const center = riderPos.value || pathPoints.value[pathPoints.value.length - 1] || [116.397128, 39.916527]
    if (!map) {
      map = new AMap.Map(mapEl.value, { zoom: 14, center })
      riderMarker = new AMap.Marker({ position: center, title: '骑手当前位置' })
      map.add(riderMarker)
    }
    if (riderPos.value) riderMarker.setPosition(riderPos.value)
    if (pathPoints.value.length > 1) {
      if (!pathLine) {
        pathLine = new AMap.Polyline({ path: pathPoints.value, strokeColor: '#1E9E62', strokeWeight: 5, strokeOpacity: 0.85, showDir: true })
        map.add(pathLine)
      } else {
        pathLine.setPath(pathPoints.value)
      }
    }
    // 仅在首次拿到有效数据时自动定位，之后不打断用户手动缩放拖动
    if (!hasFitView && (riderPos.value || pathPoints.value.length > 1)) {
      if (pathLine) map.setFitView([pathLine])
      else map.setCenter(riderPos.value)
      hasFitView = true
    }
  } catch {
    mapFailed.value = true
  }
}

/* ---------- 占位卡时间线 ---------- */
const timelineItems = computed(() => {
  const t = tracking.value
  const s = Number(t?.taskStatus || 0)
  return [
    { label: '商家出餐，等待骑手取餐', time: '', done: s >= 30 },
    { label: '骑手已取餐', time: t?.pickupTime || '', done: s >= 30 },
    { label: '配送中，正向你赶来', time: t?.estimatedDeliveryTime ? `预计 ${t.estimatedDeliveryTime} 送达` : '', done: s >= 40 },
    { label: '已送达', time: t?.deliverTime || '', done: s >= 50 }
  ]
})

// orderId 是雪花 ID，必须字符串透传，Number() 强转会丢精度导致 404
const fetchTracking = async () => {
  const id = String(route.params.orderId || '')
  if (!id) return
  loading.value = !tracking.value
  try {
    const res = await getDeliveryTracking(id)
    tracking.value = res.data
    renderMap()
  } catch {
    if (!tracking.value) ElMessage.error('暂时无法获取配送信息')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTracking()
  timer = setInterval(fetchTracking, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (map) { map.destroy(); map = null }
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <div>
          <h3>配送详情</h3>
          <p>页面会自动更新配送状态</p>
        </div>
        <el-button @click="fetchTracking">刷新</el-button>
      </div>
      <div v-loading="loading" class="content">
        <template v-if="tracking">
          <el-steps :active="tracking.taskStatus >= 50 ? 3 : tracking.taskStatus >= 40 ? 2 : tracking.taskStatus >= 30 ? 1 : 0" finish-status="success" align-center>
            <el-step title="等待取餐" />
            <el-step title="已取餐" />
            <el-step title="配送中" />
            <el-step title="已送达" />
          </el-steps>
          <el-alert
            :title="statusMap[tracking.taskStatus] || '配送状态更新中'"
            :type="tracking.taskStatus === 60 ? 'error' : tracking.taskStatus >= 50 ? 'success' : 'warning'"
            :closable="false"
            style="margin-top: 24px"
          />

          <!-- 有高德 key：实时地图（骑手位置 + 轨迹）；无 key：美化占位卡 -->
          <div v-if="showMap" ref="mapEl" class="map-container"></div>
          <div v-else class="map-placeholder">
            <div class="placeholder-copy">
              <span class="placeholder-badge">MAP</span>
              <h4>地图轨迹待启用</h4>
              <p v-if="mapFailed">地图脚本加载失败，请检查高德 key 与网络；当前先展示配送时间线。</p>
              <p v-else>地图加载需要配置高德 key（src/config/amap.ts，lbs.amap.com 选「Web端(JS API)」申请），当前先为你展示配送时间线。</p>
              <p v-if="riderPos" class="placeholder-live">骑手位置已实时更新，配置 key 后即可在地图上看到。</p>
            </div>
            <el-timeline class="placeholder-timeline">
              <el-timeline-item
                v-for="item in timelineItems"
                :key="item.label"
                :timestamp="item.time"
                :type="item.done ? 'success' : 'info'"
                :hollow="!item.done"
              >
                {{ item.label }}
              </el-timeline-item>
            </el-timeline>
          </div>

          <el-descriptions :column="2" border style="margin-top: 18px">
            <el-descriptions-item label="取餐码">{{ tracking.pickupCode || '暂无' }}</el-descriptions-item>
            <el-descriptions-item label="预计送达">{{ tracking.estimatedDeliveryTime || '待确认' }}</el-descriptions-item>
            <el-descriptions-item label="取餐时间">{{ tracking.pickupTime || '尚未取餐' }}</el-descriptions-item>
            <el-descriptions-item label="送达时间">{{ tracking.deliverTime || '尚未送达' }}</el-descriptions-item>
            <el-descriptions-item label="位置状态" :span="2">{{ riderPos ? '骑手位置已实时更新' : '骑手尚未上传位置' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="tracking.driver" class="driver">
            <div>
              <strong>{{ tracking.driver.realName || '配送员' }}</strong>
              <span>{{ vehicleText(tracking.driver.vehicleType) }}</span>
            </div>
            <a :href="`tel:${tracking.driver.phone}`">{{ tracking.driver.phone }}</a>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="暂无配送任务，商家出餐后会自动创建" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn .3s ease;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  color: var(--bs-text-title);
}

.card-header p {
  margin: 6px 0 0;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.content {
  min-height: 220px;
}

.map-container {
  height: 320px;
  margin-top: 18px;
  border: 1px solid var(--hair);
  border-radius: 8px;
  overflow: hidden;
}

.map-placeholder {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: center;
  margin-top: 18px;
  padding: 24px;
  background: var(--bg);
  border: 1px dashed var(--hair);
  border-radius: 8px;
}

.placeholder-badge {
  display: inline-block;
  padding: 4px 10px;
  background: var(--green-ink);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .14em;
  border-radius: 4px;
}

.placeholder-copy h4 {
  margin: 12px 0 8px;
  color: var(--bs-text-title);
  font-size: 16px;
}

.placeholder-copy p {
  margin: 0;
  color: var(--bs-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.placeholder-copy .placeholder-live {
  margin-top: 8px;
  color: var(--green);
}

.placeholder-timeline {
  padding-top: 6px;
}

.driver {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
  padding: 16px;
  background: var(--bg);
  border-radius: 8px;
}

.driver div {
  display: flex;
  gap: 12px;
  align-items: center;
}

.driver span {
  font-size: 13px;
  color: var(--bs-text-muted);
}

.driver a {
  color: var(--green);
}

@media (max-width: 720px) {
  .map-placeholder {
    grid-template-columns: 1fr;
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
