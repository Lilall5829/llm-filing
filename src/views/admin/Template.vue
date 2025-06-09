<template>
  <div class="template">
    <!-- <a-page-header
      :title="isViewMode ? $t('template.templateView') : $t('template.templateEdit')"
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
            :label="$t('template.templateName')"
            name="name"
            :rules="[{ required: true, message: $t('template.pleaseInputTemplateName') }]"
            class="name-item"
          >
            <a-input
              v-model:value="templateForm.name"
              :placeholder="$t('template.pleaseInputTemplateName')"
              allow-clear
            />
          </a-form-item>

          <a-form-item
            :label="$t('template.templateCode')"
            name="code"
            :rules="[{ required: false, message: $t('template.pleaseInputTemplateCode') }]"
            class="code-item"
          >
            <a-input
              v-model:value="templateForm.code"
              :placeholder="$t('template.pleaseInputTemplateCode')"
              allow-clear
            />
          </a-form-item>

          <a-form-item
            :label="$t('template.templateType')"
            name="type"
            :rules="[{ required: true, message: $t('template.pleaseInputOrSelectTemplateType') }]"
            class="type-item"
          >
            <a-auto-complete
              v-model:value="templateForm.type"
              :options="templateTypeOptions"
              :placeholder="$t('template.pleaseInputOrSelectTemplateType')"
              allow-clear
              :filter-option="filterTemplateTypes"
              class="type-select"
            />
          </a-form-item>

          <a-form-item class="button-item">
            <a-button type="primary" @click="handleImportWord" class="export-button">
              <template #icon><UploadOutlined /></template>
              {{ $t('template.importWordTemplate') }}
            </a-button>
            <span v-if="templateForm.fileName" class="file-name-text">
              {{ templateForm.fileName }}
            </span>
          </a-form-item>
        </a-space>
      </a-form>
    </a-card>

    <a-card class="content-card">
      <a-form-item :label="$t('template.nodeList')" class="node-selector">
        <a-space wrap class="node-space">
          <a-select
            v-model:value="selectedNode"
            :placeholder="$t('template.pleaseSelectNode')"
            class="node-select"
          >
            <template v-for="node in customNodes" :key="node.id">
              <a-select-option :value="node.id" class="select-option">
                <template #icon><FileOutlined /></template>
                <span class="option-text">{{ node.name }}</span>
              </a-select-option>
            </template>
          </a-select>
          <a-button type="primary" @click="showAddNodeModal" class="add-node-button">
            <template #icon><PlusOutlined /></template>
            {{ $t('template.addNode') }}
          </a-button>
        </a-space>
      </a-form-item>

      <div class="node-content">
        <div class="preview-header">
          <h3>{{ $t('template.nodePreview') }}</h3>
        </div>
        <template v-if="selectedNode">
          <a-form
            :model="currentNodeForm"
            layout="vertical"
            disabled
          >
            <div class="fields-preview-container">
              <template v-for="(field, fieldIndex) in getCurrentNodeFields()" :key="fieldIndex">
                <template v-for="(cell, cellIndex) in (field.row || [])" :key="cellIndex">
                  <div class="field-cell" :class="{ 'half-width': cell.colspan === 1 }">
              <a-form-item
                      :label="cell.label"
                      :name="`field_${fieldIndex}_${cellIndex}`"
                    >
                      <template #extra>{{ cell.description || '' }}</template>
                      <template v-if="cell.type === 'checkbox' && cell.options && cell.options.length">
                        <div class="option-container">
                          <a-checkbox-group
                            v-model:value="currentNodeForm[`field_${fieldIndex}_${cellIndex}`]"
                            :options="cell.options" 
                            disabled
                          />
                      </div>
                    </template>
                      <template v-else-if="cell.type === 'radio' && cell.options && cell.options.length">
                        <a-radio-group 
                          v-model:value="currentNodeForm[`field_${fieldIndex}_${cellIndex}`]"
                          :options="cell.options" 
                          disabled
                        />
                      </template>
                      <template v-else-if="cell.type === 'select' && cell.options && cell.options.length">
                        <a-select
                          v-model:value="currentNodeForm[`field_${fieldIndex}_${cellIndex}`]"
                          :options="cell.options.map(opt => ({ label: opt, value: opt }))"
                          style="width: 100%"
                          disabled
                        />
                </template>
                <component
                  v-else
                        :is="getFieldComponent(cell.type)"
                        v-model:value="currentNodeForm[`field_${fieldIndex}_${cellIndex}`]"
                        :placeholder="$t('template.placeholders.pleaseSelectOrInput', { action: $t(`template.placeholders.${cell.type === 'select' ? 'select' : 'input'}`), label: cell.label })"
                        :rows="cell.type === 'textarea' || cell.type === 'richtext' ? 4 : undefined"
                  disabled
                />
              </a-form-item>
                  </div>
            </template>
              </template>
            </div>
          </a-form>
        </template>
        <template v-else>
          <a-empty :description="$t('template.pleaseSelectNode')" />
        </template>
      </div>

      <!-- 字段编辑器区域 -->
      <div v-if="selectedNode" class="field-editor">
        <div class="field-editor-header">
          <h3>{{ getCurrentNodeName() }} - {{ $t('template.fieldManagement') }}</h3>
          <a-button type="primary" @click="handleAddField">
            <template #icon><PlusOutlined /></template>
            {{ $t('template.addNewField') }}
          </a-button>
        </div>

        <div class="fields-container">
          <template v-for="(field, index) in getCurrentNodeFields()" :key="index">
            <div class="field-item">
              <div class="field-header">
                <span class="field-index">{{ $t('template.fieldItem', { index: index + 1 }) }}</span>
                <div class="field-actions">
                  <a-space wrap class="action-buttons">
                    <a-button 
                      type="link" 
                      :disabled="index === 0"
                      @click="moveField(index, 'up')"
                      class="action-button"
                    >
                      <template #icon><UpOutlined /></template>
                      {{ $t('template.moveUp') }}
                    </a-button>
                    <a-button 
                      type="link" 
                      :disabled="index === getCurrentNodeFields().length - 1"
                      @click="moveField(index, 'down')"
                      class="action-button"
                    >
                      <template #icon><DownOutlined /></template>
                      {{ $t('template.moveDown') }}
                    </a-button>
                    <a-button 
                      type="link" 
                      danger 
                      @click="deleteField(index)"
                      class="action-button"
                    >
                      <template #icon><DeleteOutlined /></template>
                      {{ $t('common.delete') }}
                    </a-button>
                  </a-space>
        </div>
      </div>

              <template v-for="(cell, cellIndex) in (field.row || [])" :key="cellIndex">
              <a-form layout="inline" class="field-form">
                <a-form-item :label="$t('template.fieldName')" class="field-form-item">
                  <a-input
                      v-model:value="cell.label"
                    :placeholder="$t('template.pleaseInputFieldName')"
                    class="field-input"
                  />
                </a-form-item>

                <a-form-item :label="$t('template.fieldType')" class="field-form-item">
                  <a-select
                      v-model:value="cell.type"
                    class="field-input"
                  >
                      <a-select-option value="text">{{ $t('template.fieldTypes.text') }}</a-select-option>
                      <a-select-option value="textarea">{{ $t('template.fieldTypes.textarea') }}</a-select-option>
                      <a-select-option value="richtext">{{ $t('template.fieldTypes.richtext') }}</a-select-option>
                    <a-select-option value="select">{{ $t('template.fieldTypes.select') }}</a-select-option>
                    <a-select-option value="date">{{ $t('template.fieldTypes.date') }}</a-select-option>
                    <a-select-option value="checkbox">{{ $t('template.fieldTypes.checkbox') }}</a-select-option>
                      <a-select-option value="radio">{{ $t('template.fieldTypes.radio') }}</a-select-option>
                  </a-select>
                </a-form-item>

                  <a-form-item :label="$t('template.columnWidth')" class="field-form-item">
                    <a-select
                      v-model:value="cell.colspan"
                    class="field-input"
                    >
                      <a-select-option :value="1">{{ $t('template.columnOptions.oneColumn') }}</a-select-option>
                      <a-select-option :value="2">{{ $t('template.columnOptions.twoColumns') }}</a-select-option>
                    </a-select>
                </a-form-item>

                  <a-form-item :label="$t('template.fieldDescription')" class="field-form-item">
                  <a-input
                      v-model:value="cell.description"
                      :placeholder="$t('template.pleaseInputFieldDescription')"
                    class="field-input"
                  />
                </a-form-item>

                  <template v-if="['checkbox', 'radio', 'select'].includes(cell.type)">
                  <div class="field-options-editor">
                    <div class="field-options-header">
                      <h4>{{ $t('template.optionConfig') }}</h4>
                        <a-button type="link" @click="addOption(cell)">
                          <template #icon><PlusOutlined /></template>
                          {{ $t('template.addOption') }}
                        </a-button>
                    </div>
                      <template v-if="!cell.options || cell.options.length === 0">
                        <div class="empty-options">
                          <a-empty :description="$t('template.noOptions')" />
                          </div>
                      </template>
                      <template v-else>
                        <div class="options-list">
                          <div v-for="(option, optionIndex) in cell.options" :key="optionIndex" class="option-item">
                                <a-input
                              :value="option"
                              @update:value="(val) => updateOptionValue(cell, optionIndex, val)"
                                  :placeholder="$t('template.optionValue')"
                                  class="option-input"
                                />
                                <a-button
                                  type="link"
                                  danger
                              @click="() => deleteOption(cell, optionIndex)"
                            >
                              {{ $t('template.deleteOption') }}
                            </a-button>
                          </div>
                        </div>
                      </template>
                  </div>
                </template>
              </a-form>
              </template>
            </div>
          </template>
        </div>

        <div v-if="getCurrentNodeFields().length === 0" class="empty-fields">
          <a-empty :description="$t('template.noFields')" />
          </div>

        <div class="field-editor-footer">
          <a-button type="dashed" block @click="handleAddField" style="margin-bottom: 16px">
            <template #icon><PlusOutlined /></template>
            {{ $t('template.addNextField') }}
          </a-button>
          
          <a-button 
            type="primary" 
            block 
            :loading="saving"
            @click="handleSaveNode"
          >
            <template #icon><SaveOutlined /></template>
            {{ $t('template.saveChanges') }}
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- 添加节点对话框 -->
    <a-modal
      v-model:visible="addNodeModalVisible"
      :title="$t('template.addNodeDialog')"
      @ok="handleAddNode"
      @cancel="handleCancelAddNode"
    >
      <a-form layout="vertical">
        <a-form-item
          :label="$t('template.nodeName')"
          :rules="[{ required: true, message: $t('template.pleaseInputNodeName') }]"
        >
          <a-auto-complete
            v-model:value="newNodeName"
            :options="availableNodeOptions"
            :placeholder="$t('template.pleaseSelectOrInputNodeName')"
            :filter-option="filterNodeOptions"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { templateAPI } from '@/api';
