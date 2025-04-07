<template>
  <div class="template">
    <!-- <a-page-header
      :title="isViewMode ? '模板查看' : '模板编辑'"
      :back-icon="true"
      @back="() => router.back()"
    /> -->

    <a-card class="form-card">
      <a-form
        :model="templateForm"
        :disabled="isViewMode"
      >
        <a-space wrap class="form-space">
          <a-form-item
            label="模板名称"
            name="name"
            :rules="[{ required: true, message: '请输入模板名称' }]"
            class="name-item"
          >
            <a-input
              v-model:value="templateForm.name"
            placeholder="请输入模板名称"
              allow-clear
            />
          </a-form-item>

          <a-form-item
            label="模板类型"
            name="type"
            :rules="[{ required: true, message: '请选择模板类型' }]"
            class="type-item"
          >
            <a-select
              v-model:value="templateForm.type"
              placeholder="请选择模板类型"
              allow-clear
              class="type-select"
            >
              <a-select-option value="生成合成类">生成合成类</a-select-option>
              <a-select-option value="信息检索类">信息检索类</a-select-option>
              <a-select-option value="排序精选类">排序精选类</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item class="button-item">
            <a-button type="primary" @click="handleExportWord" class="export-button">
              <template #icon><DownloadOutlined /></template>
              导出Word模板
            </a-button>
          </a-form-item>
        </a-space>
      </a-form>
    </a-card>

    <a-card class="content-card">
      <a-form-item label="填写节点列表" class="node-selector">
        <a-space wrap class="node-space">
          <a-select
            v-model:value="selectedNode"
            placeholder="请选择节点"
            class="node-select"
          >
            <a-select-option value="algorithm" class="select-option">
              <template #icon><CodeOutlined /></template>
              <span class="option-text">算法基础属性信息</span>
            </a-select-option>
            <template v-for="node in customNodes" :key="node.id">
              <a-select-option :value="node.id" class="select-option">
                <template #icon><FileOutlined /></template>
                <span class="option-text">{{ node.name }}</span>
              </a-select-option>
            </template>
          </a-select>
          <a-button type="primary" @click="showAddNodeModal" class="add-node-button">
            <template #icon><PlusOutlined /></template>
            添加节点
          </a-button>
        </a-space>
      </a-form-item>

      <div class="node-content">
        <div class="preview-header">
          <h3>节点预览</h3>
        </div>
        <template v-if="selectedNode">
          <a-form
            :model="currentNodeForm"
            layout="vertical"
            disabled
          >
            <template v-for="field in getCurrentNodeFields()" :key="field.id">
              <a-form-item
                :label="field.label"
                :name="field.id"
                :rules="[{ required: field.required, message: `请${field.type === 'select' ? '选择' : '输入'}${field.label}` }]"
              >
                <template #extra>{{ field.guide }}</template>
                <template v-if="field.type === 'checkbox' && field.id === 'application_fields'">
                  <div class="application-fields-container">
                    <template v-for="category in field.props.options" :key="category.value">
                      <div class="category-group">
                        <div class="category-title">{{ category.label }}</div>
                        <div class="category-options">
                          <a-checkbox-group
                            v-model:value="currentNodeForm[field.id]"
                            :options="category.children"
                            disabled
                          />
                        </div>
                      </div>
                    </template>
                  </div>
                </template>
                <component
                  v-else
                  :is="getFieldComponent(field.type)"
                  v-model:value="currentNodeForm[field.id]"
                  :placeholder="field.example || `请${field.type === 'select' ? '选择' : '输入'}${field.label}`"
                  v-bind="field.props"
                  disabled
                />
              </a-form-item>
            </template>
          </a-form>
        </template>
        <template v-else>
          <a-empty description="请选择节点" />
        </template>
      </div>

      <!-- 字段编辑器区域 -->
      <div v-if="selectedNode" class="field-editor">
        <div class="field-editor-header">
          <h3>{{ getCurrentNodeName() }} - 字段管理</h3>
          <a-button type="primary" @click="handleAddField">
            <template #icon><PlusOutlined /></template>
            添加新字段
          </a-button>
        </div>

        <div class="fields-container">
          <template v-for="(field, index) in getCurrentNodeFields()" :key="field.id">
            <div class="field-item">
              <div class="field-header">
                <span class="field-index">第{{ index + 1 }}项</span>
                <div class="field-actions">
                  <a-space wrap class="action-buttons">
                    <a-button 
                      type="link" 
                      :disabled="index === 0"
                      @click="moveField(index, 'up')"
                      class="action-button"
                    >
                      <template #icon><UpOutlined /></template>
                      向上移动
                    </a-button>
                    <a-button 
                      type="link" 
                      :disabled="index === getCurrentNodeFields().length - 1"
                      @click="moveField(index, 'down')"
                      class="action-button"
                    >
                      <template #icon><DownOutlined /></template>
                      向下移动
                    </a-button>
                    <a-button 
                      type="link" 
                      danger 
                      @click="deleteField(index)"
                      class="action-button"
                    >
                      <template #icon><DeleteOutlined /></template>
                      删除
                    </a-button>
                  </a-space>
        </div>
      </div>

              <a-form layout="inline" class="field-form">
                <a-form-item label="字段名称" class="field-form-item">
                  <a-input
                    v-model:value="field.label"
                    placeholder="请输入字段名称"
                    class="field-input"
                  />
                </a-form-item>

                <a-form-item label="字段类型" class="field-form-item">
                  <a-select
                    v-model:value="field.type"
                    class="field-input"
                  >
                    <a-select-option value="input">文本输入</a-select-option>
                    <a-select-option value="select">下拉选择</a-select-option>
                    <a-select-option value="date">日期选择</a-select-option>
                    <a-select-option value="checkbox">多选框</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="必填项" class="field-form-item">
                  <a-checkbox v-model:checked="field.required">是</a-checkbox>
                </a-form-item>

                <a-form-item label="字段实例" class="field-form-item">
                  <a-input
                    v-model:value="field.example"
                    placeholder="请输入字段实例"
                    class="field-input"
                  />
                </a-form-item>

                <a-form-item label="填写指引" class="field-form-item">
                  <a-input
                    v-model:value="field.guide"
                    placeholder="请输入填写指引"
                    class="field-input"
                  />
                </a-form-item>

                <template v-if="field.type === 'checkbox' && field.id === 'application_fields'">
                  <div class="field-options-editor">
                    <div class="field-options-header">
                      <h4>选项配置</h4>
                    </div>
                    <div class="category-editor">
                      <template v-for="(category, categoryIndex) in field.props.options" :key="category.value">
                        <div class="category-edit-group">
                          <div class="category-edit-header">
                            <span class="category-name">{{ category.label }}</span>
                          </div>
                          <div class="category-children">
                            <template v-for="(child, childIndex) in category.children" :key="child.value">
                              <div class="option-item">
                                <a-input
                                  v-model:value="child.label"
                                  placeholder="选项名称"
                                  class="option-input"
                                />
                                <a-input
                                  v-model:value="child.value"
                                  placeholder="选项值"
                                  class="option-input"
                                />
                                <a-button
                                  type="link"
                                  danger
                                  @click="() => deleteOption(field, categoryIndex, childIndex)"
                                >
                                  删除
                                </a-button>
                              </div>
                            </template>
                            <a-button
                              type="dashed"
                              block
                              @click="() => addOption(field, categoryIndex)"
                              style="margin-top: 8px"
                            >
                              <template #icon><PlusOutlined /></template>
                              添加选项
                            </a-button>
                          </div>
                        </div>
                      </template>
                    </div>
                  </div>
                </template>
              </a-form>
            </div>
          </template>
        </div>

        <div v-if="getCurrentNodeFields().length === 0" class="empty-fields">
          <a-empty description="暂无字段，请添加新字段" />
          </div>

        <div class="field-editor-footer">
          <a-button type="dashed" block @click="handleAddField" style="margin-bottom: 16px">
            <template #icon><PlusOutlined /></template>
            添加下一项
          </a-button>
          
          <a-button 
            type="primary" 
            block 
            :loading="saving"
            @click="handleSaveNode"
          >
            <template #icon><SaveOutlined /></template>
            保存修改
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- 添加节点对话框 -->
    <a-modal
      v-model:visible="addNodeModalVisible"
      title="添加节点"
      @ok="handleAddNode"
      @cancel="handleCancelAddNode"
    >
      <a-form layout="vertical">
        <a-form-item
          label="节点名称"
          :rules="[{ required: true, message: '请输入节点名称' }]"
        >
          <a-input v-model:value="newNodeName" placeholder="请输入节点名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import templateData from '@/mock/template.json';
