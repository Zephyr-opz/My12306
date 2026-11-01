<template>
  <div class="ticket-page">
    <el-card class="search-card">
      <template #header><span>车票查询</span></template>
      <el-alert title="演示数据来自 my12306 数据库，可查询 2026-10-22 至 2026-11-10" type="info" :closable="false" show-icon class="source-alert" />
      <el-form :inline="true" @submit.prevent="searchTickets">
        <el-form-item label="出发站"><el-select v-model="form.fromStation" filterable placeholder="选择出发站" style="width: 180px"><el-option v-for="station in stations" :key="station.code" :label="station.name" :value="station.code" /></el-select></el-form-item>
        <el-form-item label="到达站"><el-select v-model="form.toStation" filterable placeholder="选择到达站" style="width: 180px"><el-option v-for="station in stations" :key="station.code" :label="station.name" :value="station.code" /></el-select></el-form-item>
        <el-form-item label="出发日期"><el-date-picker v-model="form.departureDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="searchTickets">查询车票</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="searched" class="result-card">
      <template #header><div class="result-header"><span>查询结果</span><el-tag type="info">{{ trainList.length }} 趟列车</el-tag></div></template>
      <el-empty v-if="!trainList.length" description="暂无符合条件的列车" />
      <el-table v-else :data="trainList" stripe>
        <el-table-column prop="trainNumber" label="车次" width="100" />
        <el-table-column label="出发" width="150"><template #default="{ row }">{{ row.departure }} {{ row.departureTime }}</template></el-table-column>
        <el-table-column label="到达" width="150"><template #default="{ row }">{{ row.arrival }} {{ row.arrivalTime }}</template></el-table-column>
        <el-table-column prop="duration" label="历时" width="100" />
        <el-table-column label="席别与票价"><template #default="{ row }"><el-tag v-for="seat in row.seatClassList" :key="seat.type" :type="Number(seat.quantity) > 0 ? 'success' : 'info'" class="seat-tag">{{ seatTypeLabels[seat.type] || ('席别' + seat.type) }}：{{ formatPrice(seat.price) }}元（{{ Number(seat.quantity) > 0 ? `余${seat.quantity}` : '售罄' }}）</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="viewStops(row.trainId)">经停站</el-button><el-button link type="success" :disabled="!hasAvailableSeat(row)" @click="openPurchase(row)">{{ hasAvailableSeat(row) ? '购票' : '售罄' }}</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="stopsVisible" title="经停站信息" width="680px"><el-table :data="stops" stripe><el-table-column prop="sequence" label="序号" width="80" /><el-table-column prop="departure" label="车站" /><el-table-column prop="arrivalTime" label="到站时间" /><el-table-column prop="departureTime" label="发车时间" /><el-table-column prop="stopoverTime" label="停留" /></el-table></el-dialog>

    <el-dialog v-model="purchaseVisible" title="确认购票" width="560px">
      <el-descriptions v-if="selectedTrain" :column="2" border>
        <el-descriptions-item label="车次">{{ selectedTrain.trainNumber }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ form.departureDate }}</el-descriptions-item>
        <el-descriptions-item label="区间">{{ selectedTrain.departure }} → {{ selectedTrain.arrival }}</el-descriptions-item>
        <el-descriptions-item label="席别"><el-select v-model="purchaseSeatType"><el-option v-for="seat in selectedTrain.seatClassList" :key="seat.type" :label="seatOptionLabel(seat)" :value="seat.type" :disabled="Number(seat.quantity) <= 0" /></el-select></el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <div class="passenger-title">选择乘车人</div>
      <el-checkbox-group v-model="selectedPassengerIds"><el-checkbox v-for="passenger in passengers" :key="passenger.id" :label="passenger.id">{{ passenger.realName }}（{{ passenger.idCard }}）</el-checkbox></el-checkbox-group>
      <el-empty v-if="!passengers.length" description="请先在乘车人页面添加乘车人" />
      <template #footer><el-button @click="purchaseVisible = false">取消</el-button><el-button type="primary" :loading="purchasing" :disabled="!selectedSeatAvailable" @click="submitPurchase">确认购票并支付</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchStationAll, fetchTicketSearch, fetchTrainStation, fetchPassengerList, fetchBuyTicket, fetchPay } from '@/api/railway'