import {
DeleteOutlined,
DownOutlined,
FileOutlined,
PlusOutlined,
SaveOutlined,
UploadOutlined,
UpOutlined
} from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const templateId = route.query.id;
const isViewMode = route.query.mode === "view";
// 标记是否通过导入Word已经创建了模板
const templateCreatedByImport = ref(false);

// 表单数据
const templateForm = reactive({
  name: "",
  type: "",
  file: null,
  fileName: "",
  hasExistingFile: false,
  code: "",
});

// 选中的节点
const selectedNode = ref(null);

// 自定义节点列表
const customNodes = ref([]);

// 可用节点名称列表 - 从API获取
const availableNodeNames = ref([]);

// 可用节点选项 - 转换为AutoComplete需要的格式
const availableNodeOptions = computed(() => {
  return availableNodeNames.value.map(name => ({
    label: name,
    value: name
  }));
});

// 过滤节点选项
const filterNodeOptions = (inputValue, option) => {
  return option.value.toLowerCase().includes(inputValue.toLowerCase());
};

// 节点表单数据
const nodeForms = reactive({});

// 添加节点对话框
const addNodeModalVisible = ref(false);
const newNodeName = ref('');

// 保存状态
const saving = ref(false);

// 当前节点的表单数据
const currentNodeForm = reactive({});

