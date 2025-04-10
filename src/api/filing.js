import request from "./auth";

// 获取备案列表
export function getFilingList(params) {
  return request({
    url: "/api/filings/page",
    method: "get",
    params,
  });
}

// 获取备案详情
export function getFilingDetail(id) {
  return request({
    url: `/api/filings/${id}`,
    method: "get",
  });
}

// 创建备案
export function createFiling(data) {
  return request({
    url: "/api/filings",
    method: "post",
    data,
  });
}

// 更新备案
export function updateFiling(id, data) {
  return request({
    url: `/api/filings/${id}`,
    method: "put",
    data,
  });
}

// 提交备案审核
export function submitFiling(id) {
  return request({
    url: `/api/filings/${id}/submit`,
    method: "post",
  });
}

// 审核备案
export function reviewFiling(id, data) {
  return request({
    url: `/api/filings/${id}/review`,
    method: "post",
    data,
  });
}

// 删除备案
export function deleteFiling(id) {
  return request({
    url: `/api/filings/${id}`,
    method: "delete",
  });
}
