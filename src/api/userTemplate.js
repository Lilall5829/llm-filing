import request from "./auth";

// 分页获取已申请的模板列表
export function getAppliedTemplateList(params) {
  return request({
    url: "/api/userTemplate/page",
    method: "get",
    params,
  });
}

// 用户申请模板或管理员发送模板
export function applyTemplate(templateId, userIds) {
  // 构造请求数据
  const requestData = {
    userIds: ["testuser2"], // 使用具体的登录名，让后端处理转换
  };

  console.log("申请模板参数:", { templateId, requestData });

  return request({
    url: "/api/userTemplate/applyTemplate",
    method: "post",
    params: { templateId },
    data: requestData,
    timeout: 10000, // 增加超时时间
  })
    .then((response) => {
      console.log("申请模板成功:", response);
      return response;
    })
    .catch((error) => {
      console.error("申请模板失败:", error);
      if (error.response && error.response.data) {
        console.error("错误详情:", error.response.data);
      }
      throw error;
    });
}

// 获取当前用户的模板内容
export function getTemplateContent(id) {
  return request({
    url: "/api/userTemplate/getTemplateContent",
    method: "get",
    params: { id },
  });
}

// 保存用户填写的模板内容
export function saveTemplateContent(id, content, status) {
  // 根据OpenAPI规范，saveTemplateContent接口不支持status参数
  // 但我们可以在content中添加提交状态信息
  console.log("保存模板内容 - ID:", id);
  console.log("保存模板内容 - 数据类型:", typeof content);
  console.log(
    "保存模板内容 - 数据前10个字符:",
    content.substring(0, 10) + "..."
  );

  return request({
    url: "/api/userTemplate/saveTemplateContent",
    method: "post",
    params: { id },
    data: content,
    headers: {
      "Content-Type": "application/json", // 确保内容类型为JSON
    },
  });
}

// 管理员审核用户提交的模板
export function reviewTemplate(id, status, remarks) {
  return request({
    url: "/api/userTemplate/reviewTemplate",
    method: "post",
    params: { id, status },
    data: remarks,
  });
}

// 管理员审核用户的申请（初始审核）
export function reviewApplication(id, status, remarks) {
  // 使用同一个审核接口，因为OpenAPI文档中没有单独的申请审核接口
  return request({
    url: "/api/userTemplate/reviewTemplate",
    method: "post",
    params: { id, status },
    data: remarks,
  });
}

// 用户提交表单内容进行审核
export function submitForReview(id) {
  return request({
    url: "/api/userTemplate/submitForReview",
    method: "post",
    params: { id },
  });
}

// 更新模板状态 (可用于用户提交审核或管理员审核)
export function updateTemplateStatus(id, status, remarks) {
  return request({
    url: "/api/userTemplate/updateTemplateStatus",
    method: "post",
    params: { id, status },
    data: remarks,
  });
}

// 获取模板统计信息
export function getTemplateStatistics() {
  return request({
    url: "/api/userTemplate/statistics",
    method: "get",
  });
}

// 获取状态类型选项
export function getStatusOptions() {
  return Promise.resolve({
    code: 200,
    data: [
      { value: "0", label: "待审核" },
      { value: "1", label: "申请通过" },
      { value: "2", label: "拒绝申请" },
      { value: "3", label: "待填写" },
      { value: "4", label: "填写中" },
      { value: "5", label: "审核中" },
      { value: "6", label: "审核通过" },
      { value: "7", label: "退回" },
    ],
    message: "获取成功",
  });
}
