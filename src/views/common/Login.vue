<template>
  <div class="login-container">
    <div class="login-form">
      <h1>欢迎登录</h1>
      <h3>账号密码登录</h3>
      <a-form
        :model="formState"
        name="loginForm"
        @finish="handleFinish"
        autocomplete="off"
      >
        <a-form-item
          name="phone"
          :rules="[{ required: true, message: '请输入手机号' }]"
        >
          <a-input v-model:value="formState.phone" placeholder="请输入手机号" size="large" />
        </a-form-item>

        <a-form-item
          name="verificationCode"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <a-row :gutter="8">
            <a-col :span="16">
              <a-input
                v-model:value="formState.verificationCode"
                placeholder="请输入验证码"
                size="large"
              />
            </a-col>
            <a-col :span="8">
              <a-button 
                size="large" 
                @click="getVerificationCode" 
                :loading="isGettingCode"
                :disabled="isGettingCode"
              >
                {{ codeButtonText }}
              </a-button>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item name="remember">
          <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
          <a class="login-form-forgot" @click="handleForgotCode">
            收不到验证码？
          </a>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            class="login-form-button"
            size="large"
            block
            :loading="isLoading"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { checkSession, getVerificationCode as getCode, login } from '@/api/auth';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const isLoading = ref(false);
const isGettingCode = ref(false);
const codeButtonText = ref('获取验证码');
let codeCountdown = null;

const formState = reactive({
  phone: '',
  verificationCode: '',
  remember: true,
});

// 检查会话状态
const checkLoginStatus = async () => {
  try {
    const isLoggedIn = await checkSession();
    if (isLoggedIn) {
      message.success('检测到已登录状态，正在跳转...');
      router.push('/');
    }
  } catch (error) {
    console.error('Session check failed:', error);
  }
};

// 组件挂载时检查登录状态
onMounted(() => {
  checkLoginStatus();
});

const startCodeCountdown = () => {
  let count = 60;
  codeButtonText.value = `${count}秒后重试`;
  codeCountdown = setInterval(() => {
    count -= 1;
    if (count <= 0) {
      clearInterval(codeCountdown);
      codeButtonText.value = '获取验证码';
      isGettingCode.value = false;
    } else {
      codeButtonText.value = `${count}秒后重试`;
    }
  }, 1000);
};

const getVerificationCode = async () => {
  if (!formState.phone) {
    message.error('请先输入手机号');
    return;
  }

  try {
    isGettingCode.value = true;
    await getCode(formState.phone);
    message.success('验证码已发送');
    startCodeCountdown();
  } catch (error) {
    message.error('获取验证码失败，请稍后重试');
    isGettingCode.value = false;
  }
};

const handleFinish = async () => {
  try {
    isLoading.value = true;
    const result = await login(formState.phone, formState.verificationCode);
    message.success('登录成功');
    router.push('/');
  } catch (error) {
    message.error('登录失败，请检查手机号和验证码是否正确');
  } finally {
    isLoading.value = false;
  }
};

const handleForgotCode = () => {
  message.info('请联系管理员协助处理');
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
}

.login-form {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

h1 {
  margin-bottom: 8px;
  font-size: 28px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  text-align: center;
}

h3 {
  margin-bottom: 24px;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}

.login-form-forgot {
  float: right;
  color: #1890ff;
  cursor: pointer;
}

.login-form-button {
  margin-top: 16px;
}
</style> 