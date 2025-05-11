<template>
  <div class="form-example">
    <a-card title="模板内容填写示例">
      <template #extra>
        <a-space>
          <a-button :loading="saving" @click="handleSave">保存草稿</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSubmit"
            >提交审核</a-button
          >
        </a-space>
      </template>

      <div v-if="loading" class="loading-container">
        <a-spin tip="加载模板内容中..."></a-spin>
      </div>

      <a-form
        v-else
        :model="formData"
        :labelCol="{ span: 6 }"
        :wrapperCol="{ span: 18 }"
      >
        <template v-if="formFields.length > 0">
          <a-form-item
            v-for="field in formFields"
            :key="field.id"
            :label="field.label"
            :name="field.id"
            :rules="[
              { required: field.required, message: `请输入${field.label}` },
            ]"
          >
            <!-- 根据字段类型选择不同的表单组件 -->
            <a-input
              v-if="field.type === 'text'"
              v-model:value="formData[field.id]"
              :placeholder="`请输入${field.label}`"
            />

            <a-textarea
              v-else-if="field.type === 'textarea'"
              v-model:value="formData[field.id]"
              :placeholder="`请输入${field.label}`"
              :rows="4"
            />

            <a-date-picker
              v-else-if="field.type === 'date'"
              v-model:value="formData[field.id]"
              style="width: 100%"
            />

            <a-select
              v-else-if="field.type === 'select'"
              v-model:value="formData[field.id]"
              :placeholder="`请选择${field.label}`"
              style="width: 100%"
            >
              <a-select-option
                v-for="option in field.options"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-select-option>
            </a-select>

            <!-- 默认显示普通文本输入框 -->
            <a-input
              v-else
              v-model:value="formData[field.id]"
              :placeholder="`请输入${field.label}`"
            />
          </a-form-item>
        </template>

        <div v-else class="empty-container">
          <a-empty description="没有可填写的表单内容" />
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from "vue";
import { message } from "ant-design-vue";
import { userTemplateAPI } from "@/api";

// 主要状态
const formFields = ref([]);
const formData = reactive({});
const loading = ref(false);
const saving = ref(false);
const submitting = ref(false);

// 模拟的用户模板关系ID，实际应当从路由或属性中获取
const userTemplateId = "user-template-123";

// 获取模板内容
const fetchTemplateContent = async () => {
  loading.value = true;

  try {
    const response = await userTemplateAPI.getTemplateContent(userTemplateId);

    if (response.code === 200 && response.data) {
      // 解析内容
      try {
        const contentData = JSON.parse(response.data);

        // 提取表单字段
        if (contentData.sections && Array.isArray(contentData.sections)) {
          // 假设内容结构为 { sections: [{ name: '段落1', fields: [{...}, {...}] }] }
          let allFields = [];
          contentData.sections.forEach((section) => {
            if (section.fields && Array.isArray(section.fields)) {
              // 为每个字段生成唯一ID
              const sectionFields = section.fields.map((field, index) => ({
                ...field,
                id: `${section.name.replace(/\s+/g, "_")}_field_${index}`,
                sectionName: section.name,
              }));
              allFields = [...allFields, ...sectionFields];
            }
          });

          formFields.value = allFields;

          // 如果有预填充的数据
          if (contentData.formData) {
            Object.assign(formData, contentData.formData);
          }
        } else if (
          contentData.formFields &&
          Array.isArray(contentData.formFields)
        ) {
          // 另一种可能的结构: { formFields: [{...}, {...}] }
          formFields.value = contentData.formFields.map((field, index) => ({
            ...field,
            id: `field_${index}`,
          }));

          if (contentData.formData) {
            Object.assign(formData, contentData.formData);
          }
        } else {
          message.warning("模板结构不完整");
          formFields.value = [];
        }
      } catch (error) {
        console.error("解析模板内容失败:", error);
        message.error("解析模板内容失败");
        formFields.value = [];
      }
    } else {
      formFields.value = [];

      // 如果是新模板，创建一些示例字段（实际项目中应当根据模板定义）
      createSampleFields();
    }
  } catch (error) {
    console.error("获取模板内容失败:", error);
    message.error("获取模板内容失败: " + (error.message || "未知错误"));

    // 创建一些示例字段（仅用于演示）
    createSampleFields();
  } finally {
    loading.value = false;
  }
};

// 创建示例字段（仅用于演示）
const createSampleFields = () => {
  formFields.value = [
    { id: "name", label: "姓名", type: "text", required: true },
    {
      id: "gender",
      label: "性别",
      type: "select",
      required: true,
      options: [
        { value: "male", label: "男" },
        { value: "female", label: "女" },
      ],
    },
    { id: "birthday", label: "出生日期", type: "date", required: true },
    { id: "address", label: "地址", type: "textarea", required: false },
    {
      id: "education",
      label: "学历",
      type: "select",
      required: true,
      options: [
        { value: "high_school", label: "高中" },
        { value: "college", label: "大专" },
        { value: "bachelor", label: "本科" },
        { value: "master", label: "硕士" },
        { value: "doctor", label: "博士" },
      ],
    },
  ];
};

// 保存草稿
const handleSave = async () => {
  if (saving.value) return;

  saving.value = true;

  try {
    // 构建要保存的内容
    const contentToSave = JSON.stringify({
      formFields: formFields.value,
      formData: formData,
    });

    const response = await userTemplateAPI.saveTemplateContent(
      userTemplateId,
      contentToSave
    );

    if (response.code === 200) {
      message.success("保存成功");
    } else {
      message.error("保存失败: " + response.message);
    }
  } catch (error) {
    console.error("保存失败:", error);
    message.error("保存失败: " + (error.message || "未知错误"));
  } finally {
    saving.value = false;
  }
};

// 提交审核
const handleSubmit = async () => {
  if (submitting.value) return;

  submitting.value = true;

  try {
    // 先保存当前内容
    const contentToSave = JSON.stringify({
      formFields: formFields.value,
      formData: formData,
    });

    const saveResponse = await userTemplateAPI.saveTemplateContent(
      userTemplateId,
      contentToSave
    );

    if (saveResponse.code !== 200) {
      throw new Error("保存内容失败: " + saveResponse.message);
    }

    // 更新状态为审核中(5)
    const updateResponse = await userTemplateAPI.updateTemplateStatus(
      userTemplateId,
      5,
      "用户提交审核"
    );

    if (updateResponse.code === 200) {
      message.success("提交审核成功");
    } else {
      message.error("提交审核失败: " + updateResponse.message);
    }
  } catch (error) {
    console.error("提交审核失败:", error);
    message.error("提交审核失败: " + (error.message || "未知错误"));
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  fetchTemplateContent();
});
</script>

<style scoped>
.form-example {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.empty-container {
  padding: 48px 0;
  text-align: center;
}

.ant-card {
  margin-bottom: 24px;
}
</style>
