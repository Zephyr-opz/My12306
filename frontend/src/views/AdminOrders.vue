<template>
  <div class="admin-orders-page">
    <el-card>
      <template #header><span>订单管理</span></template>
      <el-form :inline="true" :model="filters">
        <el-form-item label="订单号"><el-input v-model="filters.orderSn" clearable /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="filters.username" clearable /></el-form-item>
        <el-form-item label="车次"><el-input v-model="filters.trainNumber" clearable /></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable style="width: 130px"><el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="loadOrders">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderSn" label="订单号" min-width="190" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="trainNumber" label="车次" width="100" />
        <el-table-column prop="ridingDate" label="乘车日期" width="130" />
        <el-table-column prop="departure" label="出发站" />
        <el-table-column prop="arrival" label="到达站" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }">{{ statusLabels[row.status] || row.status }}</template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="danger" @click="removeOrder(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="loadOrders" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAdminOrder, fetchAdminOrders } from '@/api/railway'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const filters = reactive({ orderSn: '', username: '', trainNumber: '', status: null })
const statusLabels = { 0: '待支付', 10: '已支付', 20: '已进站', 40: '已退票', 50: '已改签', 60: '已取消' }

const loadOrders = async () => {
  loading.value = true
  try { const res = await fetchAdminOrders({ ...filters, current: page.value, size }); orders.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}
const removeOrder = async (row) => {
  await ElMessageBox.confirm('删除将同时删除订单明细、车票、支付记录并释放占用，确定继续吗？', '危险操作', { type: 'warning' })
  await deleteAdminOrder(row.orderSn); ElMessage.success('订单记录已删除'); await loadOrders()
}
onMounted(loadOrders)
</script>

<style scoped>
.admin-orders-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.el-pagination { margin-top: 20px; justify-content: flex-end; }
</style>
