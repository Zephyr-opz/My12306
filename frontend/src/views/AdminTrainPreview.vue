<template>
  <div class="preview-page" v-loading="loading">
    <el-card v-if="preview">
      <template #header>
        <div class="preview-header">
          <span>车次模板预览</span>
          <el-tag type="warning">尚未保存</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="preview-tip" title="以下时间和区间票价由系统试算生成，点击“确认保存”后才会写入数据库；点击“返回修改”可继续调整。" />

      <el-descriptions :column="3" border>
        <el-descriptions-item label="车次号">{{ preview.trainNumber }}</el-descriptions-item>
        <el-descriptions-item label="始发站">{{ preview.startStation }}</el-descriptions-item>
        <el-descriptions-item label="终点站">{{ preview.endStation }}</el-descriptions-item>
        <el-descriptions-item label="始发时间">{{ formatDateTime(preview.departureTime) }}</el-descriptions-item>
        <el-descriptions-item label="终到时间">{{ formatDateTime(preview.arrivalTime) }}</el-descriptions-item>
        <el-descriptions-item label="全程时长">{{ preview.totalMinutes }} 分钟</el-descriptions-item>
        <el-descriptions-item label="运行日期">{{ preview.runStartDate }} 至 {{ preview.runEndDate }}</el-descriptions-item>
        <el-descriptions-item label="始发地区">{{ preview.startRegion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="终点地区">{{ preview.endRegion || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">系统计算的经停站时间</el-divider>
      <el-table :data="preview.stops" border stripe>
        <el-table-column prop="sequence" label="站序" width="90" />
        <el-table-column label="站点" min-width="150"><template #default="{ row, $index }"><el-tag v-if="$index === 0" type="success">始发站</el-tag><el-tag v-else-if="$index === preview.stops.length - 1" type="warning">终点站</el-tag> {{ row.departure }}</template></el-table-column>
        <el-table-column label="到站时间" width="180"><template #default="{ row }">{{ formatDateTime(row.arrivalTime) }}</template></el-table-column>
        <el-table-column label="发车时间" width="180"><template #default="{ row }">{{ row.departureTime ? formatDateTime(row.departureTime) : '—' }}</template></el-table-column>
        <el-table-column label="停留分钟" width="120"><template #default="{ row }">{{ row.stopoverTime || 0 }}</template></el-table-column>
      </el-table>

      <el-divider content-position="left">全程票价</el-divider>
      <el-table :data="preview.fullFares" border stripe>
        <el-table-column label="席别" width="130"><template #default="{ row }">{{ seatLabel(row.seatType) }}</template></el-table-column>
        <el-table-column label="全程票价"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
      </el-table>

      <el-divider content-position="left">系统生成的区间票价</el-divider>
      <el-table :data="preview.intervalFares" border stripe max-height="420">
        <el-table-column prop="departure" label="出发站" min-width="130" />
        <el-table-column prop="arrival" label="到达站" min-width="130" />
        <el-table-column label="席别" width="130"><template #default="{ row }">{{ seatLabel(row.seatType) }}</template></el-table-column>
        <el-table-column label="区间票价"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
      </el-table>

      <el-divider content-position="left">车厢容量</el-divider>
      <el-table :data="preview.carriages" border stripe>
        <el-table-column prop="carriageNumber" label="车厢号" />
        <el-table-column label="席别"><template #default="{ row }">{{ seatLabel(row.carriageType) }}</template></el-table-column>
        <el-table-column prop="seatCount" label="座位数" />
      </el-table>

      <div class="preview-actions">
        <el-button @click="backToEdit">返回修改</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSave">确认保存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAdminTrain, updateAdminTrain } from '@/api/railway'

const router = useRouter()
const preview = ref(null)
const draft = ref(null)
const loading = ref(true)
const saving = ref(false)
const previewStorageKey = 'my12306.admin.train.preview'

const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
const money = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const seatLabel = (value) => ({ 0: '商务座', 1: '一等座', 2: '二等座' }[Number(value)] || '其他席别')

const loadPreview = () => {
  const raw = sessionStorage.getItem(previewStorageKey)
  if (!raw) {
    ElMessage.error('预览数据已失效，请重新填写车次模板')
    router.replace({ name: 'AdminTrains' })
    return
  }
  try {
    draft.value = JSON.parse(raw)
    preview.value = draft.value.preview
  } catch (error) {
    sessionStorage.removeItem(previewStorageKey)
    ElMessage.error('预览数据读取失败，请重新填写车次模板')
    router.replace({ name: 'AdminTrains' })
  } finally {
    loading.value = false
  }
}

const backToEdit = () => router.back()

const confirmSave = async () => {
  if (!draft.value?.payload) return
  saving.value = true
  try {
    const id = draft.value.editingId || draft.value.payload.id
    if (id) await updateAdminTrain(id, draft.value.payload)
    else await createAdminTrain(draft.value.payload)
    sessionStorage.removeItem(previewStorageKey)
    ElMessage.success(id ? '车次模板已修改' : '车次模板已保存')
    await router.replace({ name: 'AdminTrains' })
  } finally {
    saving.value = false
  }
}

onMounted(loadPreview)
</script>

<style scoped>
.preview-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.preview-header { display: flex; justify-content: space-between; align-items: center; }
.preview-tip { margin-bottom: 18px; }
.preview-page .el-divider { margin: 26px 0 16px; }
.preview-actions { display: flex; justify-content: center; gap: 12px; margin-top: 28px; }
</style>

