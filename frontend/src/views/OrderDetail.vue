<template>
  <div class="order-detail-page" v-loading="loading">
    <el-card v-if="order">
      <template #header>
        <div class="detail-header"><span>车票详细信息</span><el-tag :type="statusType(order.status)">{{ statusLabels[order.status] || '未知状态' }}</el-tag></div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单号">{{ order.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="车次">{{ order.trainNumber }}</el-descriptions-item>
        <el-descriptions-item label="乘车日期">{{ order.ridingDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="出发站">{{ order.departure }}</el-descriptions-item>
        <el-descriptions-item label="到达站">{{ order.arrival }}</el-descriptions-item>
        <el-descriptions-item label="出发时间">{{ formatDateTime(order.departureTime) }}</el-descriptions-item>
        <el-descriptions-item label="到达时间">{{ formatDateTime(order.arrivalTime) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ money(order.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(order.orderTime || order.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">乘车人和座位</el-divider>
      <el-table :data="passengerDetails" border stripe>
        <el-table-column prop="realName" label="乘车人" min-width="120" />
        <el-table-column label="席别" width="120"><template #default="{ row }">{{ seatLabel(row.seatType) }}</template></el-table-column>
        <el-table-column prop="carriageNumber" label="车厢" width="100" />
        <el-table-column prop="seatNumber" label="座位" width="100" />
        <el-table-column label="区间" min-width="180"><template #default="{ row }">{{ row.departure }} → {{ row.arrival }}</template></el-table-column>
        <el-table-column label="票价" width="120"><template #default="{ row }">{{ money(row.price || row.amount) }}</template></el-table-column>
        <el-table-column label="票状态" width="110"><template #default="{ row }">{{ itemStatusLabels[row.status] || '未知状态' }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!passengerDetails.length" description="暂无车票明细" />

      <div class="detail-actions"><el-button @click="router.back()">返回订单</el-button></div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchOrderBySn } from '@/api/railway'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const order = ref(null)
const passengerDetails = ref([])
const statusLabels = { 0: '待支付', 10: '已支付', 20: '已进站', 40: '已退票', 50: '已改签', 60: '已取消' }
const itemStatusLabels = { 0: '待支付', 10: '已支付', 40: '已退票', 60: '已取消' }

const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
const money = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const seatLabel = (value) => ({ 0: '商务座', 1: '一等座', 2: '二等座' }[Number(value)] || '其他席别')
const statusType = (value) => ({ 0: 'warning', 10: 'success', 40: 'info', 60: 'info' }[value] || 'info')

const load = async () => {
  loading.value = true
  try {
    const res = await fetchOrderBySn({ orderSn: route.params.orderSn })
    order.value = res.data?.order || null
    passengerDetails.value = res.data?.passengerDetails || []
  } catch (error) {
    ElMessage.error('车票详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.detail-header { display: flex; justify-content: space-between; align-items: center; }
.order-detail-page .el-divider { margin: 26px 0 16px; }
.detail-actions { display: flex; justify-content: center; margin-top: 24px; }
</style>
