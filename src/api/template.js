import request from "./auth";

// 获取模板列表
export function getTemplateList(params) {
  return request({
    url: "/api/templateRegistry/page",
    method: "get",
    params,
  });
}

// 获取公共模板列表 (普通用户可访问版本)
export function getPublicTemplates(params) {
  return request({
    url: "/api/public/templates",
    method: "get",
    params,
  });
}

// 获取模板详情
export function getTemplateDetail(templateId) {
  return request({
    url: `/api/templateRegistry/getTemplateRegistryById`,
    method: "get",
    params: { templateId },
  });
}

// 获取模板类型列表 - 从模板列表接口中提取类型
export function getTemplateTypes() {
  // 使用模板列表接口获取数据，后续在前端处理类型信息
  return request({
    url: "/api/templateRegistry/page",
    method: "get",
    params: {
      pageSize: 100, // 获取足够多的模板以提取所有可能的类型
    },
  });
}

// 获取模板节点列表 - 从模板详情API中获取数据
export function getTemplateNodes() {
  // 使用模板列表接口获取所有模板，后续在前端提取节点信息
  return request({
    url: "/api/templateRegistry/page",
    method: "get",
    params: {
      pageSize: 50, // 获取足够多的模板以提取所有可能的节点
    },
  });
}

// 保存模板
export function saveTemplate(data, file) {
  const formData = new FormData();

  // 处理文件参数
  if (file) {
    // 如果提供了文件，使用该文件
    formData.append("file", file);
  } else {
    // 如果没有提供文件，创建一个空的Blob作为占位文件
    // 因为后端API要求必须有文件部分，即使是在编辑模式下

    // 创建一个空的文本Blob，大小极小
    const emptyBlob = new Blob([""], { type: "application/octet-stream" });

    // 创建一个标记为空文件的File对象
    const emptyFile = new File([emptyBlob], "empty-placeholder.txt", {
      type: "application/octet-stream",
      lastModified: new Date().getTime(),
    });

    // 添加占位文件到表单数据，同时添加标记，便于后端识别这是占位符而非实际文件
    formData.append("file", emptyFile);

    // 在data中添加标记，告诉后端这是空文件占位符，不要覆盖原有文件
    if (data.id) {
      data.keepExistingFile = true;
    }
  }

  formData.append("data", JSON.stringify(data));

  return request({
    url: "/api/templateRegistry/saveTemplateRegistry",
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 删除模板
export function deleteTemplate(templateId) {
  // 确保templateId是有效值
  if (!templateId) {
    console.error("删除模板失败: 未提供有效的模板ID");
    return Promise.reject(new Error("模板ID不能为空"));
  }

  return request({
    url: `/api/templateRegistry/deleteTemplate`,
    method: "delete",
    params: { templateId },
    // 增加超时时间，确保操作有足够时间完成
    timeout: 10000,
    // 重试机制
    retry: 2,
    retryDelay: 1000,
    validateStatus: function (status) {
      // 接受所有状态码，不让axios抛出错误，以便我们可以自定义错误处理
      return true;
    },
  })
    .then((response) => {
      // 检查HTTP状态码
      if (response.status >= 400) {
        console.error(
          `删除API HTTP错误: ${response.status} - ${
            response.statusText || "无状态文本"
          }`
        );
        return Promise.reject(
          new Error(
            `服务器返回错误: ${response.status} ${response.statusText || ""}`
          )
        );
      }

      // 检查响应状态
      if (response.code !== 200) {
        console.error(
          `删除API业务错误: code=${response.code}, message=${
            response.message || "无错误消息"
          }`
        );
        return Promise.reject(
          new Error(response.message || "删除失败，服务器未返回成功状态")
        );
      }

      return response;
    })
    .catch((error) => {
      console.error("删除模板API错误:", error);

      // 详细记录错误信息
      if (error.response) {
        // 服务器返回了错误响应
        console.error("错误状态:", error.response.status);
        console.error("错误头信息:", error.response.headers);
        console.error("错误数据:", error.response.data);
      } else if (error.request) {
        // 请求已发送但没有收到响应
        console.error("未收到响应，请求信息:", error.request);
      } else {
        // 请求设置阶段出错
        console.error("请求配置错误:", error.message);
      }

      // 重新抛出错误，让调用方处理
      throw error;
    });
}

// 导入Word模板
export function importWordTemplate(file, templateName, templateType) {
  const formData = new FormData();
  formData.append("file", file);

  // 添加模板名称和类型（如果有）
  const data = {};
  if (templateName) data.templateName = templateName;
  if (templateType) data.templateType = templateType;

  formData.append("data", JSON.stringify(data));

  return request({
    url: "/api/templateRegistry/saveTemplateRegistry",
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}
