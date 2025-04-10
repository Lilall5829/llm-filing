import request from "./auth";

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
export function resetPassword(id, data) {
  return request({
    url: `/api/users/${id}/reset-password`,
    method: "put",
    data,
  });
}
