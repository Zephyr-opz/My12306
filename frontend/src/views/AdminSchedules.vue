<template>
  <div class="admin-schedules-page">
    <el-card>
      <template #header><div class="card-header"><span>运行计划管理</span><el-tag type="info">模板自动生成后可在这里单日调整</el-tag></div></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="车次"><el-select v-model="filters.trainId" clearable placeholder="全部车次" style="width: 180px"><el-option v-for="train in trains" :key="train.id" :label="train.trainNumber" :value="train.id" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="filters.departureDate" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="loadSchedules">查询</el-button></el-form-item>
      </el-form>
      <el-divider content-position="left">新增单日运行计划</el-divider>
      <el-form :inline="true" :model="form" class="filter-form">
        <el-form-item label="车次"><el-select v-model="form.trainId" placeholder="选择车次" style="width: 180px"><el-option v-for="train in trains" :key="train.id" :label="train.trainNumber" :value="train.id" /></el-select></el-form-item>
        <el-form-item label="运行日期"><el-date-picker v-model="form.departureDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="发车时间"><el-date-picker v-model="form.departureTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="到达时间"><el-date-picker v-model="form.arrivalTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" placeholder="可选" /></el-form-item>
        <el-form-item><el-button type="success" :loading="saving" @click="saveSchedule">新增计划</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="range-card">
      <template #header><span>日期范围停用/恢复</span></template>
      <el-form :inline="true" :model="rangeForm">
        <el-form-item label="车次"><el-select v-model="rangeForm.trainId" placeholder="选择车次" style="width: 180px"><el-option v-for="train in trains" :key="train.id" :label="train.trainNumber" :value="train.id" /></el-select></el-form-item>
        <el-form-item label="开始"><el-date-picker v-model="rangeForm.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束"><el-date-picker v-model="rangeForm.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="warning" :loading="rangeLoading" @click="changeRangeStatus(0)">停用范围</el-button><el-button type="success" :loading="rangeLoading" @click="changeRangeStatus(1)">恢复范围</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <template #header><span>运行计划列表</span></template>
      <el-table :data="schedules" v-loading="loading" stripe>
        <el-table-column prop="trainId" label="车次ID" width="90" />
        <el-table-column prop="departureDate" label="运行日期" width="130" />
        <el-table-column prop="departureTime" label="发车时间" width="180" />
        <el-table-column prop="arrivalTime" label="到达时间" width="180" />
        <el-table-column prop="planStatus" label="计划状态" width="100"><template #default="{ row }"><el-tag :type="row.planStatus === 1 ? 'success' : 'info'">{{ row.planStatus === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="190"><template #default="{ row }"><el-button link type="primary" @click="editSchedule(row)">编辑</el-button><el-button link type="warning" @click="toggleSchedule(row)">{{ row.planStatus === 1 ? '停用' : '启用' }}</el-button><el-button link type="danger" @click="removeSchedule(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑运行计划" width="520px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="运行日期"><el-date-picker v-model="editForm.departureDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="发车时间"><el-date-picker v-model="editForm.departureTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="到达时间"><el-date-picker v-model="editForm.arrivalTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="计划状态"><el-switch v-model="editForm.planStatus" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible = false">取消</el-button><el-button type="primary" @click="updateSchedule">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAdminSchedule, deleteAdminSchedule, fetchAdminSchedules, fetchAdminTrains, updateAdminSchedule, updateAdminScheduleStatusRange } from '@/api/railway'

const trains = ref([])
const schedules = ref([])
const loading = ref(false)
const saving = ref(false)
const rangeLoading = ref(false)
const editVisible = ref(false)
const filters = reactive({ trainId: null, departureDate: '' })
const form = reactive({ trainId: null, departureDate: '', departureTime: '', arrivalTime: '', saleStatus: 0, planStatus: 1, remark: '' })
const rangeForm = reactive({ trainId: null, startDate: '', endDate: '' })
const editForm = reactive({ id: null, trainId: null, departureDate: '', departureTime: '', arrivalTime: '', saleStatus: 0, planStatus: 1, remark: '' })

const loadTrains = async () => { const res = await fetchAdminTrains(); trains.value = res.data || [] }
const loadSchedules = async () => {
  loading.value = true
  try {
    const params = {}
    if (filters.trainId) params.trainId = filters.trainId
    if (filters.departureDate) params.departureDate = filters.departureDate
    const res = await fetchAdminSchedules(params)
    schedules.value = res.data || []
  } finally { loading.value = false }
}
const saveSchedule = async () => {
  if (!form.trainId || !form.departureDate || !form.departureTime || !form.arrivalTime) return ElMessage.warning('请填写车次、日期和时间')
  saving.value = true
  try { await createAdminSchedule(form); ElMessage.success('运行计划已新增'); await loadSchedules() } finally { saving.value = false }
}
const editSchedule = (row) => { Object.assign(editForm, row); editVisible.value = true }
const updateSchedule = async () => { await updateAdminSchedule(editForm.id, editForm); ElMessage.success('运行计划已更新'); editVisible.value = false; await loadSchedules() }
const toggleSchedule = async (row) => { const body = { ...row, planStatus: row.planStatus === 1 ? 0 : 1 }; await updateAdminSchedule(row.id, body); ElMessage.success('状态已更新'); await loadSchedules() }
const removeSchedule = async (row) => { await ElMessageBox.confirm('确定删除该运行计划吗？已有订单或车票的计划不应删除。', '确认', { type: 'warning' }); await deleteAdminSchedule(row.id); ElMessage.success('运行计划已删除'); await loadSchedules() }
const changeRangeStatus = async (planStatus) => {
  if (!rangeForm.trainId || !rangeForm.startDate || !rangeForm.endDate) return ElMessage.warning('请选择车次和日期范围')
  rangeLoading.value = true
  try {
    const res = await updateAdminScheduleStatusRange({ ...rangeForm, planStatus })
    ElMessage.success(`${planStatus === 1 ? '恢复' : '停用'}完成，影响 ${Number(res.data || 0)} 条运行计划`)
    await loadSchedules()
  } finally { rangeLoading.value = false }
}

onMounted(async () => { await loadTrains(); await loadSchedules() })
</script>

<style scoped>
.admin-schedules-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.filter-form { display: flex; flex-wrap: wrap; gap: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.range-card, .list-card { margin-top: 20px; }
</style>