// 模板类型选项 - 从后端获取
const templateTypeOptions = ref([]);

// 用于防止重复保存的标记
const isSubmitting = ref(false);

// 获取节点列表
const fetchNodeList = async () => {
  try {
    // 调用API获取模板列表，用于提取节点信息
    const response = await templateAPI.getTemplateNodes();
    
    // 检查响应数据
    if (response.data && response.data.records && response.data.records.length > 0) {
      // 从所有模板中提取节点信息
      const allNodes = [];
      
      // 遍历所有模板
      for (const template of response.data.records) {
        if (template.templateContent) {
          try {
            const content = JSON.parse(template.templateContent);
            
            // 处理新格式 sections
            if (content.sections && content.sections.length > 0) {
              content.sections.forEach(section => {
                if (section.name) {
                  allNodes.push(section.name);
                }
              });
            } 
            // 处理旧格式 nodes
            else if (content.nodes && content.nodes.length > 0) {
              content.nodes.forEach(node => {
                if (node.name) {
                  allNodes.push(node.name);
                }
              });
            }
          } catch (e) {
            console.error('解析模板内容失败:', e);
          }
        }
      }
      
      // 提取唯一的节点名称
      const uniqueNodes = [...new Set(allNodes)];
      
      // 将节点保存到全局变量中
      if (uniqueNodes.length > 0) {
        // 保存可用节点列表
        availableNodeNames.value = uniqueNodes;
      }
    }
  } catch (error) {
    console.error('获取节点列表失败:', error);
  }
};

