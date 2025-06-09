import { createRouter, createWebHistory } from "vue-router";
import i18n from "../i18n";

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
          meta: { titleKey: "pageTitle.templateManagement" },
        },
        {
          path: "template",
          name: "template",
          component: () => import("../views/admin/Template.vue"),
          meta: { titleKey: "template.title" },
        },
        {
          path: "user-management",
          name: "user-management",
          component: () => import("../views/admin/UserManagement.vue"),
          meta: { titleKey: "pageTitle.userManagement" },
        },
        {
          path: "task-board",
          name: "task-board",
          component: () => import("../views/admin/TaskBoard.vue"),
          meta: { titleKey: "pageTitle.taskBoard" },
        },
        {
          path: "application-management",
          name: "application-management",
          component: () => import("../views/admin/ApplicationManagement.vue"),
          meta: { titleKey: "pageTitle.applicationManagement" },
        },
        {
          path: "content-review",
          name: "content-review",
          component: () => import("../views/admin/ContentReview.vue"),
          meta: { titleKey: "application.review" },
        },
        {
          path: "application-review",
          name: "application-review",
          component: () => import("../views/admin/ApplicationReview.vue"),
          meta: { titleKey: "application.review" },
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
          meta: { titleKey: "application.title" },
        },
        {
          path: "filing-center",
          name: "filing-center",
          component: () => import("../views/user/FilingCenter.vue"),
          meta: { titleKey: "application.title" },
        },
        {
          path: "filing-edit",
          name: "filing-edit",
          component: () => import("../views/user/FilingEdit.vue"),
          meta: { titleKey: "application.title" },
        },
      ],
    },
    // 登录路由
    {
      path: "/login",
      name: "login",
      component: () => import("../views/common/Login.vue"),
      meta: { titleKey: "pageTitle.login" },
    },
    // 404页面
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: () => import("../views/common/NotFound.vue"),
      meta: { titleKey: "pageTitle.notFound" },
    },
  ],
});

// 路由守卫，可以在这里添加权限控制等功能
router.beforeEach((to, from, next) => {
  // 设置页面标题
  const { t } = i18n.global;
  const pageTitle = to.meta.titleKey ? t(to.meta.titleKey) : t("system.title");
  document.title = to.meta.titleKey
    ? `${pageTitle} - ${t("system.title")}`
    : t("system.title");

  // 获取用户信息和token
  const token = localStorage.getItem("token");
  const userRole = localStorage.getItem("userRole");

  // 如果访问登录页面，直接放行
  if (to.path === "/login") {
    // 如果已登录且在访问登录页，重定向到对应的首页
    if (token) {
      return next(userRole === "admin" ? "/admin" : "/user");
    }
    return next();
  }

  // 需要登录的页面
  if (!token) {
    // 未登录，重定向到登录页
    return next("/login");
  }

  // 根据用户角色进行页面权限控制
  if (to.path.startsWith("/admin")) {
    // 管理员页面，检查是否为管理员
    if (userRole !== "admin") {
      // 非管理员用户尝试访问管理员页面，重定向到用户首页
      return next("/user");
    }
  } else if (to.path.startsWith("/user")) {
    // 用户页面，无需特殊检查
    // 但如果管理员访问用户页面，也允许
  }

  // 默认放行
  next();
});

export default router;
