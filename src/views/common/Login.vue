<template>
  <div class="login-container">
    <div class="language-switcher-wrapper">
      <LanguageSwitcher />
    </div>
    <div class="login-form">
      <h1>{{ $t("login.welcome") }}</h1>
      <h3>
        {{ isAdminMode ? $t("login.adminLogin") : $t("login.userLogin") }}
      </h3>
      <a-form
        :model="formState"
        name="loginForm"
        @finish="handleFinish"
        autocomplete="off"
      >
        <a-form-item
          name="username"
          :rules="[
            { required: true, message: $t('login.pleaseInputUsername') },
          ]"
        >
          <a-input
            v-model:value="formState.username"
            :placeholder="$t('login.pleaseInputUsername')"
            size="large"
          />
        </a-form-item>

        <a-form-item
          name="password"
          :rules="[
            { required: true, message: $t('login.pleaseInputPassword') },
          ]"
        >
          <a-input-password
            v-model:value="formState.password"
            :placeholder="$t('login.pleaseInputPassword')"
            size="large"
          />
        </a-form-item>

        <a-form-item name="remember">
          <a-checkbox v-model:checked="formState.remember">{{
            $t("login.rememberMe")
          }}</a-checkbox>
          <a class="login-form-forgot" @click="handleForgotPassword">
            {{ $t("login.forgotPassword") }}
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
            {{ $t("login.loginButton") }}
          </a-button>
        </a-form-item>

        <div class="login-mode-switch">
          <a @click="toggleLoginMode">
            {{
              isAdminMode ? $t("login.switchToUser") : $t("login.switchToAdmin")
            }}
          </a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { adminLogin, checkSession, login } from "@/api/auth";
import { message } from "ant-design-vue";
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import LanguageSwitcher from "../../components/LanguageSwitcher.vue";

const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const isLoading = ref(false);
const isAdminMode = ref(false);

const formState = reactive({
  username: "",
  password: "",
  remember: true,
});

// 检查会话状态
const checkLoginStatus = async () => {
  try {
    const isLoggedIn = await checkSession();
    if (isLoggedIn) {
      message.success(t("login.alreadyLoggedIn"));

      // 根据登录前的路径或角色决定跳转位置
      const redirectPath = route.query.redirect || "/";
      router.push(redirectPath);
    }
  } catch (error) {
    console.error("Session check failed:", error);
  }
};

// 加载记住的账号
const loadRememberedAccount = () => {
  const rememberedUsername = localStorage.getItem("rememberedUsername");
  const rememberedPassword = localStorage.getItem("rememberedPassword");

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
  isAdminMode.value = route.query.mode === "admin";
  // 加载已记住的账号
  loadRememberedAccount();
  // 检查登录状态
  checkLoginStatus();
});

const handleFinish = async () => {
  if (!formState.username || !formState.password) {
    message.error(
      t("login.pleaseInputUsername") + t("login.pleaseInputPassword")
    );
    return;
  }

  try {
    isLoading.value = true;

    // 登录数据
    const loginData = {
      loginName: formState.username,
      password: formState.password,
    };

    // 使用登录API
    const loginFn = isAdminMode.value ? adminLogin : login;
    const response = await loginFn(loginData);

    // 保存token - 根据文档LoginResponseDto结构
    if (response.data && response.data.token) {
      localStorage.setItem("token", response.data.token);
      localStorage.setItem(
        "userRole",
        response.data.role === 1 ? "admin" : "user"
      );
      localStorage.setItem("userId", response.data.userId);
      localStorage.setItem("userName", response.data.userName);

      // 如果用户选择"记住我"，保存账号和密码
      if (formState.remember) {
        localStorage.setItem("rememberedUsername", formState.username);
        localStorage.setItem("rememberedPassword", formState.password);
      } else {
        localStorage.removeItem("rememberedUsername");
        localStorage.removeItem("rememberedPassword");
      }

      message.success(t("login.loginSuccess"));

      // 根据用户角色决定跳转位置
      const redirectPath = response.data.role === 1 ? "/admin" : "/user";
      router.push(redirectPath);
    } else {
      throw new Error("登录响应缺少必要信息");
    }
  } catch (error) {
    console.error("登录失败:", error);

    // 获取并显示详细错误信息
    let errorMsg = t("login.loginFailed");

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
  message.info(t("login.contactAdmin"));
};

const toggleLoginMode = () => {
  isAdminMode.value = !isAdminMode.value;
  // 更新URL以反映登录模式
  const query = { ...route.query };
  if (isAdminMode.value) {
    query.mode = "admin";
  } else {
    delete query.mode;
  }
  router.replace({ query });
};
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.language-switcher-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.login-form {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.login-form h1 {
  text-align: center;
  margin-bottom: 16px;
  color: #333;
  font-size: 28px;
  font-weight: 600;
}

.login-form h3 {
  text-align: center;
  margin-bottom: 32px;
  color: #666;
  font-size: 16px;
  font-weight: 400;
}

.login-form-button {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
}

.login-form-forgot {
  float: right;
  color: var(--primary-color);
  text-decoration: none;
}

.login-form-forgot:hover {
  text-decoration: underline;
}

.login-mode-switch {
  text-align: center;
  margin-top: 16px;
}

.login-mode-switch a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
}

.login-mode-switch a:hover {
  text-decoration: underline;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-input-affix-wrapper),
:deep(.ant-input) {
  height: 44px;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  font-size: 14px;
}

:deep(.ant-input) {
  padding: 4px 11px;
  line-height: 36px;
}

:deep(.ant-input-affix-wrapper) {
  padding: 4px 11px;
  line-height: 36px;
}

:deep(.ant-input-affix-wrapper .ant-input) {
  padding: 0;
  border: none;
  outline: none;
  box-shadow: none;
  background: transparent;
  line-height: 36px;
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

:deep(.ant-checkbox-wrapper) {
  font-size: 14px;
  color: #666;
}
</style>