// 获取模板类型列表
const fetchTemplateTypes = async () => {
  try {
    // 调用API获取模板列表
    const response = await templateAPI.getTemplateTypes();
    
    // 检查响应数据
    if (response.data && response.data.records && response.data.records.length > 0) {
      // 从列表数据中提取唯一的模板类型
      const types = [...new Set(response.data.records
        .filter(item => item.templateType)
        .map(item => item.templateType))];
      
      if (types.length > 0) {
        // 将提取的类型格式化为组件需要的格式
        templateTypeOptions.value = types.map(type => ({
          label: type,
          value: type
        }));
      } else {
        // 如果API返回为空，不使用默认值
        templateTypeOptions.value = [];
      }
    } else {
      // 如果API返回为空，不使用默认值
      templateTypeOptions.value = [];
    }
  } catch (error) {
    console.error('获取模板类型列表失败:', error);
    message.error(t('template.getTemplateTypesFailed'));
    // 发生错误时，设置为空数组
    templateTypeOptions.value = [];
  }
};

// 过滤模板类型选项
const filterTemplateTypes = (inputValue, option) => {
  return option.value.toLowerCase().includes(inputValue.toLowerCase());
};

// 获取当前节点名称
const getCurrentNodeName = () => {
  if (!selectedNode.value) return '';
  
  // 查找自定义节点名称
  const customNode = customNodes.value.find(node => node.id === selectedNode.value);
  return customNode ? customNode.name : '新建节点';
};

// 监听节点选择变化
watch(selectedNode, (newValue) => {
  if (newValue) {
    // 清空当前表单数据
    Object.keys(currentNodeForm).forEach(key => {
      delete currentNodeForm[key];
    });
    
    // 确保节点表单数据已初始化
      if (!nodeForms[newValue]) {
        nodeForms[newValue] = reactive({
          fields: []
        });
      }
    
    // 初始化表单数据字段默认值
    const fields = nodeForms[newValue].fields;
    fields.forEach((field, fieldIndex) => {
      if (field.row) {
        field.row.forEach((cell, cellIndex) => {
          const key = `field_${fieldIndex}_${cellIndex}`;
          
          // 根据字段类型设置适当的默认值
          if (cell.type === 'checkbox') {
            // 复选框类型，确保值是数组
            currentNodeForm[key] = cell.defaultValue || [];
          } else if (cell.type === 'radio' || cell.type === 'select') {
            // 单选或选择类型，设置为字符串或null
            currentNodeForm[key] = cell.defaultValue || null;
          } else {
            // 其他类型，默认为空字符串
            currentNodeForm[key] = cell.defaultValue || '';
          }
        });
      }
    });
  }
});

