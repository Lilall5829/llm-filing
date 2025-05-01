import request from "./auth";

// 下载备案文档
export function downloadFilingDocument(id) {
  return request({
    url: "/api/file/downloadword",
    method: "get",
    params: { id },
    responseType: "blob", // 响应类型设为blob以处理文件下载
  });
}

// 处理文件下载（将blob转换为下载文件）
export function handleFileDownload(response, fileName) {
  // 创建blob链接
  const blob = new Blob([response.data]);
  const url = window.URL.createObjectURL(blob);

  // 创建a标签并触发下载
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName || "downloaded-file.docx";
  document.body.appendChild(link);
  link.click();

  // 清理DOM
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
