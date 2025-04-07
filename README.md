# 大模型备案信息填报系统

基于 Vue 3 的大模型备案信息填报系统，包括管理端和用户端。

## 项目架构

- 管理端

  - 模板管理
  - 用户管理
  - 任务看板
  - 修改密码
  - 退出登录

- 用户端
  - 备案申请
  - 备案中心
  - 修改密码
  - 退出登录

## 项目设置

```sh
npm install
```

### 开发环境

```sh
npm run dev
```

### 生产环境

```sh
npm run build
```

## 项目结构

```
src/
├── assets/          # 静态资源
├── views/           # 页面组件
│   ├── admin/       # 管理端页面
│   ├── user/        # 用户端页面
│   └── common/      # 公共页面
├── router/          # 路由配置
├── App.vue          # 根组件
└── main.js          # 入口文件
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