// 处理保存节点
const handleSaveNode = async () => {
  if (!selectedNode.value) {
    message.error(t('template.pleaseSelectNodeToSave'));
    return;
  }

  // 防止重复提交
  if (isSubmitting.value || saving.value) {
    message.info(t('template.savingInProgress'));
    return;
  }

  // 验证字段
  const fields = getCurrentNodeFields();
  if (fields.length === 0) {
    message.error(t('template.pleaseAddAtLeastOneField'));
    return;
  }

  for (const field of fields) {
    if (!field.row || !field.row.length || !field.row[0].label) {
      message.error(t('template.pleaseNameAllFields'));
      return;
    }
  }

  try {
    // 设置两个保存标记，双重防止重复提交
    saving.value = true;
    isSubmitting.value = true;

    // 构建完整模板数据（符合templateformat.json格式）
    const templateContent = {
      sections: customNodes.value.map(node => {
        return {
          name: node.name,
          fields: nodeForms[node.id]?.fields || []
        };
      })
    };

    // 准备API请求数据
    const templateDataToSave = {
      id: templateId,
      templateName: templateForm.name,
      templateCode: templateForm.code,
      templateType: templateForm.type,
      templateDescription: '', // 可选的模板描述
      templateContent: JSON.stringify(templateContent)
    };

    // 判断是否是编辑模式或是否已有关联文件
    const isEditMode = !!templateId || templateCreatedByImport.value;
    const hasFile = templateForm.file || templateForm.hasExistingFile;
    
    // 如果是新建模式（非编辑模式）且没有上传文件，显示提示
    if (!isEditMode && !hasFile) {
      message.warning(t('template.uploadTemplateFileFirst'));
      saving.value = false;
      return;
    }

    // 记录保存操作的类型（便于调试）
    let saveOperationType = '';
    
    // 判断是否是新建模板且已经通过导入创建了
    if (!templateId && templateCreatedByImport.value) {
      // 如果在导入Word时已经创建了模板，使用router中的id
      const currentTemplateId = route.query.id;
      if (currentTemplateId) {
        templateDataToSave.id = currentTemplateId;
        saveOperationType = t('template.updateImportedTemplate');
      }
    } else if (templateId) {
      saveOperationType = t('template.updateExistingTemplate');
    } else {
      saveOperationType = t('template.createNewTemplate');
    }

    // 保存模板，只在有新上传文件时传入文件
    // 如果是编辑模式且没有上传新文件，则不传入文件参数，后端会保留原有文件
    const fileToUpload = templateForm.file || null;
    
    const response = await templateAPI.saveTemplate(
      templateDataToSave, 
      fileToUpload
    );
    
    // 如果是新建模板，保存ID到路由中
    if (!templateId && response.data && response.data.id) {
      // 更新路由参数，但不触发页面重载
      router.replace({ 
        query: { ...route.query, id: response.data.id } 
      });
      templateCreatedByImport.value = true;
    }
    
    message.success(`${saveOperationType}成功`);
  } catch (error) {
    console.error('保存失败:', error);
    message.error(error.message || '保存失败，请重试');
  } finally {
    saving.value = false;
    isSubmitting.value = false;
  }
};

// 导入Word模板
const handleImportWord = async () => {
  try {
    // 如果是编辑模式并且已有文件，提示用户
    if ((templateId || templateCreatedByImport.value) && templateForm.hasExistingFile) {
      Modal.confirm({
        title: t('template.confirmImport'),
        content: t('template.replaceExistingDoc'),
        okText: t('template.continueImport'),
        cancelText: t('common.cancel'),
        onOk: () => triggerFileUpload(),
      });
    } else {
      triggerFileUpload();
    }
  } catch (error) {
    console.error('导入操作失败:', error);
    message.error(t('template.importFailed'));
  }
};

// 触发文件上传
const triggerFileUpload = () => {
  // 创建文件选择器
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.doc,.docx';
  
  input.onchange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;
    
    // 验证文件类型和大小
    if (!handleBeforeUpload(file)) return;
    
    // 保存文件引用，供后续保存模板时使用
    templateForm.file = file;
    templateForm.fileName = file.name;
    
    // 显示上传中提示
    const loadingMessage = message.loading(t('template.uploadingAndProcessing'), 0);
    
    try {
      // 准备要提交的基本信息
      const templateInfo = {
        templateName: templateForm.name || undefined,
        templateCode: templateForm.code || undefined,
        templateType: templateForm.type || undefined
      };
      
      // 如果是编辑模式，添加模板ID
      if (templateId) {
        templateInfo.id = templateId;
      } else if (templateCreatedByImport.value && route.query.id) {
        templateInfo.id = route.query.id;
      }
      
      // 调用API导入Word文档
      const response = await templateAPI.saveTemplate(templateInfo, file);
      
      if (response.data) {
        const template = response.data;
        
        // 加载模板基本信息
        templateForm.name = template.templateName;
        templateForm.code = template.templateCode;
        templateForm.type = template.templateType;
        
        // 标记已有文件
        templateForm.hasExistingFile = true;
        
        // 解析模板内容
        if (template.templateContent) {
          try {
            const templateContent = JSON.parse(template.templateContent);
            
            // 处理sections（新格式）
            if (templateContent.sections && templateContent.sections.length > 0) {
              initializeNodeFields(templateContent.sections);
            }
            // 兼容旧格式的nodes数组
            else if (templateContent.nodes && templateContent.nodes.length > 0) {
              convertAndInitializeNodes(templateContent.nodes);
            }
          } catch (e) {
            console.error('解析模板内容失败:', e);
            message.error(t('template.parseTemplateContentFailed'));
          }
        }
        
        // 获取模板ID (如果是新建模板)
        if (template.id && !templateId) {
          // 更新路由参数，但不触发页面重载
          router.replace({ 
            query: { ...route.query, id: template.id } 
          });
          // 标记已经通过导入创建了模板，避免重复保存
          templateCreatedByImport.value = true;
        }
        
        message.success(t('template.importSuccess'));
      }
  } catch (error) {
      console.error('导入失败:', error);
      message.error(error.message || t('template.importFailed'));
    } finally {
      // 关闭加载提示
      loadingMessage();
    }
  };
  
  // 触发文件选择
  input.click();
};

