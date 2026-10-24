<template>
  <div class="page-container">
    <el-card>
      <template #header><span>我的订单</span></template>
      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderSn" label="订单号" min-width="180" />
        <el-table-column prop="trainNumber" label="车次" width="100" />
        <el-table-column prop="departure" label="出发站" />
        <el-table-column prop="arrival" label="到达站" />
        <el-table-column prop="totalAmount" label="金额" width="100"><template #default="scope">{{ (Number(scope.row.totalAmount || 0) / 100).toFixed(1) }} 元</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="scope">{{ statusLabels[scope.row.status] || '未知' }}</template></el-table-column>
        <el-table-column label="操作" width="230"><template #default="scope"><el-button link type="primary" @click="viewDetail(scope.row)">车票详情</el-button><el-button v-if="scope.row.status === 0" link type="warning" @click="cancel(scope.row)">取消订单</el-button><el-button v-if="scope.row.status === 10" link type="danger" @click="refund(scope.row)">退票</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMyTicket, fetchCancelTicket, fetchRefundTicket } from '@/api/railway'
const orders = ref([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const router = useRouter()
const statusLabels = { 0: '待支付', 10: '已支付', 20: '已进站', 40: '已退票', 50: '已改签', 60: '已取消' }
const load = async () => { loading.value = true; try { const res = await fetchMyTicket({ current: page.value, size: 10 }); orders.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
const cancel = async (row) => { await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' }); await fetchCancelTicket({ orderSn: row.orderSn }); ElMessage.success('订单已取消'); await load() }
const refund = async (row) => { await ElMessageBox.confirm('确认申请退票？', '提示', { type: 'warning' }); await fetchRefundTicket({ orderSn: row.orderSn }); ElMessage.success('退票成功'); await load() }
const viewDetail = (row) => router.push({ name: 'OrderDetail', params: { orderSn: row.orderSn } })
onMounted(load)
</script>

<style scoped>
.page-container { padding: 20px; }
.el-pagination { margin-top: 20px; justify-content: flex-end; }
</style>