import {
CodeOutlined,
DeleteOutlined,
DownloadOutlined,
DownOutlined,
FileOutlined,
PlusOutlined,
SaveOutlined,
UpOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const templateId = route.query.id;
const isViewMode = route.query.mode === "view";

// 表单数据
const templateForm = reactive({
  name: "",
  type: "",
  file: null,
  fileName: "",
  algorithmNode: null,
  customNodes: []
});

// 算法表单数据
const algorithmForm = reactive({
  algorithmType: "",
  algorithmName: "",
  launchTime: null,
  version: "",
  applicationFields: [],
});


// 选中的节点
const selectedNode = ref(null);

// 文件列表
const fileList = ref([]);

// 自定义节点列表
const customNodes = ref([]);

// 节点表单数据
const nodeForms = reactive({});

// 添加节点对话框
const addNodeModalVisible = ref(false);
const newNodeName = ref('');

// 新字段数据
const newField = reactive({
  label: '',
  type: 'input',
  name: '',
  rules: [],
  props: {}
});

// 保存状态
const saving = ref(false);

// 当前节点的表单数据
const currentNodeForm = reactive({});

// 获取当前节点名称
const getCurrentNodeName = () => {
  if (!selectedNode.value) return '';
  const node = templateData.nodes.find(n => n.id === selectedNode.value);
  return node ? node.name : '新建节点';
};

// 监听节点选择变化
watch(selectedNode, (newValue) => {
  if (newValue) {
    // 清空当前表单数据
    Object.keys(currentNodeForm).forEach(key => {
      delete currentNodeForm[key];
    });
    
    // 初始化或加载节点数据
    if (newValue === 'algorithm') {
      // 从模板数据中加载算法节点数据
      const node = templateData.nodes.find(n => n.id === 'algorithm');
      if (node) {
        node.fields.forEach(field => {
          currentNodeForm[field.id] = '';
        });
      }
    } else {
      // 确保自定义节点的表单数据已初始化
      if (!nodeForms[newValue]) {
        nodeForms[newValue] = reactive({
          fields: []
        });
      }
      // 初始化自定义节点的表单数据
      nodeForms[newValue].fields.forEach(field => {
        currentNodeForm[field.id] = '';
      });
    }
  }
});

// 获取字段类型名称
const getFieldTypeName = (type) => {
  const typeMap = {
    'input': '文本输入',
    'select': '下拉选择',
    'date': '日期选择',
    'checkbox': '多选框'
  };
  return typeMap[type] || type;
};

// 处理保存节点
const handleSaveNode = async () => {
  if (!selectedNode.value) {
    message.error('请选择要保存的节点');
    return;
  }

  // 验证字段
  const fields = getCurrentNodeFields();
  if (fields.length === 0) {
    message.error('请至少添加一个字段');
    return;
  }

  for (const field of fields) {
    if (!field.label) {
      message.error('请填写所有字段的名称');
      return;
    }
  }

  try {
    saving.value = true;

    // 构建要保存的模板数据
    const templateData = {
      id: templateId,
      name: templateForm.name,
      type: templateForm.type,
      nodes: [
        {
          id: 'algorithm',
          name: '算法基础属性信息',
          fields: nodeForms['algorithm']?.fields || []
        },
        ...customNodes.value.map(node => ({
          id: node.id,
          name: node.name,
          fields: nodeForms[node.id]?.fields || []
        }))
      ]
    };

    // 调用后端API保存模板数据
    await saveTemplateToBackend(templateData);

    message.success('保存成功');
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败，请重试');
  } finally {
    saving.value = false;
  }
};

// 保存模板到后端
const saveTemplateToBackend = async (templateData) => {
  try {
    const response = await fetch('/api/templates', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(templateData)
    });

    if (!response.ok) {
      throw new Error('保存失败');
    }

    return await response.json();
  } catch (error) {
    console.error('API调用失败:', error);
    throw error;
  }
};

