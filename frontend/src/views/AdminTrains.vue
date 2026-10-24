<template>
  <div class="admin-trains-page">
    <el-card>
      <template #header><div class="card-header"><span>车次模板管理</span><el-tag type="info">按日期范围每天生成固定时间</el-tag></div></template>
      <el-alert title="这里只维护车次模板；保存后系统会按生效日期范围生成每日运行计划。" type="info" :closable="false" class="tip" />
      <el-form ref="formRef" :model="form" label-width="110px" class="train-form">
        <el-row :gutter="18">
          <el-col :span="8"><el-form-item label="车次号" required><el-input v-model="form.trainNumber" placeholder="例如 G2001" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="始发时间" required><el-time-picker v-model="form.departureTime" value-format="HH:mm:ss" placeholder="选择时间" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="终到时间" required><el-time-picker v-model="form.arrivalTime" value-format="HH:mm:ss" placeholder="选择时间" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="默认席别"><el-select v-model="form.trainTag" placeholder="选择席别"><el-option v-for="option in seatTypeOptions" :key="option.value" :label="option.label" :value="String(option.value)" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="运行开始" required><el-date-picker v-model="form.runStartDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="运行结束" required><el-date-picker v-model="form.runEndDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="始发站"><el-select v-model="form.startStationId" filterable clearable placeholder="选择车站" @change="syncBoundaryStationsFromSelection"><el-option v-for="station in stations" :key="station.id" :label="station.name + '（' + station.code + '）'" :value="station.id" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="终点站"><el-select v-model="form.endStationId" filterable clearable placeholder="选择车站" @change="syncBoundaryStationsFromSelection"><el-option v-for="station in stations" :key="station.id" :label="station.name + '（' + station.code + '）'" :value="station.id" /></el-select></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">经停站（按顺序填写）</el-divider>
        <el-alert class="field-help" type="info" :closable="false" title="说明：首行自动作为始发站，末行自动作为终点站；只填写中间站停留分钟，系统会根据始发/终到时间自动计算各站到发时间；首站和末站停留固定为 0。" />
        <div class="stop-toolbar">
          <span>上方始发站和终点站会自动同步到首行、末行；拖动站点后也会同步回上方。</span>
          <el-button type="primary" plain circle :icon="Refresh" title="刷新经停站" aria-label="刷新经停站" @click="refreshBoundaryStops" />
        </div>
        <div v-for="(stop, index) in form.stops" :key="index" class="array-row stop-row" :class="{ dragging: draggingStopIndex === index }" @dragover.prevent @drop="dropStop(index)">
          <button type="button" class="drag-handle" draggable="true" title="拖动调整顺序" @dragstart="startStopDrag(index)" @dragend="draggingStopIndex = null">☷</button>
          <span class="sequence-label">第{{ index + 1 }}站</span>
          <el-tag v-if="index === 0" type="success">始发站</el-tag>
          <el-tag v-else-if="index === form.stops.length - 1" type="warning">终点站</el-tag>
          <el-select v-model="stop.stationId" filterable clearable placeholder="选择车站" class="station-select" @change="syncStopStation(index)"><el-option v-for="station in stations" :key="station.id" :label="station.name + '（' + station.code + '）'" :value="station.id" /></el-select>
          <el-input-number v-model="stop.stopoverTime" :min="0" :max="999" controls-position="right" placeholder="停留分钟" :disabled="isBoundaryStop(index)" />
          <el-button type="danger" class="row-delete-button" @click="removeStop(index)" :disabled="form.stops.length <= 2">删除</el-button>
        </div>
        <el-button type="primary" plain @click="addStop">增加经停站</el-button>

        <el-divider content-position="left">区间票价（单位：分）</el-divider>
        <el-alert class="field-help" type="info" :closable="false" title="说明：每个席别只填写一条全程票价；系统会按乘车分钟占全程分钟的比例自动生成区间票价。席别请选择商务座、一等座或二等座；票价单位是“分”，5000 表示 50.00 元。" />
        <div v-for="(fare, index) in form.fares" :key="index" class="array-row">
          <el-select v-model="fare.seatType" placeholder="选择席别"><el-option v-for="option in seatTypeOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select>
          <el-input-number v-model="fare.price" :min="0" controls-position="right" placeholder="全程票价（分）" />
          <el-button type="danger" class="row-delete-button" @click="removeFare(index)">删除</el-button>
        </div>
        <el-button plain @click="addFare">增加票价</el-button>

        <el-divider content-position="left">车厢容量</el-divider>
        <el-alert class="field-help" type="info" :closable="false" title="说明：车厢类型请选择商务座、一等座或二等座；座位数表示该车厢可生成的座位总数，系统会按 001、002、003……生成座位。" />
        <div v-for="(carriage, index) in form.carriages" :key="index" class="array-row">
          <el-input v-model="carriage.carriageNumber" placeholder="车厢号" />
          <el-select v-model="carriage.carriageType" placeholder="选择席别"><el-option v-for="option in seatTypeOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select>
          <el-input-number v-model="carriage.seatCount" :min="1" controls-position="right" placeholder="座位数" />
          <el-button type="danger" class="row-delete-button" @click="removeCarriage(index)">删除</el-button>
        </div>
        <el-button plain @click="addCarriage">增加车厢</el-button>

        <div class="form-actions"><el-button type="primary" :loading="previewLoading" @click="previewTrain">预览并检查</el-button><el-button v-if="editingId" @click="resetForm">取消编辑</el-button></div>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <template #header><span>已有车次模板</span></template>
      <el-table :data="trains" v-loading="loading" stripe>
        <el-table-column prop="trainNumber" label="车次" width="120" />
        <el-table-column prop="startStation" label="始发站" />
        <el-table-column prop="endStation" label="终点站" />
        <el-table-column prop="runStartDate" label="开始日期" />
        <el-table-column prop="runEndDate" label="结束日期" />
        <el-table-column label="操作" width="180"><template #default="{ row }"><el-button type="primary" link @click="editTrain(row)">编辑</el-button><el-button type="danger" link @click="removeTrain(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-card class="pause-card">
      <template #header><span>车次日期范围停用/恢复</span></template>
      <el-form :inline="true" :model="pauseForm">
        <el-form-item label="车次"><el-select v-model="pauseForm.trainId" placeholder="选择车次" style="width: 180px"><el-option v-for="train in trains" :key="train.id" :label="train.trainNumber" :value="train.id" /></el-select></el-form-item>
        <el-form-item label="开始"><el-date-picker v-model="pauseForm.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束"><el-date-picker v-model="pauseForm.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="warning" :loading="pauseLoading" @click="changePauseStatus(0)">停用</el-button><el-button type="success" :loading="pauseLoading" @click="changePauseStatus(1)">恢复</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { deleteAdminTrain, fetchAdminStations, fetchAdminTrainDetail, fetchAdminTrains, previewAdminTrain, updateAdminScheduleStatusRange } from '@/api/railway'

