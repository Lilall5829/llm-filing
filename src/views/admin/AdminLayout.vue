<template>
  <a-layout class="admin-layout">
    <a-layout-sider width="256" theme="dark" class="sidebar">
      <div class="logo">
        <h1>{{ $t("system.title") }}</h1>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline">
        <a-menu-item
          key="template-management"
          @click="$router.push({ name: 'template-management' })"
        >
          <template #icon>
            <FileOutlined />
          </template>
          <span>{{ $t("menu.templateManagement") }}</span>
        </a-menu-item>
        <a-menu-item
          key="user-management"
          @click="$router.push({ name: 'user-management' })"
        >
          <template #icon>
            <UserOutlined />
          </template>
          <span>{{ $t("menu.userManagement") }}</span>
        </a-menu-item>
        <a-menu-item
          key="task-board"
          @click="$router.push({ name: 'task-board' })"
        >
          <template #icon>
            <DashboardOutlined />
          </template>
          <span>{{ $t("menu.taskBoard") }}</span>
        </a-menu-item>
        <a-menu-item
          key="application-management"
          @click="$router.push({ name: 'application-management' })"
        >
          <template #icon>
            <KeyOutlined />
          </template>
          <span>{{ $t("menu.applicationManagement") }}</span>
        </a-menu-item>
        <a-menu-item key="logout" @click="handleLogout">
          <template #icon>
            <LogoutOutlined />
          </template>
          <span>{{ $t("menu.logout") }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <div class="header-content">
          <div class="current-user">
            <a-space>
              <UserOutlined />
              <span>{{ $t("user.hello", { name: adminName }) }}</span>
            </a-space>
          </div>
          <div class="header-actions">
            <LanguageSwitcher />
          </div>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import {
DashboardOutlined,
FileOutlined,
KeyOutlined,
LogoutOutlined,
UserOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { checkSession, logout } from "../../api/auth";
import LanguageSwitcher from "../../components/LanguageSwitcher.vue";

const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const selectedKeys = ref([route.name]);
const adminName = ref(t("user.admin"));

watch(
  () => route.name,
  (newName) => {
    selectedKeys.value = [newName];
  }
);

// 检查用户登录状态并获取用户信息
onMounted(async () => {
  try {
    const isLoggedIn = await checkSession();
    if (!isLoggedIn) {
      message.error(t("login.sessionExpired"));
      router.push("/login");
      return;
    }

    // 从localStorage获取用户信息
    const userName = localStorage.getItem("userName");
    if (userName) {
      adminName.value = userName;
    }
  } catch (error) {
    console.error("会话验证失败:", error);
  }
});

// 退出登录
const handleLogout = async () => {
  try {
    await logout();
    message.success(t("adminLayout.logoutSuccess"));
    router.push("/login?mode=admin");
  } catch (error) {
    console.error("退出登录失败:", error);
    message.error(t("adminLayout.logoutFailed"));
  }
};
</script>

<style scoped>
.admin-layout {
  height: 100%;
}

.sidebar {
  box-shadow: 2px 0 8px 0 rgba(29, 35, 41, 0.05);
}

.logo {
  height: var(--header-height);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #002140;
}

.logo h1 {
  font-size: 14px;
  color: white;
  margin-bottom: 4px;
  text-align: center;
  line-height: 1.2;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.current-user {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style>
