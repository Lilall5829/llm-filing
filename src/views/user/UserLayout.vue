<template>
  <div class="user-layout">
    <div class="sidebar">
      <div class="logo">
        <h1>大模型备案信息填报系统</h1>
      </div>
      <div class="menu">
        <div
          class="menu-item"
          :class="{ active: $route.name === 'filing-application' }"
          @click="$router.push({ name: 'filing-application' })"
        >
          <span>备案申请</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: $route.name === 'filing-center' }"
          @click="$router.push({ name: 'filing-center' })"
        >
          <span>备案中心</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: $route.name === 'user-change-password' }"
          @click="$router.push({ name: 'user-change-password' })"
        >
          <span>修改密码</span>
        </div>
        <div class="menu-item" @click="logout">
          <span>退出登录</span>
        </div>
      </div>
    </div>
    <div class="main-content">
      <div class="header">
        <div class="current-user">
          <span>您好，用户</span>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const logout = () => {
  // 这里可以添加登出逻辑
  router.push('/login');
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
  padding: 16px;
  border-bottom: 1px solid #002140;
}

.logo h1 {
  font-size: 20px;
  color: white;
  margin-bottom: 4px;
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
  justify-content: flex-end;
  padding: 0 24px;
}

.current-user {
  margin-right: 16px;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style> 