const router = useRouter()
const formRef = ref()
const previewLoading = ref(false)
const loading = ref(false)
const pauseLoading = ref(false)
const trains = ref([])
const stations = ref([])
const draggingStopIndex = ref(null)
const editingId = ref(null)
const previewStorageKey = 'my12306.admin.train.preview'
const seatTypeOptions = [
  { value: 0, label: '商务座' },
  { value: 1, label: '一等座' },
  { value: 2, label: '二等座' }
]

const newStop = (sequence, stopoverTime = 5) => ({ sequence, stationId: null, departure: '', stopoverTime })
const form = reactive({
  trainNumber: '', trainType: 0, trainTag: '0', trainBrand: '0',
  startStation: '', endStation: '', startStationId: null, endStationId: null, startRegion: '', endRegion: '',
  saleStatus: 0, departureTime: '', arrivalTime: '',
  runStartDate: '2026-10-22', runEndDate: '2026-11-05',
  stops: [newStop(1, 0), newStop(2, 0)],
  fares: [{ seatType: 2, price: 5000 }],
  carriages: [{ carriageNumber: '01', carriageType: 2, seatCount: 20 }]
})
const pauseForm = reactive({ trainId: null, startDate: '', endDate: '' })

const isBoundaryStop = (index) => index === 0 || index === form.stops.length - 1
const syncStopBoundaries = () => {
  form.stops.forEach((stop, index) => {
    stop.sequence = index + 1
    if (isBoundaryStop(index)) stop.stopoverTime = 0
    else if (stop.stopoverTime == null) stop.stopoverTime = 5
  })
}
const stationById = (id) => stations.value.find(station => String(station.id) === String(id))
const stationIdByName = (name) => stations.value.find(station => station.name === name)?.id || null
const applyStationToStop = (stop, stationId) => {
  const station = stationById(stationId)
  stop.stationId = station?.id || null
  stop.departure = station?.name || ''
  stop.arrival = station?.name || ''
}
const syncStationInputsFromStops = () => {
  form.startStation = form.stops[0]?.departure || ''
  form.endStation = form.stops[form.stops.length - 1]?.departure || ''
  form.startStationId = form.stops[0]?.stationId || stationIdByName(form.startStation)
  form.endStationId = form.stops[form.stops.length - 1]?.stationId || stationIdByName(form.endStation)
}
const syncStopStation = (index) => {
  applyStationToStop(form.stops[index], form.stops[index].stationId)
  syncStationInputsFromStops()
}
const syncBoundaryStationsFromSelection = () => {
  if (form.stops.length < 2) return
  applyStationToStop(form.stops[0], form.startStationId)
  applyStationToStop(form.stops[form.stops.length - 1], form.endStationId)
  syncStationInputsFromStops()
}
const refreshBoundaryStops = () => {
  if (form.stops.length < 2) return
  syncBoundaryStationsFromSelection()
  syncStopBoundaries()
  ElMessage.success('始发站和终点站已同步到经停站')
}
const addStop = () => {
  const insertAt = Math.max(form.stops.length - 1, 1)
  form.stops.splice(insertAt, 0, newStop(insertAt + 1, 5))
  syncStopBoundaries()
}
const removeStop = (index) => {
  if (form.stops.length <= 2) return
  form.stops.splice(index, 1)
  syncStopBoundaries()
  syncStationInputsFromStops()
}
const startStopDrag = (index) => { draggingStopIndex.value = index }
const dropStop = (targetIndex) => {
  if (draggingStopIndex.value === null || draggingStopIndex.value === targetIndex) return
  const [moved] = form.stops.splice(draggingStopIndex.value, 1)
  form.stops.splice(targetIndex, 0, moved)
  draggingStopIndex.value = null
  syncStopBoundaries()
  syncStationInputsFromStops()
}
const addFare = () => form.fares.push({ seatType: 2, price: 5000 })
const removeFare = (index) => form.fares.splice(index, 1)
const addCarriage = () => form.carriages.push({ carriageNumber: String(form.carriages.length + 1).padStart(2, '0'), carriageType: 2, seatCount: 20 })
const removeCarriage = (index) => form.carriages.splice(index, 1)