const userStore = useUserStore()
const stations = ref([])
const passengers = ref([])
const trainResult = ref({ trainList: [] })
const loading = ref(false)
const purchasing = ref(false)
const searched = ref(false)
const stopsVisible = ref(false)
const purchaseVisible = ref(false)
const stops = ref([])
const selectedTrain = ref(null)
const selectedPassengerIds = ref([])
const purchaseSeatType = ref(null)
const seatTypeLabels = { 0: '商务座', 1: '一等座', 2: '二等座', 3: '动卧', 4: '高级软卧', 5: '软卧', 6: '硬卧', 7: '软座', 8: '硬座', 9: '无座' }
const form = reactive({ fromStation: '', toStation: '', departureDate: '2026-10-22' })
const trainList = computed(() => trainResult.value.trainList || [])
const formatPrice = (price) => (Number(price || 0) / 100).toFixed(1)
const hasAvailableSeat = (row) => (row.seatClassList || []).some(seat => Number(seat.quantity) > 0)
const seatOptionLabel = (seat) => `${seatTypeLabels[seat.type] || seat.type} ${formatPrice(seat.price)}元${Number(seat.quantity) > 0 ? `（余${seat.quantity}）` : '（售罄）'}`
const selectedSeatAvailable = computed(() => {
  const seat = selectedTrain.value?.seatClassList?.find(item => item.type === purchaseSeatType.value)
  return !!seat && Number(seat.quantity) > 0
})
const ticketCacheKey = 'my12306:tickets:query:v1'

const restoreTicketCache = () => {
  try {
    const cached = JSON.parse(sessionStorage.getItem(ticketCacheKey) || 'null')
    if (!cached) return
    if (cached.form) Object.assign(form, cached.form)
    if (cached.result) trainResult.value = cached.result
    searched.value = cached.searched === true
  } catch {
    sessionStorage.removeItem(ticketCacheKey)
  }
}

const saveTicketCache = () => {
  sessionStorage.setItem(ticketCacheKey, JSON.stringify({ form: { ...form }, result: trainResult.value, searched: searched.value }))
}

onMounted(async () => {
  restoreTicketCache()
  try {
    const res = await fetchStationAll()
    stations.value = res.data || []
    if (searched.value && form.fromStation && form.toStation) {
      const latest = await fetchTicketSearch(form)
      trainResult.value = latest.data || { trainList: [] }
      saveTicketCache()
    }
  } catch { ElMessage.error('车站或车票数据加载失败') }
})

const searchTickets = async () => {
  if (!form.fromStation || !form.toStation) return ElMessage.warning('请选择出发站和到达站')
  loading.value = true; searched.value = true
  try { const res = await fetchTicketSearch(form); trainResult.value = res.data || { trainList: [] }; saveTicketCache() } finally { loading.value = false }
}

const viewStops = async (trainId) => { const res = await fetchTrainStation({ trainId }); stops.value = res.data || []; stopsVisible.value = true }

const openPurchase = async (row) => {
  selectedTrain.value = row
  purchaseSeatType.value = row.seatClassList?.[0]?.type ?? null
  selectedPassengerIds.value = []
  const res = await fetchPassengerList({ username: userStore.userInfo?.username })
  passengers.value = res.data || []
  purchaseVisible.value = true
}

const submitPurchase = async () => {
  if (!selectedPassengerIds.value.length) return ElMessage.warning('请选择至少一名乘车人')
  if (purchaseSeatType.value === null) return ElMessage.warning('请选择席别')
  purchasing.value = true
  try {
    const result = await fetchBuyTicket({ trainId: selectedTrain.value.trainId, departure: selectedTrain.value.departure, arrival: selectedTrain.value.arrival, departureDate: form.departureDate, chooseType: purchaseSeatType.value, passengers: selectedPassengerIds.value.map(passengerId => ({ passengerId })) })
    const order = result.data
    await fetchPay({ orderSn: order.orderSn, channel: 1, tradeType: 1, subject: `My12306 ${selectedTrain.value.trainNumber} 车票`, totalAmount: order.totalAmount })
    purchaseVisible.value = false
    ElMessage.success(`购票成功，订单号：${order.orderSn}`)
    await searchTickets()
  } finally { purchasing.value = false }
}
</script>

<style scoped>
.ticket-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.search-card, .result-card { max-width: 1280px; margin: 0 auto 20px; }
.result-header { display: flex; justify-content: space-between; align-items: center; }
.seat-tag { margin-right: 8px; margin-bottom: 4px; }
.passenger-title { margin-bottom: 12px; font-weight: 600; }
</style>
