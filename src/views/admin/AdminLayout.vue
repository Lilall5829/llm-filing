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
        <a-menu-item key="logout" @click="handleLogout">
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
            <span>您好，{{ adminName }}</span>
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
import { message } from 'ant-design-vue';
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { checkSession, logout } from '../../api/auth';

const router = useRouter();
const route = useRoute();
const selectedKeys = ref([route.name]);
const adminName = ref('管理员');

watch(() => route.name, (newName) => {
  selectedKeys.value = [newName];
});

// 检查用户登录状态并获取用户信息
onMounted(async () => {
  try {
    const isLoggedIn = await checkSession();
    if (!isLoggedIn) {
      message.error('您的登录已过期，请重新登录');
      router.push('/login');
      return;
    }
    
    // 从localStorage获取用户信息
    const userName = localStorage.getItem('userName');
    if (userName) {
      adminName.value = userName;
    }
  } catch (error) {
    console.error('会话验证失败:', error);
  }
});

// 退出登录函数
const handleLogout = async () => {
  try {
    // 调用登出函数 - 目前只在前端清除token
    await logout();
    message.success('退出登录成功');
    router.push('/login');
  } catch (error) {
    console.error('退出登录失败:', error);
    message.error('退出登录失败，请重试');
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