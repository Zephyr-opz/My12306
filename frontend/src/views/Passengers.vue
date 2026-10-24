<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header"><span>乘车人管理</span><el-button type="primary" @click="openAdd">添加乘车人</el-button></div>
      </template>
      <el-table v-loading="loading" :data="passengers" stripe>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="idCard" label="证件号" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="discountType" label="票种">
          <template #default="scope">{{ discountLabels[scope.row.discountType] || '成人票' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑乘车人' : '添加乘车人'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="证件类型"><el-select v-model="form.idType"><el-option label="身份证" :value="0" /><el-option label="护照" :value="1" /></el-select></el-form-item>
        <el-form-item label="证件号码"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="票种"><el-select v-model="form.discountType"><el-option v-for="(label, value) in discountLabels" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchPassengerList, fetchAddPassenger, fetchEditPassenger, fetchDeletePassenger } from '@/api/railway'

const userStore = useUserStore()
const passengers = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const discountLabels = { 0: '成人票', 1: '儿童票', 2: '学生票', 3: '残军票' }
const form = reactive({ id: null, realName: '', idType: 0, idCard: '', discountType: 0, phone: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await fetchPassengerList({ username: userStore.userInfo?.username })
    passengers.value = res.data || []
  } finally { loading.value = false }
}
const reset = () => Object.assign(form, { id: null, realName: '', idType: 0, idCard: '', discountType: 0, phone: '' })
const openAdd = () => { reset(); editing.value = false; dialogVisible.value = true }
const openEdit = (row) => { Object.assign(form, row); editing.value = true; dialogVisible.value = true }
const save = async () => {
  if (!form.realName || !form.idCard) return ElMessage.warning('请填写姓名和证件号码')
  saving.value = true
  try { editing.value ? await fetchEditPassenger(form) : await fetchAddPassenger(form); dialogVisible.value = false; ElMessage.success('保存成功'); await load() }
  finally { saving.value = false }
}
const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除乘车人“${row.realName}”？`, '提示', { type: 'warning' })
  await fetchDeletePassenger({ id: row.id }); ElMessage.success('已删除'); await load()
}
onMounted(load)
</script>

<style scoped>
.page-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
