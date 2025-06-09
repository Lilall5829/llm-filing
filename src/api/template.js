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
    params: { id: templateId },
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
export const deleteTemplate = async (id) => {
  if (!id) {
    console.error("删除模板失败: 未提供有效的模板ID");
    throw new Error("模板ID不能为空");
  }

  try {
    const response = await request.delete(
      `/api/templateRegistry/deleteTemplate`,
      {
        params: { id },
      }
    );

    // 检查返回结果
    if (response && response.code === 200) {
      return response;
    } else {
      console.error("删除模板API错误:", error);

      // 详细错误信息记录
      if (error.response) {
        console.error("错误状态:", error.response.status);
        console.error("错误头信息:", error.response.headers);
        console.error("错误数据:", error.response.data);
      } else if (error.request) {
        console.error("未收到响应，请求信息:", error.request);
      } else {
        console.error("请求配置错误:", error.message);
      }

      throw error;
    }
  } catch (error) {
    throw new Error(`删除模板失败: ${response?.message || "服务器错误"}`);
  }
};

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
