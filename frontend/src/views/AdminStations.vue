<template>
  <div class="admin-stations-page">
    <el-card>
      <template #header><div class="card-header"><span>地区车站维护</span><el-tag type="info">仅维护基础资料</el-tag></div></template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="地区维护" name="regions">
          <div class="toolbar">
            <el-input v-model="regionKeyword" clearable placeholder="搜索地区名称/编码" @keyup.enter="loadRegions" />
            <el-button type="primary" @click="loadRegions">查询</el-button>
            <el-button type="success" @click="openRegionDialog()">新增地区</el-button>
          </div>
          <el-table :data="regions" v-loading="regionLoading" border stripe>
            <el-table-column prop="name" label="地区名称" />
            <el-table-column prop="fullName" label="地区全称" />
            <el-table-column prop="code" label="地区编码" />
            <el-table-column prop="initial" label="首字母" width="100" />
            <el-table-column prop="spell" label="拼音" />
            <el-table-column label="热门" width="90"><template #default="{ row }">{{ row.popularFlag === 1 ? '是' : '否' }}</template></el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }"><el-button type="primary" link @click="openRegionDialog(row)">编辑</el-button><el-button type="danger" link @click="removeRegion(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="车站维护" name="stations">
          <div class="toolbar">
            <el-input v-model="stationKeyword" clearable placeholder="搜索车站名称/编码/拼音" @keyup.enter="loadStations" />
            <el-button type="primary" @click="loadStations">查询</el-button>
            <el-button type="success" @click="openStationDialog()">新增车站</el-button>
          </div>
          <el-table :data="stations" v-loading="stationLoading" border stripe>
            <el-table-column prop="code" label="车站编码" width="130" />
            <el-table-column prop="name" label="车站名称" />
            <el-table-column prop="spell" label="拼音" />
            <el-table-column prop="regionName" label="所属地区" />
            <el-table-column prop="region" label="地区编码" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }"><el-button type="primary" link @click="openStationDialog(row)">编辑</el-button><el-button type="danger" link @click="removeStation(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="regionDialogVisible" :title="regionEditingId ? '编辑地区' : '新增地区'" width="520px">
      <el-form :model="regionForm" label-width="90px">
        <el-form-item label="地区名称" required><el-input v-model="regionForm.name" /></el-form-item>
        <el-form-item label="地区全称"><el-input v-model="regionForm.fullName" /></el-form-item>
        <el-form-item label="地区编码" required><el-input v-model="regionForm.code" /></el-form-item>
        <el-form-item label="首字母"><el-input v-model="regionForm.initial" /></el-form-item>
        <el-form-item label="拼音"><el-input v-model="regionForm.spell" /></el-form-item>
        <el-form-item label="热门地区"><el-switch v-model="regionForm.popularFlag" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="regionDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRegion">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="stationDialogVisible" :title="stationEditingId ? '编辑车站' : '新增车站'" width="520px">
      <el-form :model="stationForm" label-width="90px">
        <el-form-item label="车站编码" required><el-input v-model="stationForm.code" /></el-form-item>
        <el-form-item label="车站名称" required><el-input v-model="stationForm.name" /></el-form-item>
        <el-form-item label="拼音"><el-input v-model="stationForm.spell" /></el-form-item>
        <el-form-item label="所属地区"><el-select v-model="stationForm.region" filterable clearable placeholder="选择地区" @change="syncRegionName"><el-option v-for="region in regions" :key="region.id" :label="region.name + '（' + region.code + '）'" :value="region.code" /></el-select></el-form-item>
        <el-form-item label="地区名称"><el-input v-model="stationForm.regionName" disabled /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stationDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveStation">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAdminRegion, createAdminStation, deleteAdminRegion, deleteAdminStation, fetchAdminRegions, fetchAdminStations, updateAdminRegion, updateAdminStation } from '@/api/railway'

const activeTab = ref('regions')
const regions = ref([])
const stations = ref([])
const regionKeyword = ref('')
const stationKeyword = ref('')
const regionLoading = ref(false)
const stationLoading = ref(false)
const saving = ref(false)
const regionDialogVisible = ref(false)
const stationDialogVisible = ref(false)
const regionEditingId = ref(null)
const stationEditingId = ref(null)
const regionForm = reactive({ name: '', fullName: '', code: '', initial: '', spell: '', popularFlag: 0 })
const stationForm = reactive({ code: '', name: '', spell: '', region: '', regionName: '' })

const loadRegions = async () => {
  regionLoading.value = true
  try { const res = await fetchAdminRegions({ keyword: regionKeyword.value || undefined }); regions.value = res.data || [] } finally { regionLoading.value = false }
}
const loadStations = async () => {
  stationLoading.value = true
  try { const res = await fetchAdminStations({ keyword: stationKeyword.value || undefined }); stations.value = res.data || [] } finally { stationLoading.value = false }
}
const resetRegion = () => Object.assign(regionForm, { name: '', fullName: '', code: '', initial: '', spell: '', popularFlag: 0 })
const resetStation = () => Object.assign(stationForm, { code: '', name: '', spell: '', region: '', regionName: '' })
const openRegionDialog = (row) => { regionEditingId.value = row?.id || null; resetRegion(); if (row) Object.assign(regionForm, row); regionDialogVisible.value = true }
const openStationDialog = (row) => { stationEditingId.value = row?.id || null; resetStation(); if (row) Object.assign(stationForm, row); stationDialogVisible.value = true }
const syncRegionName = () => { stationForm.regionName = regions.value.find(region => region.code === stationForm.region)?.name || '' }
const saveRegion = async () => {
  if (!regionForm.name || !regionForm.code) return ElMessage.warning('请填写地区名称和编码')
  saving.value = true
  try { if (regionEditingId.value) await updateAdminRegion(regionEditingId.value, regionForm); else await createAdminRegion(regionForm); ElMessage.success('地区已保存'); regionDialogVisible.value = false; await loadRegions(); await loadStations() } finally { saving.value = false }
}
const saveStation = async () => {
  if (!stationForm.name || !stationForm.code) return ElMessage.warning('请填写车站名称和编码')
  saving.value = true
  try { if (stationEditingId.value) await updateAdminStation(stationEditingId.value, stationForm); else await createAdminStation(stationForm); ElMessage.success('车站已保存'); stationDialogVisible.value = false; await loadStations() } finally { saving.value = false }
}
const removeRegion = async (row) => { await ElMessageBox.confirm(`确定删除地区“${row.name}”吗？有车站使用时会被拒绝。`, '确认', { type: 'warning' }); await deleteAdminRegion(row.id); ElMessage.success('地区已删除'); await loadRegions() }
const removeStation = async (row) => { await ElMessageBox.confirm(`确定删除车站“${row.name}”吗？有车次使用时会被拒绝。`, '确认', { type: 'warning' }); await deleteAdminStation(row.id); ElMessage.success('车站已删除'); await loadStations() }

onMounted(async () => { await loadRegions(); await loadStations() })
</script>

<style scoped>
.admin-stations-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.toolbar .el-input { width: 300px; }
</style>
