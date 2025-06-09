import request from "./auth";

// 分页获取已申请的模板列表
export const getAppliedTemplateList = async (params = {}) => {
  try {
    const response = await request.get("/api/userTemplate/page", { params });
    return response;
  } catch (error) {
    throw error;
  }
};

// 用户申请模板或管理员发送模板
export const applyTemplate = async (templateId, userIds = []) => {
  try {
    const response = await request.post(
      "/api/userTemplate/applyTemplate",
      userIds.length > 0 ? { userIds } : {},
      {
        params: { templateId },
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
    return response;
  } catch (error) {
    console.error("申请模板失败:", error);
    if (error.response) {
      console.error("错误详情:", error.response.data);
    }
    throw error;
  }
};

// 获取当前用户的模板内容
export const getTemplateContent = async (id) => {
  try {
    const response = await request.get(
      `/api/userTemplate/getTemplateContent?id=${id}`
    );
    return response;
  } catch (error) {
    throw error;
  }
};

// 保存用户填写的模板内容
export const saveTemplateContent = async (id, content) => {
  try {
    // 修复参数传递 - id作为查询参数，content作为请求体
    const response = await request.post(
      "/api/userTemplate/saveTemplateContent",
      content, // 直接传递content对象作为请求体
      {
        params: { id }, // id作为查询参数
        headers: {
          "Content-Type": "application/json",
        },
      }
    );

    return response;
  } catch (error) {
    throw error;
  }
};

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

// 获取模板定义（普通用户可访问）
export function getTemplateDefinition(templateId) {
  return request({
    url: "/api/userTemplate/getTemplateDefinition",
    method: "get",
    params: { templateId },
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
