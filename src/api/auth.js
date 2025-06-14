import axios from "axios";

// 远程服务器URL（已注释）
// const baseURL = "";

// 本地后端URL
const baseURL = "http://localhost:8080";

// 创建axios实例
const request = axios.create({
  baseURL,
  timeout: 5000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在请求发送前，添加认证令牌
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    console.error("请求拦截器错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const errorMsg = error.response.data?.message || "请求失败";

      // 根据状态码处理错误
      switch (status) {
        case 401:
          console.warn("用户未授权或会话已过期，需要重新登录");
          // 清除本地存储的认证信息
          localStorage.removeItem("token");
          localStorage.removeItem("userRole");
          localStorage.removeItem("userId");
          localStorage.removeItem("userName");

          // 如果不在登录页面，则跳转到登录页面
          if (window.location.pathname !== "/login") {
            window.location.href = "/login";
          }
          break;
        case 403:
          console.warn("权限不足，无法执行操作");
          break;
        case 404:
          console.warn("请求的资源不存在");
          break;
        case 500:
          console.error("服务器内部错误");
          break;
      }
    }

    return Promise.reject(error);
  }
);

// 检查用户会话状态 - 通过验证token本地存在且能获取用户列表来确认
export const checkSession = async () => {
  // 首先检查本地是否存在token
  const token = localStorage.getItem("token");
  if (!token) {
    return false;
  }

  try {
    // 尝试获取用户列表的第一页数据，只是验证token是否有效
    await getUserList({ pageNum: 1, pageSize: 1 });
    return true;
  } catch (error) {
    console.error("Session validation failed:", error);
    // 如果请求失败（如401错误），清除无效token
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("token");
    }
    return false;
  }
};

// 用户登录
export function login(data) {
  return request({
    url: "/api/auth/login",
    method: "post",
    data,
  });
}

// 管理员登录
export function adminLogin(data) {
  return request({
    url: "/api/auth/admin/login",
    method: "post",
    data,
  });
}

// 用户注册
export function register(data) {
  return request({
    url: "/api/auth/register",
    method: "post",
    data,
  });
}

// 获取用户列表
export function getUserList(params) {
  return request({
    url: "/api/users/page",
    method: "get",
    params,
  });
}

// 更新用户信息
export function updateUser(id, data) {
  return request({
    url: `/api/users/${id}`,
    method: "put",
    data,
  });
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: "delete",
  });
}

// 重置用户密码
export function resetPassword(id, newPassword) {
  return request({
    url: `/api/users/${id}/reset-password`,
    method: "put",
    data: { newPassword },
  });
}

// 客户端登出处理（只在前端清除token，不调用后端）
export const logout = () => {
  // 清除所有与用户会话相关的存储
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  localStorage.removeItem("userId");
  localStorage.removeItem("userName");
  // 不清除记住的用户名，以便用户下次登录时使用
  // localStorage.removeItem('rememberedUsername');

  // 清除可能存在的会话cookie
  document.cookie.split(";").forEach(function (c) {
    document.cookie = c
      .replace(/^ +/, "")
      .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
  });

  // 模拟成功响应
  return Promise.resolve({ code: 200, message: "登出成功" });
};

export default request;
