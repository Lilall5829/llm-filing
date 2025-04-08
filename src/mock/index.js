import axios from "axios";
import MockAdapter from "axios-mock-adapter";

// 导入模拟数据
import applications from "./applications.json";
// 其他数据将在需要时导入

const mock = new MockAdapter(axios, { delayResponse: 1000 });

// 申请管理API
mock.onGet("/api/applications").reply((config) => {
  const { page = 1, pageSize = 10, status, keyword } = config.params || {};

  let filteredItems = [...applications.data.items];

  // 状态筛选
  if (status && status !== "all") {
    filteredItems = filteredItems.filter((item) => item.status === status);
  }

  // 关键词筛选
  if (keyword) {
    const lowercaseKeyword = keyword.toLowerCase();
    filteredItems = filteredItems.filter(
      (item) =>
        item.userName.toLowerCase().includes(lowercaseKeyword) ||
        item.content.title.toLowerCase().includes(lowercaseKeyword) ||
        item.content.description.toLowerCase().includes(lowercaseKeyword)
    );
  }

  // 计算分页
  const total = filteredItems.length;
  const startIndex = (page - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, total);
  const paginatedItems = filteredItems.slice(startIndex, endIndex);

  return [
    200,
    {
      code: 0,
      message: "获取成功",
      data: {
        total,
        items: paginatedItems,
      },
    },
  ];
});

// 获取单个申请
mock.onGet(new RegExp("/api/applications/.*")).reply((config) => {
  const id = config.url.split("/").pop();
  const application = applications.data.items.find((item) => item.id === id);

  if (application) {
    return [
      200,
      {
        code: 0,
        message: "获取成功",
        data: application,
      },
    ];
  } else {
    return [
      404,
      {
        code: 404,
        message: "申请不存在",
        data: null,
      },
    ];
  }
});

// 更新申请状态
mock.onPut(new RegExp("/api/applications/.*/status")).reply((config) => {
  const id = config.url.split("/")[3];
  const { status } = JSON.parse(config.data);
  const application = applications.data.items.find((item) => item.id === id);

  if (application) {
    application.status = status;
    return [
      200,
      {
        code: 0,
        message: "状态更新成功",
        data: application,
      },
    ];
  } else {
    return [
      404,
      {
        code: 404,
        message: "申请不存在",
        data: null,
      },
    ];
  }
});

// 添加更多模拟API...

// 确保导出mock对象，以便在main.js中引入
export default mock;
