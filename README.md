# 大模型备案信息填报系统

基于 Vue 3 的大模型备案信息填报系统，包括管理端和用户端。

## 项目架构

- 管理端

  - 模板管理
  - 用户管理
  - 任务看板
  - 修改密码
  - 退出登录

- 用户端
  - 备案申请
  - 备案中心
  - 修改密码
  - 退出登录

## 项目设置

```sh
npm install
```

### 开发环境

```sh
npm run dev
```

### 生产环境

```sh
npm run build
```

## 项目结构

```
src/
├── assets/          # 静态资源
├── views/           # 页面组件
│   ├── admin/       # 管理端页面
│   ├── user/        # 用户端页面
│   └── common/      # 公共页面
├── router/          # 路由配置
├── App.vue          # 根组件
└── main.js          # 入口文件
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

# 在Talend API Tester中配置multipart/form-data请求

## 基本步骤

1. **打开Talend API Tester**
   - 点击Chrome浏览器右上角的扩展图标，选择Talend API Tester

2. **创建新请求**
   - 点击"+"按钮创建新请求
   - 输入请求名称，例如"保存模板"

3. **设置请求方法和URL**
   - 选择方法：POST
   - 输入URL：`http://your-api-host/api/templateRegistry/saveTemplateRegistry`

4. **添加认证头**
   - 在"Headers"标签下点击"Add header"
   - 名称：`Authorization`
   - 值：`Bearer your_token_here`（替换为实际token）

## 配置multipart/form-data

5. **选择正确的内容类型**
   - 在Body标签中，选择"form-data"选项（不要选择"raw"或"x-www-form-urlencoded"）

6. **添加文件参数**
   - 点击"Add parameter"
   - 名称：`file`
   - 类型：选择文件类型（在参数右侧有一个文件图标可点击）
   - 点击"Choose file"按钮上传Word模板文件

7. **添加JSON数据参数**
   - 点击"Add parameter"
   - 名称：`data`
   - 类型：保持为文本类型
   - 值：输入完整的JSON对象（确保使用双引号，并且是一个有效的JSON字符串）
   - 例如：
   ```json
   {"templateName":"企业备案申请表","templateCode":"TPL-2023001","templateDescription":"企业备案申请模板","templateType":"企业备案","templateContent":"{\"sections\":[{\"sectionTitle\":\"基本信息\",\"fields\":[{\"id\":\"name\",\"label\":\"企业名称\",\"type\":\"text\"}]}]}"}
   ```

## 避免常见错误

1. **确保JSON格式正确**
   - 所有键名必须用双引号
   - 字符串值必须用双引号
   - 不要有多余的逗号
   - templateContent中的JSON需要作为字符串（被转义）

2. **注意multipart边界**
   - Talend会自动处理multipart/form-data的边界，无需手动设置Content-Type header

3. **检查参数名称**
   - 确保参数名称正确：`file`和`data`，区分大小写

## 发送请求

8. **发送请求**
   - 点击"Send"按钮
   - 观察状态码和响应体

9. **查看请求详情**
   - 在请求发送后，可以在"Request"标签查看完整的请求内容
   - 确保Content-Type包含`multipart/form-data; boundary=...`

如果模板名称不能为空错误仍然出现，请确认:
- data参数中的JSON包含templateName字段
- JSON格式完全正确
- 没有使用错误的参数名（如"formData"代替"data"）
