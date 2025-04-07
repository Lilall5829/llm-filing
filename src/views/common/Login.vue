<template>
  <div class="login-container">
    <div class="login-form">
      <h1>大模型备案信息填报系统</h1>
      <div class="content-placeholder">
        <!-- 这里是登录表单的内容 -->
      </div>
    </div>
  </div>
</template>

<script setup>
// 组件逻辑将在这里实现
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
  background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNDQiIGhlaWdodD0iMTQ0IiB2aWV3Qm94PSIwIDAgMTQ0IDE0NCI+PHBhdGggZmlsbD0iI2U5ZTllOSIgZD0iTTcyIDcyTDAgNzIgMCAwIDcyIDB6Ii8+PHBhdGggZmlsbD0iI2Y1ZjVmNSIgZD0iTTE0NCAxNDRMNzIgMTQ0IDcyIDcyIDE0NCA3MnoiLz48L3N2Zz4=');
  background-repeat: repeat;
}

.login-form {
  width: 400px;
  padding: 40px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  text-align: center;
}

h1 {
  margin-bottom: 40px;
  font-size: 24px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.content-placeholder {
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  background-color: #fafafa;
}
</style> 