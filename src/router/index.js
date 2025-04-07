import { createRouter, createWebHistory } from "vue-router";

// 管理端布局和页面
import AdminLayout from "../views/admin/AdminLayout.vue";
import TemplateManagement from "../views/admin/TemplateManagement.vue";

// 用户端布局和页面
import FilingApplication from "../views/user/FilingApplication.vue";
import UserLayout from "../views/user/UserLayout.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      redirect: "/admin",
    },
    // 管理端路由
    {
      path: "/admin",
      component: AdminLayout,
      children: [
        {
          path: "",
          name: "template-management",
          component: TemplateManagement,
          meta: { title: "模板管理" },
        },
        {
          path: "template",
          name: "template",
          component: () => import("../views/admin/Template.vue"),
          meta: { title: "模板" },
        },
        {
          path: "user-management",
          name: "user-management",
          component: () => import("../views/admin/UserManagement.vue"),
          meta: { title: "用户管理" },
        },
        {
          path: "task-board",
          name: "task-board",
          component: () => import("../views/admin/TaskBoard.vue"),
          meta: { title: "任务看板" },
        },
        {
          path: "change-password",
          name: "admin-change-password",
          component: () => import("../views/admin/ChangePassword.vue"),
          meta: { title: "修改密码" },
        },
      ],
    },
    // 用户端路由
    {
      path: "/user",
      component: UserLayout,
      children: [
        {
          path: "",
          name: "filing-application",
          component: FilingApplication,
          meta: { title: "备案申请" },
        },
        {
          path: "filing-center",
          name: "filing-center",
          component: () => import("../views/user/FilingCenter.vue"),
          meta: { title: "备案中心" },
        },
        {
          path: "change-password",
          name: "user-change-password",
          component: () => import("../views/user/ChangePassword.vue"),
          meta: { title: "修改密码" },
        },
      ],
    },
    // 登录路由
    {
      path: "/login",
      name: "login",
      component: () => import("../views/common/Login.vue"),
      meta: { title: "登录" },
    },
    // 404页面
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: () => import("../views/common/NotFound.vue"),
      meta: { title: "页面未找到" },
    },
  ],
});

// 路由守卫，可以在这里添加权限控制等功能
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - 大模型备案信息填报系统`
    : "大模型备案信息填报系统";
  next();
});

export default router;