// 处理文件上传前的验证
const handleBeforeUpload = (file) => {
  const isDoc = file.type === 'application/msword' || 
                file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  if (!isDoc) {
    message.error(t('template.onlyWordFiles'));
    return false;
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error(t('template.fileSizeLimit'));
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
    message.error(t('template.pleaseInputNodeName'));
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
  message.success(t('template.nodeAddSuccess'));
};

// 取消添加节点
const handleCancelAddNode = () => {
  newNodeName.value = '';
  addNodeModalVisible.value = false;
};

// 获取字段组件
const getFieldComponent = (type) => {
  const typeMap = {
    'text': 'a-input',
    'textarea': 'a-textarea',
    'richtext': 'a-textarea',
    'checkbox': 'a-checkbox-group',
    'radio': 'a-radio-group',
    'date': 'a-date-picker',
    'select': 'a-select'
  };
  return typeMap[type] || 'a-input';
};

// 获取当前节点的字段列表
const getCurrentNodeFields = () => {
  if (!selectedNode.value) return [];
  
  // 确保节点表单数据存在
  if (!nodeForms[selectedNode.value]) {
        nodeForms[selectedNode.value] = reactive({
          fields: []
        });
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

  // 创建符合templateformat.json格式的新字段
  const newField = {
    row: [
      {
    label: '',
        type: 'text',
        colspan: 2
      }
    ]
  };
  
  nodeForms[nodeId].fields.push(newField);
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
  message.success(t('template.fieldDeleteSuccess'));
};

// 添加选项
const addOption = (cell) => {
  if (!cell.options) {
    cell.options = [];
  }
  cell.options.push('');
};

// 删除选项
const deleteOption = (cell, optionIndex) => {
  if (cell.options && cell.options.length > optionIndex) {
    cell.options.splice(optionIndex, 1);
  }
};

// 更新选项值
const updateOptionValue = (cell, optionIndex, value) => {
  if (cell.options && cell.options.length > optionIndex) {
    cell.options[optionIndex] = value;
  }
};

// 转换旧字段格式为新字段格式
const convertOldFieldsToNew = (oldFields) => {
  if (!oldFields || !Array.isArray(oldFields)) return [];
  
  return oldFields.map(field => {
    return {
      row: [
        {
          label: field.label,
          type: convertOldFieldType(field.type),
          colspan: 2,
          description: field.guide,
          options: field.props?.options ? convertOptions(field.props.options) : undefined
        }
      ]
    };
  });
};

// 转换旧字段类型为新字段类型
const convertOldFieldType = (oldType) => {
  const typeMap = {
    'input': 'text',
    'textarea': 'textarea',
    'select': 'select',
    'date': 'date',
    'checkbox': 'checkbox'
  };
  return typeMap[oldType] || 'text';
};

// 转换选项格式
const convertOptions = (oldOptions) => {
  if (Array.isArray(oldOptions)) {
    // 简单选项数组
    return oldOptions.map(opt => typeof opt === 'string' ? opt : (opt.label || opt.value));
  } else if (oldOptions && typeof oldOptions === 'object') {
    // 尝试处理复杂选项结构
    const result = [];
    Object.keys(oldOptions).forEach(key => {
      const option = oldOptions[key];
      if (option.children) {
        option.children.forEach(child => {
          result.push(child.label || child.value);
        });
      }
    });
    return result.length > 0 ? result : undefined;
  }
  return undefined;
};

// 处理加载已有模板内容的逻辑，确保字段类型正确
const initializeNodeFields = (sections) => {
  // 清空现有节点
  customNodes.value = [];
  
  sections.forEach((section, index) => {
    // 添加自定义节点
    const nodeId = `node_${Date.now()}_${index}`;
    customNodes.value.push({
      id: nodeId,
      name: section.name
    });
    
    // 确保字段的类型值正确
    const processedFields = section.fields.map(field => {
      // 深拷贝以避免修改原始对象
      const newField = JSON.parse(JSON.stringify(field));
      
      if (newField.row) {
        newField.row = newField.row.map(cell => {
          // 处理特殊类型的默认值
          if (cell.type === 'checkbox' && (!cell.defaultValue || typeof cell.defaultValue === 'string')) {
            cell.defaultValue = [];
          }
          return cell;
        });
      }
      
      return newField;
    });
    
    // 保存节点字段
    nodeForms[nodeId] = reactive({
      fields: processedFields
    });
  });
  
  // 自动选中第一个节点（如果有）
  if (customNodes.value.length > 0) {
    selectedNode.value = customNodes.value[0].id;
  }
};

// 转换旧格式节点到新格式，确保类型正确
const convertAndInitializeNodes = (nodes) => {
  // 清空现有节点
  customNodes.value = [];
  
  nodes.forEach((node) => {
    // 添加自定义节点
    const nodeId = node.id || `node_${Date.now()}_${customNodes.value.length}`;
    customNodes.value.push({
      id: nodeId,
      name: node.name
    });
    
    // 转换并保存节点字段
    const convertedFields = convertOldFieldsToNew(node.fields);
    nodeForms[nodeId] = reactive({
      fields: convertedFields
    });
  });
  
  // 自动选中第一个节点（如果有）
  if (customNodes.value.length > 0) {
    selectedNode.value = customNodes.value[0].id;
  }
};

// 组件挂载时初始化
onMounted(async () => {
  // 获取模板类型
  await fetchTemplateTypes();
  
  // 获取节点列表 - 预加载可用节点供用户选择
  await fetchNodeList();
  
  // 如果URL已有模板ID，表明是编辑模式，标记为已创建状态
  if (templateId) {
    templateCreatedByImport.value = true;
    
    try {
      // 显示加载状态
      saving.value = true;
      
      // 调用API获取模板详情
      const response = await templateAPI.getTemplateDetail(templateId);
      
      if (response.data) {
        const template = response.data;
        
    // 加载模板基本信息
        templateForm.name = template.templateName;
        templateForm.code = template.templateCode;
        templateForm.type = template.templateType;
        
        // 设置文件名和文件状态
        if (template.filePath) {
          const pathParts = template.filePath.split('/');
          templateForm.fileName = pathParts[pathParts.length - 1] || '已上传文件';
          
          // 标记模板已有文件，即使没有实际文件对象
          // 这里我们设置一个特殊标志，表示这是一个已存在的文件
          // 虽然没有实际的File对象，但可以通过此标志判断模板是否已有关联文件
          templateForm.hasExistingFile = true;
        }
        
        // 解析模板内容
        if (template.templateContent) {
          try {
            const templateContent = JSON.parse(template.templateContent);
            
            // 处理sections（新格式）
            if (templateContent.sections && templateContent.sections.length > 0) {
              initializeNodeFields(templateContent.sections);
            }
            // 兼容旧格式的nodes数组
            else if (templateContent.nodes && templateContent.nodes.length > 0) {
              convertAndInitializeNodes(templateContent.nodes);
            }
          } catch (e) {
            console.error('解析模板内容失败:', e);
            message.error(t('template.parseTemplateContentFailed'));
          }
        }
      }
    } catch (error) {
      console.error('获取模板详情失败:', error);
      message.error(error.message || t('template.getTemplateDetailFailed'));
    } finally {
      saving.value = false;
    }
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

.code-item {
  min-width: 250px;
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
  .code-item,
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

.field-row-preview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.field-cell {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  flex: 0 0 100%;
  box-sizing: border-box;
}

.field-cell.half-width {
  flex: 0 0 calc(50% - 8px);
}

.option-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.empty-options {
  margin: 16px 0;
}

.fields-preview-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  width: 100%;
}

@media (max-width: 768px) {
  .field-cell.half-width {
    flex: 0 0 100%;
  }
}

.file-name-text {
  margin-left: 8px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}
</style>
