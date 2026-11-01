<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="logo">My12306 铁路售票系统</div>
      <el-menu mode="horizontal" :default-active="activeMenu" class="menu" router>
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/tickets">车票查询</el-menu-item>
        <el-menu-item index="/passengers">乘车人</el-menu-item>
        <el-menu-item index="/orders">我的订单</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/users">用户管理</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/admin/stations">地区车站</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/admin/trains">车次模板</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/admin/schedules">运行计划</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/admin/orders">订单管理</el-menu-item>
      </el-menu>
      <div class="user-info">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-icon><User /></el-icon>
            {{ userStore.userInfo?.username }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" @closed="resetPasswordForm">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordChange">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        callback(value === passwordForm.newPassword ? undefined : new Error('两次输入的新密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  if (!userStore.userInfo) userStore.getUserInfo()
})

const handleCommand = (command) => {
  if (command === 'password') passwordDialogVisible.value = true
  if (command === 'logout') userStore.logout()
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const submitPasswordChange = async () => {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  passwordSubmitting.value = true
  try {
    await changePassword({ ...passwordForm })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
  } finally {
    passwordSubmitting.value = false
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.header { display: flex; align-items: center; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,.08); padding: 0 20px; }
.logo { font-size: 20px; font-weight: bold; color: #409eff; margin-right: 50px; }
.menu { flex: 1; border: none; }
.user-info { margin-left: 20px; }
.el-dropdown-link { cursor: pointer; display: flex; align-items: center; color: #606266; }
</style>
