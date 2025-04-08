<template>
  <a-layout class="admin-layout">
    <a-layout-sider
      width="256"
      theme="dark"
      class="sidebar"
    >
      <div class="logo">
        <h1>大模型备案信息填报系统</h1>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
      >
        <a-menu-item key="template-management" @click="$router.push({ name: 'template-management' })">
          <template #icon>
            <FileOutlined />
          </template>
          <span>模板管理</span>
        </a-menu-item>
        <a-menu-item key="user-management" @click="$router.push({ name: 'user-management' })">
          <template #icon>
            <UserOutlined />
          </template>
          <span>用户管理</span>
        </a-menu-item>
        <a-menu-item key="task-board" @click="$router.push({ name: 'task-board' })">
          <template #icon>
            <DashboardOutlined />
          </template>
          <span>任务看板</span>
        </a-menu-item>
        <a-menu-item key="application-management" @click="$router.push({ name: 'application-management' })">
          <template #icon>
            <KeyOutlined />
          </template>
          <span>申请管理</span>
        </a-menu-item>
        <a-menu-item key="logout" @click="logout">
          <template #icon>
            <LogoutOutlined />
          </template>
          <span>退出登录</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <div class="current-user">
          <a-space>
            <UserOutlined />
            <span>您好，管理员</span>
          </a-space>
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
UserOutlined
} from '@ant-design/icons-vue';
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();
const selectedKeys = ref([route.name]);

watch(() => route.name, (newName) => {
  selectedKeys.value = [newName];
});

const logout = () => {
  // 这里可以添加登出逻辑
  router.push('/login');
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
  padding: 16px;
  border-bottom: 1px solid #002140;
}

.logo h1 {
  font-size: 20px;
  color: white;
  margin-bottom: 4px;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.content {
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style> 