// 导出Word模板
const handleExportWord = async () => {
  try {
    // 验证必填字段
    if (!templateForm.name || !templateForm.type) {
      message.error('请填写模板名称和类型');
      return;
    }

    // 构建导出数据
    const exportData = {
      templateName: templateForm.name,
      templateType: templateForm.type,
      nodes: [
        {
          id: 'algorithm',
          name: '算法基础属性信息',
          fields: nodeForms['algorithm']?.fields || []
        },
        ...customNodes.value.map(node => ({
          id: node.id,
          name: node.name,
          fields: nodeForms[node.id]?.fields || []
        }))
      ]
    };

    // 调用后端API导出Word
    const response = await fetch('/api/templates/export', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(exportData)
    });

    if (!response.ok) {
      throw new Error('导出失败');
    }

    // 获取blob数据
    const blob = await response.blob();
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${templateForm.name}.docx`;
    
    // 触发下载
    document.body.appendChild(link);
    link.click();
    
    // 清理
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    message.success('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    message.error('导出失败，请重试');
  }
};

// 处理文件上传前的验证
const handleBeforeUpload = (file) => {
  const isDoc = file.type === 'application/msword' || 
                file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  if (!isDoc) {
    message.error('只能上传Word文档！');
    return false;
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('文件大小不能超过 2MB！');
    return false;
  }
  return true;
};

// 显示添加节点对话框
const showAddNodeModal = () => {
  addNodeModalVisible.value = true;
};

// 处理添加节点
const handleAddNode = () => {
  if (!newNodeName.value) {
    message.error('请输入节点名称');
    return;
  }

  const nodeId = `node_${Date.now()}`;
  const newNode = {
    id: nodeId,
    name: newNodeName.value
  };
  
  customNodes.value.push(newNode);

  // 初始化节点的表单数据
  nodeForms[nodeId] = reactive({
    fields: []
  });

  // 重置并关闭对话框
  newNodeName.value = '';
  addNodeModalVisible.value = false;
  
  // 自动选中新创建的节点
  selectedNode.value = nodeId;
  message.success('节点添加成功');
};

// 取消添加节点
const handleCancelAddNode = () => {
  newNodeName.value = '';
  addNodeModalVisible.value = false;
};

// 获取节点表单数据
const getNodeForm = (nodeId) => {
  return nodeForms[nodeId] || {};
};

// 获取节点字段列表
const getNodeFields = (nodeId) => {
  return nodeForms[nodeId]?.fields || [];
};

// 获取字段组件
const getFieldComponent = (type) => {
  const components = {
    input: 'a-input',
    select: 'a-select',
    date: 'a-date-picker',
    checkbox: 'a-checkbox-group'
  };
  return components[type] || 'a-input';
};

// 获取当前节点的字段列表
const getCurrentNodeFields = () => {
  if (!selectedNode.value) return [];
  
  // 确保节点表单数据存在
  if (!nodeForms[selectedNode.value]) {
    // 如果是算法节点，从模板数据中查找
    if (selectedNode.value === 'algorithm') {
      const node = templateData.nodes.find(n => n.id === 'algorithm');
      if (node) {
        nodeForms[selectedNode.value] = reactive({
          fields: [...node.fields]
        });
      } else {
        nodeForms[selectedNode.value] = reactive({
          fields: []
        });
      }
    } else {
      // 如果是自定义节点，初始化空字段列表
      nodeForms[selectedNode.value] = reactive({
        fields: []
      });
    }
  }
  return nodeForms[selectedNode.value].fields;
};

// 修改添加字段方法
const handleAddField = () => {
  const nodeId = selectedNode.value;
  if (!nodeForms[nodeId]) {
    nodeForms[nodeId] = reactive({
      fields: []
    });
  }

  const fieldId = `field_${Date.now()}`;
  nodeForms[nodeId].fields.push({
    id: fieldId,
    label: '',
    type: 'input',
    required: true,
    example: '',
    guide: '',
    props: {
      allowClear: true
    }
  });
};

// 修改移动字段方法
const moveField = (index, direction) => {
  const fields = getCurrentNodeFields();
  if (direction === 'up' && index > 0) {
    const temp = fields[index];
    fields[index] = fields[index - 1];
    fields[index - 1] = temp;
  } else if (direction === 'down' && index < fields.length - 1) {
    const temp = fields[index];
    fields[index] = fields[index + 1];
    fields[index + 1] = temp;
  }
};

// 修改删除字段方法
const deleteField = (index) => {
  getCurrentNodeFields().splice(index, 1);
  message.success('字段删除成功');
};

// 添加选项
const addOption = (field, categoryIndex) => {
  const newOption = {
    label: '',
    value: `option_${Date.now()}`
  };
  field.props.options[categoryIndex].children.push(newOption);
};

// 删除选项
const deleteOption = (field, categoryIndex, childIndex) => {
  field.props.options[categoryIndex].children.splice(childIndex, 1);
};

// 如果是编辑或查看模式，加载数据
onMounted(() => {
  if (templateId) {
    // 加载模板基本信息
    templateForm.name = templateData.name;
    templateForm.type = templateData.type;

    // 初始化算法节点
    const algorithmNode = templateData.nodes.find(node => node.id === 'algorithm');
    if (algorithmNode) {
      templateForm.algorithmNode = algorithmNode;
      nodeForms['algorithm'] = reactive({
        fields: [...algorithmNode.fields]
      });
    }

    // 默认选中算法节点
    selectedNode.value = 'algorithm';
  }
});
</script>

<style>
.template {
  padding: 24px;
  min-width: 375px;
  overflow-x: auto;
}

.form-card {
  margin-bottom: 24px;
  min-width: 350px;
}

.form-card :deep(.ant-form-item-label) {
  min-width: 70px;
}

.form-card :deep(.ant-row) {
  display: flex;
  flex-wrap: wrap;
}

.form-card :deep(.ant-col) {
  display: flex;
}

.form-card :deep(.ant-form-item) {
  flex: 1;
  margin-bottom: 0;
}

.content-card {
  min-height: 500px;
  min-width: 350px;
}

.template::-webkit-scrollbar {
  height: 8px;
}

.template::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.template::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.template::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.template {
  scrollbar-width: thin;
  scrollbar-color: #888 #f1f1f1;
}

.node-selector {
  margin-bottom: 24px;
}

.node-selector .ant-form-item-control-input {
  width: 100%;
}

.node-selector .ant-space {
  width: 100%;
  gap: 16px !important;
}

.node-select {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.type-space {
  width: 100%;
  gap: 16px !important;
}

.type-select {
  flex: 1;
  min-width: 180px;
  max-width: 300px;
}

.export-button {
  white-space: nowrap;
}

.node-content {
  padding: 24px;
  background: #fff;
  border-radius: 2px;
}

.node-content :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.node-content :deep(.ant-form-item-label) {
  font-weight: 500;
}

.node-content :deep(.ant-input-disabled),
.node-content :deep(.ant-select-disabled),
.node-content :deep(.ant-picker-disabled),
.node-content :deep(.ant-checkbox-wrapper-disabled) {
  background-color: #fafafa;
  color: rgba(0, 0, 0, 0.85);
  cursor: default;
}

.node-content :deep(.ant-form-item-extra) {
  color: #8c8c8c;
  font-size: 13px;
  font-style: italic;
}

.application-fields-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.category-group {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.category-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.85);
}

.category-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: 16px;
}

.ant-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ant-checkbox-wrapper {
  margin-right: 0;
  white-space: nowrap;
}

.field-editor {
  margin-top: 24px;
  padding: 24px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 2px;
}

.field-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.field-editor-header h3 {
  margin: 0;
}

.fields-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.field-item {
  padding: 24px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 2px;
}

.field-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.field-index {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  white-space: nowrap;
}

.field-actions {
  flex: 1;
  min-width: 200px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px !important;
}

.action-button {
  padding: 4px 8px;
  height: auto;
  line-height: 1.5;
  white-space: nowrap;
}

.action-button .anticon {
  margin-right: 4px;
}

.field-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  width: 100%;
}

.field-form-item {
  flex: 1;
  min-width: 250px;
  margin: 0;
}

.field-input {
  width: 100%;
  min-width: 0;
}

.field-form .ant-form-item-label {
  min-width: 70px;
  white-space: normal;
  text-align: left;
}

.field-form .ant-input,
.field-form .ant-select,
.field-form .ant-checkbox-group {
  max-width: 100%;
}

.field-form .ant-select-selection-item,
.field-form .ant-input {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-options-editor {
  width: 100%;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.category-edit-group {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 2px;
  overflow-x: auto;
}

.option-item {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.option-input {
  flex: 1;
  min-width: 150px;
}

.empty-fields {
  padding: 48px 0;
  text-align: center;
}

.field-editor-footer {
  margin-top: 24px;
}

.select-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ant-select-selection-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ant-select-selection-item .anticon {
  flex-shrink: 0;
}

.form-space {
  width: 100%;
  gap: 24px !important;
}

.form-space :deep(.ant-space-item) {
  margin-right: 0 !important;
}

.name-item {
  min-width: 300px;
  margin-bottom: 0;
}

.type-item {
  min-width: 200px;
  margin-bottom: 0;
}

.button-item {
  margin-bottom: 0;
}

.type-select {
  width: 180px;
}

.export-button {
  white-space: nowrap;
}

@media screen and (max-width: 992px) {
  .form-space {
    gap: 16px !important;
  }

  .name-item,
  .type-item {
    width: 100%;
  }

  .type-select {
    width: 100%;
  }
}

@media screen and (max-width: 576px) {
  .form-space {
    gap: 8px !important;
  }

  .button-item {
    width: 100%;
  }

  .export-button {
    width: 100%;
  }
}

.preview-header {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-header h3 {
  margin: 0;
  color: rgba(0, 0, 0, 0.85);
}

.preview-content {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.preview-node-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: rgba(0, 0, 0, 0.85);
}

.preview-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-field-item {
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.field-label {
  font-weight: 500;
  margin-bottom: 8px;
}

.field-required {
  color: #ff4d4f;
  margin-left: 4px;
}

.field-type {
  color: #8c8c8c;
  font-size: 13px;
  margin-bottom: 4px;
}

.field-guide {
  color: #8c8c8c;
  font-size: 13px;
  font-style: italic;
}
</style>
