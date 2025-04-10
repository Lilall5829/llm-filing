<template>
  <div class="login-container">
    <div class="login-form">
      <h1>欢迎登录</h1>
      <h3>{{ isAdminMode ? '管理员登录' : '用户登录' }}</h3>
      <a-form
        :model="formState"
        name="loginForm"
        @finish="handleFinish"
        autocomplete="off"
      >
        <a-form-item
          name="username"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input 
            v-model:value="formState.username" 
            placeholder="请输入账号" 
            size="large"
          />
        </a-form-item>

        <a-form-item
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item name="remember">
          <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
          <a class="login-form-forgot" @click="handleForgotPassword">
            忘记密码？
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

        <div class="login-mode-switch">
          <a @click="toggleLoginMode">
            {{ isAdminMode ? '普通用户登录' : '管理员登录' }}
          </a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { adminLogin, checkSession, login } from '@/api/auth';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();
const isLoading = ref(false);
const isAdminMode = ref(false);

const formState = reactive({
  username: '',
  password: '',
  remember: true,
});

// 检查会话状态
const checkLoginStatus = async () => {
  try {
    const isLoggedIn = await checkSession();
    if (isLoggedIn) {
      message.success('检测到已登录状态，正在跳转...');
      
      // 根据登录前的路径或角色决定跳转位置
      const redirectPath = route.query.redirect || '/';
      router.push(redirectPath);
    }
  } catch (error) {
    console.error('Session check failed:', error);
  }
};

// 加载记住的账号
const loadRememberedAccount = () => {
  const rememberedUsername = localStorage.getItem('rememberedUsername');
  const rememberedPassword = localStorage.getItem('rememberedPassword');
  
  if (rememberedUsername) {
    formState.username = rememberedUsername;
  }
  
  if (rememberedPassword) {
    formState.password = rememberedPassword;
    formState.remember = true;
  }
};

// 组件挂载时检查登录状态和模式
onMounted(() => {
  // 检查是否为管理员登录模式
  isAdminMode.value = route.query.mode === 'admin';
  // 加载已记住的账号
  loadRememberedAccount();
  // 检查登录状态
  checkLoginStatus();
});

const handleFinish = async () => {
  if (!formState.username || !formState.password) {
    message.error('请输入账号和密码');
    return;
  }

  try {
    isLoading.value = true;
    
    // 登录数据
    const loginData = {
      loginName: formState.username,
      password: formState.password
    };
    
    console.log("登录数据:", loginData);
    console.log("登录模式:", isAdminMode.value ? "管理员登录" : "用户登录");
    
    // 使用登录API
    const loginFn = isAdminMode.value ? adminLogin : login;
    const response = await loginFn(loginData);
    
    console.log("登录响应:", response);
    
    // 保存token - 根据文档LoginResponseDto结构
    if (response.data && response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userRole', response.data.role === 1 ? 'admin' : 'user');
      localStorage.setItem('userId', response.data.userId);
      localStorage.setItem('userName', response.data.userName);
      
      // 如果用户选择"记住我"，保存账号和密码
      if (formState.remember) {
        localStorage.setItem('rememberedUsername', formState.username);
        localStorage.setItem('rememberedPassword', formState.password);
      } else {
        localStorage.removeItem('rememberedUsername');
        localStorage.removeItem('rememberedPassword');
      }
      
      message.success('登录成功');
      
      // 根据用户角色决定跳转位置
      const redirectPath = response.data.role === 1 ? '/admin' : '/user';
      router.push(redirectPath);
    } else {
      throw new Error('登录响应缺少必要信息');
    }
  } catch (error) {
    console.error('登录失败:', error);
    
    // 获取并显示详细错误信息
    let errorMsg = '登录失败，请检查账号和密码是否正确';
    
    if (error.response && error.response.data) {
      errorMsg = error.response.data.message || errorMsg;
    } else if (error.message) {
      errorMsg = error.message;
    }
    
    message.error(errorMsg);
  } finally {
    isLoading.value = false;
  }
};

const handleForgotPassword = () => {
  message.info('请联系管理员重置密码');
};

const toggleLoginMode = () => {
  isAdminMode.value = !isAdminMode.value;
  // 更新URL以反映登录模式
  const query = { ...route.query };
  if (isAdminMode.value) {
    query.mode = 'admin';
  } else {
    delete query.mode;
  }
  router.replace({ path: route.path, query });
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

.login-mode-switch {
  margin-top: 16px;
  text-align: center;
}

.login-mode-switch a {
  color: #1890ff;
  cursor: pointer;
  text-decoration: underline;
}
</style> 