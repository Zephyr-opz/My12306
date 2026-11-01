<template>
  <div class="forgot-password-container">
    <el-card class="forgot-password-card">
      <h2 class="title">忘记密码</h2>
      <p class="tip">验证码为测试验证码，请发送后查看后端控制台。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" maxlength="6" placeholder="6位数字验证码" />
            <el-button :disabled="countdown > 0" :loading="sending" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="6-20个字符" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" class="submit-button" @click="handleResetPassword">
            重置密码
          </el-button>
        </el-form-item>
        <div class="links">
          <router-link to="/login">返回登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { resetPassword, sendResetCode } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        callback(value === form.newPassword ? undefined : new Error('两次输入的新密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

const startCountdown = () => {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  try {
    await formRef.value.validateField('username')
  } catch {
    return
  }
  sending.value = true
  try {
    await sendResetCode({ username: form.username })
    ElMessage.success('验证码已发送，请查看后端控制台')
    startCountdown()
  } finally {
    sending.value = false
  }
}

const handleResetPassword = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await resetPassword({ ...form })
    ElMessage.success('密码重置成功，请使用新密码登录')
    await router.push('/login')
  } finally {
    submitting.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.forgot-password-card {
  width: 470px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 12px;
  color: #333;
}

.tip {
  margin: 0 0 22px;
  color: #909399;
  text-align: center;
  font-size: 13px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.submit-button {
  width: 100%;
}

.links {
  text-align: center;
  margin-top: 4px;
}

.links a {
  color: #409eff;
  text-decoration: none;
}
</style>