const dateTime = (date, clock) => clock ? `${date}T${clock}` : null
const payload = () => ({
  ...form,
  id: editingId.value || undefined,
  startStation: form.stops[0]?.departure || '',
  endStation: form.stops[form.stops.length - 1]?.departure || '',
  departureTime: dateTime(form.runStartDate, form.departureTime),
  arrivalTime: dateTime(form.runStartDate, form.arrivalTime),
  stops: form.stops.map((stop, index) => ({
    sequence: String(index + 1).padStart(2, '0'), stationId: stop.stationId, departure: stop.departure, arrival: stop.departure,
    stopoverTime: isBoundaryStop(index) ? 0 : stop.stopoverTime
  })),
  fares: form.fares.map(fare => ({ ...fare, departure: form.stops[0]?.departure, arrival: form.stops[form.stops.length - 1]?.departure }))
})

const loadTrains = async () => {
  loading.value = true
  try { const res = await fetchAdminTrains(); trains.value = res.data || [] } finally { loading.value = false }
}
const loadStations = async () => {
  const res = await fetchAdminStations()
  stations.value = res.data || []
  syncStationInputsFromStops()
}

const previewTrain = async () => {
  syncStopBoundaries()
  if (!form.trainNumber || !form.runStartDate || !form.runEndDate || !form.departureTime || !form.arrivalTime) return ElMessage.warning('请完整填写车次和日期时间')
  if (form.stops.length < 2 || form.stops.some(stop => !stop.stationId)) return ElMessage.warning('请从车站选项中选择至少两个经停站')
  if (!form.fares.length || form.fares.some(fare => fare.seatType == null || fare.price == null)) return ElMessage.warning('请至少填写一条完整全程票价')
  if (!form.carriages.length || form.carriages.some(carriage => !carriage.carriageNumber || !carriage.seatCount)) return ElMessage.warning('请至少配置一个车厢及座位数')
  if (form.runEndDate < form.runStartDate) return ElMessage.warning('运行结束日期不能早于开始日期')
  previewLoading.value = true
  try {
    const sourceForm = JSON.parse(JSON.stringify(form))
    const sourcePayload = payload()
    const res = await previewAdminTrain(sourcePayload)
    sessionStorage.setItem(previewStorageKey, JSON.stringify({
      editingId: editingId.value,
      form: sourceForm,
      payload: sourcePayload,
      preview: res.data
    }))
    await router.push({ name: 'AdminTrainPreview' })
  } finally { previewLoading.value = false }
}

