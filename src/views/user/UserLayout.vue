<template>
  <div class="user-layout">
    <div class="sidebar">
      <div class="logo">
        <h1>{{ $t("system.title") }}</h1>
      </div>
      <div class="menu">
        <div
          class="menu-item"
          :class="{ active: $route.name === 'filing-application' }"
          @click="$router.push({ name: 'filing-application' })"
        >
          <span>{{ $t("userMenu.filingApplication") }}</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: $route.name === 'filing-center' }"
          @click="$router.push({ name: 'filing-center' })"
        >
          <span>{{ $t("userMenu.filingCenter") }}</span>
        </div>
        <div class="menu-item" @click="handleLogout">
          <span>{{ $t("userMenu.logout") }}</span>
        </div>
      </div>
    </div>
    <div class="main-content">
      <div class="header">
        <div class="current-user">
          <span>{{ $t("user.hello", { name: userName }) }}</span>
        </div>
        <div class="header-actions">
          <LanguageSwitcher />
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { logout } from "@/api/auth";
import { message } from "ant-design-vue";
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import LanguageSwitcher from "../../components/LanguageSwitcher.vue";

const router = useRouter();
const { t } = useI18n();
const userName = ref(t("user.admin"));

onMounted(() => {
  // 获取用户名
  const storedUserName = localStorage.getItem("userName");
  if (storedUserName) {
    userName.value = storedUserName;
  }
});

const handleLogout = async () => {
  try {
    await logout();
    message.success(t("user.logoutSuccess"));
    router.push("/login");
  } catch (error) {
    console.error("退出登录失败:", error);
    message.error(t("user.logoutFailed"));
  }
};
</script>

<style scoped>
.user-layout {
  display: flex;
  height: 100%;
}

.sidebar {
  width: var(--sidebar-width);
  background-color: #001529;
  color: white;
  height: 100%;
  box-shadow: 2px 0 8px 0 rgba(29, 35, 41, 0.05);
  display: flex;
  flex-direction: column;
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

.logo p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.menu {
  flex: 1;
  padding: 16px 0;
}

.menu-item {
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
  margin: 4px 0;
  cursor: pointer;
  transition: all 0.3s;
}

.menu-item:hover {
  background-color: #1890ff;
}

.menu-item.active {
  background-color: #1890ff;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: var(--header-height);
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.current-user {
  margin-right: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
