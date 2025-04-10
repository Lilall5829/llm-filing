import request from "./auth";

// 获取模板列表
export function getTemplateList(params) {
  return request({
    url: "/api/templates/page",
    method: "get",
    params,
  });
}

// 获取模板详情
export function getTemplateDetail(id) {
  return request({
    url: `/api/templates/${id}`,
    method: "get",
  });
}

// 创建模板
export function createTemplate(data) {
  return request({
    url: "/api/templates",
    method: "post",
    data,
  });
}

// 更新模板
export function updateTemplate(id, data) {
  return request({
    url: `/api/templates/${id}`,
    method: "put",
    data,
  });
}

// 删除模板
export function deleteTemplate(id) {
  return request({
    url: `/api/templates/${id}`,
    method: "delete",
  });
}