const clockOf = (value) => value ? String(value).slice(11, 19) : ''
const editTrain = async (row) => {
  const res = await fetchAdminTrainDetail(row.id)
  const detail = res.data
  const train = detail.train || {}
  editingId.value = row.id
  const stopList = detail.stops || []
  const firstStation = stopList[0]?.departure || train.startStation
  const lastStation = stopList[stopList.length - 1]?.departure || train.endStation
  const fullFares = (detail.fares || []).filter(fare => fare.departure === firstStation && fare.arrival === lastStation)
  const fares = fullFares.length ? fullFares : Object.values((detail.fares || []).reduce((map, fare) => {
    const current = map[fare.seatType]
    if (!current || Number(fare.price) > Number(current.price)) map[fare.seatType] = { seatType: fare.seatType, price: fare.price }
    return map
  }, {}))
  Object.assign(form, {
    ...train,
    departureTime: clockOf(train.departureTime), arrivalTime: clockOf(train.arrivalTime),
    stops: stopList.map((stop, index) => ({ sequence: index + 1, stationId: stop.stationId || stationIdByName(stop.departure), departure: stop.departure, stopoverTime: stop.stopoverTime || 0 })),
    fares: fares.map(fare => ({ seatType: fare.seatType, price: fare.price })), carriages: detail.carriages || []
  })
  form.trainTag = String(form.trainTag ?? '0')
  syncStopBoundaries()
  syncStationInputsFromStops()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const resetForm = () => {
  editingId.value = null
  Object.assign(form, { trainNumber: '', startStation: '', endStation: '', startStationId: null, endStationId: null, startRegion: '', endRegion: '', departureTime: '', arrivalTime: '', runStartDate: '2026-10-22', runEndDate: '2026-11-05', stops: [newStop(1, 0), newStop(2, 0)], fares: [{ seatType: 2, price: 5000 }], carriages: [{ carriageNumber: '01', carriageType: 2, seatCount: 20 }] })
  syncStopBoundaries()
}
const changePauseStatus = async (planStatus) => {
  if (!pauseForm.trainId || !pauseForm.startDate || !pauseForm.endDate) return ElMessage.warning('请选择车次和日期范围')
  pauseLoading.value = true
  try {
    const res = await updateAdminScheduleStatusRange({ ...pauseForm, planStatus })
    ElMessage.success(`${planStatus === 1 ? '恢复' : '停用'}完成，影响 ${Number(res.data || 0)} 条运行计划`)
  } finally { pauseLoading.value = false }
}

const removeTrain = async (row) => {
  await ElMessageBox.confirm(`确定删除车次 ${row.trainNumber} 吗？已有订单的车次无法删除。`, '确认', { type: 'warning' })
  await deleteAdminTrain(row.id); ElMessage.success('删除成功'); await loadTrains()
}

watch(() => form.startStationId, (value) => {
  if (form.stops[0] && form.stops[0].stationId !== value) {
    applyStationToStop(form.stops[0], value)
    syncStationInputsFromStops()
  }
})
watch(() => form.endStationId, (value) => {
  const last = form.stops[form.stops.length - 1]
  if (last && last.stationId !== value) {
    applyStationToStop(last, value)
    syncStationInputsFromStops()
  }
})
watch(() => form.stops.map(stop => stop.stationId), () => {
  syncStationInputsFromStops()
}, { deep: true })

const restoreDraft = () => {
  const raw = sessionStorage.getItem(previewStorageKey)
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    if (draft.form) Object.assign(form, draft.form)
    editingId.value = draft.editingId || null
    syncStopBoundaries()
    sessionStorage.removeItem(previewStorageKey)
  } catch (error) {
    sessionStorage.removeItem(previewStorageKey)
  }
}

onMounted(async () => {
  restoreDraft()
  syncStopBoundaries()
  await Promise.all([loadTrains(), loadStations()])
  syncStationInputsFromStops()
})
</script>

<style scoped>
.admin-trains-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tip { margin-bottom: 18px; }
.field-help { margin: 0 0 12px; }
.stop-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 0 12px; color: #909399; font-size: 13px; }
.train-form { max-width: 1200px; }
.array-row { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; flex-wrap: wrap; }
.array-row > * { min-width: 130px; }
.array-row > .row-delete-button { flex: 0 0 64px; width: 64px; min-width: 64px; padding: 8px 10px; }
.row-delete-button { margin-left: auto; background: #f56c6c; border-color: #f56c6c; color: #fff; }
.row-delete-button:hover { background: #f78989; border-color: #f78989; color: #fff; }
.stop-row { padding: 6px; border: 1px dashed transparent; border-radius: 4px; flex-wrap: nowrap; overflow-x: auto; }
.stop-row.dragging { opacity: .55; border-color: #409eff; background: #ecf5ff; }
.drag-handle { width: 34px; height: 32px; cursor: grab; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; color: #409eff; font-size: 18px; }
.sequence-label { width: 62px; color: #606266; }
.station-select { width: 220px; }
.form-actions { margin-top: 24px; }
.list-card { margin-top: 20px; }
.pause-card { margin-top: 20px; }
</style>
