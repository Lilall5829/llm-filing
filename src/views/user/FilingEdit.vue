<template>
  <div class="filing-edit-container">
    <div class="filing-edit">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">{{ isReadonly ? $t('filingEdit.viewTitle') : $t('filingEdit.editTitle') }}</h2>
          <div class="submission-info" v-if="!isReadonly">
            <a-tag color="processing">
              {{ $t('filingEdit.completionProgress', { completed: completedSteps, total: templateContent.nodes?.length || 0 }) }}
            </a-tag>
          </div>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-card class="info-card">
          <a-descriptions :title="$t('filingEdit.basicInfo')" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item :label="$t('filingEdit.templateCode')">{{ templateInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('filingEdit.templateName')">{{ templateInfo.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('filingEdit.applicationTime')">{{ templateInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('common.updateTime')">{{ templateInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('common.status')">
              <a-tag :color="getStatusColor(templateInfo.status)">
                {{ getStatusText(templateInfo.status) }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 新增节点完成进度组件 -->
        <a-card class="progress-card" v-if="!loading && templateContent.nodes && templateContent.nodes.length > 0">
          <div class="progress-title">{{ $t('filingEdit.nodeProgress') }}</div>
          <a-steps class="node-progress" :current="completedSteps">
            <a-step 
              v-for="node in templateContent.nodes" 
              :key="node.id" 
              :title="node.name"
              :status="getNodeStatus(node.id)"
              @click="handleStepClick(node.id)"
            />
          </a-steps>
        </a-card>

        <a-card class="content-card" v-if="!loading && templateContent.nodes && templateContent.nodes.length > 0">
          <a-tabs v-model:activeKey="activeTabKey">
            <a-tab-pane 
              v-for="node in templateContent.nodes" 
              :key="node.id"
              :tab="node.name"
            >
              <div class="node-content">
                <a-form
                  :model="nodeForms[node.id]"
                  layout="vertical"
                  :ref="el => formRefs[node.id] = el"
                  :disabled="isReadonly"
                >
                  <a-row :gutter="[16, 0]">
                    <a-col 
                      v-for="field in node.fields" 
                      :key="field.id"
                      :xs="24" 
                      :sm="field.type === 'textarea' || field.id === 'application_fields' ? 24 : 12"
                    >
                      <a-form-item
                        :label="field.label"
                        :name="field.id"
                        :rules="[{ required: field.required, message: $t('filingEdit.fieldRequired', { action: field.type === 'select' ? $t('filingEdit.select') : $t('filingEdit.input'), field: field.label }) }]"
                      >
                        <template #extra>{{ field.guide }}</template>
                        
                        <template v-if="field.type === 'checkbox' && field.id === 'application_fields'">
                          <div class="application-fields-container">
                            <a-checkbox-group v-model:value="nodeForms[node.id][field.id]">
                              <div 
                                v-for="category in field.props.options" 
                                :key="category.value"
                                class="category-group"
                              >
                                <div class="category-title">{{ category.label }}</div>
                                <div class="category-options">
                                  <a-checkbox 
                                    v-for="child in category.children" 
                                    :key="child.value"
                                    :value="child.value"
                                  >
                                    {{ child.label }}
                                  </a-checkbox>
                                </div>
                              </div>
                            </a-checkbox-group>
                          </div>
                        </template>
                        
                        <template v-else-if="field.type === 'checkbox'">
                          <a-checkbox-group v-model:value="nodeForms[node.id][field.id]">
                            <a-checkbox 
                              v-for="option in getSelectOptions(field)" 
                              :key="getOptionKey(option)" 
                              :value="getOptionValue(option)"
                            >
                              {{ getOptionLabel(option) }}
                            </a-checkbox>
                          </a-checkbox-group>
                        </template>

                        <template v-else-if="field.type === 'radio'">
                          <a-radio-group v-model:value="nodeForms[node.id][field.id]">
                            <a-radio 
                              v-for="option in getSelectOptions(field)" 
                              :key="getOptionKey(option)" 
                              :value="getOptionValue(option)"
                            >
                              {{ getOptionLabel(option) }}
                            </a-radio>
                          </a-radio-group>
                        </template>
                        
                        <template v-else-if="field.type === 'input'">
                          <a-input
                            v-model:value="nodeForms[node.id][field.id]"
                            :placeholder="field.example || $t('filingEdit.pleaseInput', { field: field.label })"
                            v-bind="field.props"
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'select'">
                          <a-select
                            v-model:value="nodeForms[node.id][field.id]"
                            :placeholder="field.example || $t('filingEdit.pleaseSelect', { field: field.label })"
                            v-bind="field.props"
                          >
                            <a-select-option 
                              v-for="option in getSelectOptions(field)" 
                              :key="getOptionKey(option)" 
                              :value="getOptionValue(option)"
                            >
                              {{ getOptionLabel(option) }}
                            </a-select-option>
                          </a-select>
                        </template>
                        
                        <template v-else-if="field.type === 'date'">
                          <a-date-picker
                            v-model:value="nodeForms[node.id][field.id]"
                            :placeholder="field.example || $t('filingEdit.pleaseSelect', { field: field.label })"
                            style="width: 100%"
                            v-bind="field.props"
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'textarea'">
                          <a-textarea
                            v-model:value="nodeForms[node.id][field.id]"
                            :placeholder="field.example || $t('filingEdit.pleaseInput', { field: field.label })"
                            :rows="4"
                            v-bind="field.props"
                          />
                        </template>
                        
                        <template v-else>
                          <a-input
                            v-model:value="nodeForms[node.id][field.id]"
                            :placeholder="field.example || $t('filingEdit.pleaseInput', { field: field.label })"
                            v-bind="field.props"
                          />
                        </template>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>

        <div v-if="!loading && (!templateContent.nodes || templateContent.nodes.length === 0)" class="empty-content">
          <a-empty :description="$t('filingEdit.noTemplateContent')">
            <template #description>
              <p>{{ $t('filingEdit.noTemplateContentDesc') }}</p>
            </template>
          </a-empty>
        </div>

        <div class="footer-actions" v-if="!isReadonly">
          <a-space size="middle">
            <a-button type="primary" @click="handleSave" :loading="savingInProgress" :disabled="submittingInProgress">
              <template #icon><SaveOutlined /></template>
              {{ $t('common.save') }}
            </a-button>
            <a-button type="primary" @click="handleSubmit" :loading="submittingInProgress" 
                      :disabled="savingInProgress || !canSubmit">
              <template #icon><CheckOutlined /></template>
              {{ $t('filingEdit.submitForReview') }}
            </a-button>
          </a-space>
        </div>

        <div class="footer-actions" v-else>
          <a-space size="middle">
            <a-button @click="goBack">
              <template #icon><ArrowLeftOutlined /></template>
              {{ $t('common.back') }}
            </a-button>
          </a-space>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { userTemplateAPI } from '@/api';
import {
ArrowLeftOutlined,
CheckOutlined,
SaveOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const filingId = route.query.id;
const isReadonly = computed(() => route.query.readonly === 'true');
const submittingInProgress = ref(false);
const savingInProgress = ref(false);
const loading = ref(true);
const activeTabKey = ref('');
const nodeValidationState = reactive({});

// 判断是否可以提交
// 状态码1=申请通过，3=待填写，4=填写中，7=退回时可以提交
const canSubmit = computed(() => {
  const status = Number(templateInfo.status);
  return [1, 3, 4, 7].includes(status);
});

// 模板内容
const templateContent = reactive({
  nodes: []
});

// 备案数据
const templateInfo = reactive({
  id: '',
  templateId: '',
  templateCode: '',
  templateName: '',
  createTime: '',
  updateTime: '',
  status: '',
  statusDesc: ''
});

// 节点表单数据
const nodeForms = reactive({});

// 表单引用
const formRefs = reactive({});

// 计算已完成步骤数
const completedSteps = computed(() => {
  let completed = 0;
  
  for (const nodeId in nodeValidationState) {
    if (nodeValidationState[nodeId] === 'finish') {
      completed++;
    }
  }
  
  return completed;
});

// 获取状态文本
const getStatusText = (status) => {
  return t(`status.${Number(status)}`);
};

// 获取状态颜色
const getStatusColor = (status) => {
  const statusMap = {
    0: 'default',   // 待审核
    1: 'green',      // 申请通过
    2: 'red',       // 拒绝申请
    3: 'orange',    // 待填写
    4: 'purple',    // 填写中
    5: 'processing', // 审核中
    6: 'success',   // 审核通过
    7: 'error',     // 退回
  };
  return statusMap[Number(status)] || 'default';
};

// 获取节点状态
const getNodeStatus = (nodeId) => {
  return nodeValidationState[nodeId] || 'wait';
};

// 节点步骤点击处理
const handleStepClick = (nodeId) => {
  activeTabKey.value = nodeId;
};

// 返回上一页
const goBack = () => {
  router.push('/user/filing-center');
};

// 保存模板数据
const saveTemplateData = async () => {
  try {
    // 构建保存数据 - 确保不会双重编码JSON
    const contentToSave = {
      nodes: templateContent.nodes,
      formData: nodeForms
    };

    const response = await userTemplateAPI.saveTemplateContent(filingId, contentToSave);

    if (response && response.code === 200) {
      message.success(t('filingEdit.saveSuccess'));
      
      // 刷新模板信息，获取最新状态
      await fetchFilingInfo();
    } else {
      throw new Error(response?.message || t('filingEdit.saveFailed'));
    }
  } catch (error) {
    console.error('保存失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error(t('filingEdit.saveFailed') + ': ' + (error.message || t('filingEdit.unknownError')));
  }
};

// 处理保存按钮点击
const handleSave = async () => {
  if (savingInProgress.value || submittingInProgress.value) {
    message.info(t('filingEdit.operationInProgress'));
    return;
  }

  savingInProgress.value = true;
  try {
    await saveTemplateData();
  } finally {
    savingInProgress.value = false;
  }
};

// 处理提交审核按钮点击
const handleSubmit = async () => {
  if (savingInProgress.value || submittingInProgress.value) {
    message.info(t('filingEdit.operationInProgress'));
    return;
  }

  // 检查当前状态是否可以提交
  if (templateInfo.status !== 3 && templateInfo.status !== 4) {
    message.error(t('filingEdit.cannotSubmit'));
    return;
  }

  // 验证必填字段
  const hasIncompleteNodes = templateContent.nodes.some(node => {
    const requiredFields = node.fields.filter(field => field.required);
    return requiredFields.some(field => {
      const value = nodeForms[node.id]?.[field.id];
      return !value || (Array.isArray(value) && value.length === 0) || (typeof value === 'string' && value.trim() === '');
    });
  });

  if (hasIncompleteNodes) {
    const incompleteNode = templateContent.nodes.find(node => {
      const requiredFields = node.fields.filter(field => field.required);
      return requiredFields.some(field => {
        const value = nodeForms[node.id]?.[field.id];
        return !value || (Array.isArray(value) && value.length === 0) || (typeof value === 'string' && value.trim() === '');
      });
    });

    if (incompleteNode) {
      message.error(t('filingEdit.completeMissingFields', { nodeName: incompleteNode.name }));
      // 自动切换到有问题的节点
      activeTabKey.value = incompleteNode.id;
    }
    return;
  }

  submittingInProgress.value = true;
  try {
    await submitForReview();
  } finally {
    submittingInProgress.value = false;
  }
};

// 验证单个节点表单
const validateNodeForm = async (nodeId) => {
  try {
    if (formRefs[nodeId] && !isReadonly.value) {
      await formRefs[nodeId].validate();
      nodeValidationState[nodeId] = 'finish';
      return true;
    } else if (isReadonly.value) {
      // 只读模式下直接标记为完成
      nodeValidationState[nodeId] = 'finish';
      return true;
    }
    return false;
  } catch (error) {
    if (!isReadonly.value) {
      nodeValidationState[nodeId] = 'error';
    }
    return false;
  }
};

// 验证所有表单
const validateForms = async () => {
  try {
    let hasError = false;
    let firstErrorNodeId = null;
    
    // 遍历所有表单进行验证
    for (const nodeId in formRefs) {
      if (formRefs[nodeId] && !isReadonly.value) {
        try {
          await formRefs[nodeId].validate();
          nodeValidationState[nodeId] = 'finish';
        } catch (error) {
          hasError = true;
          nodeValidationState[nodeId] = 'error';
          
          // 记录第一个出错的节点
          if (!firstErrorNodeId) {
            firstErrorNodeId = nodeId;
          }
        }
      }
    }
    
    // 如果有错误，自动切换到第一个出错的节点
    if (hasError && firstErrorNodeId) {
      activeTabKey.value = firstErrorNodeId;
      const nodeName = templateContent.nodes.find(node => node.id === firstErrorNodeId)?.name || t('filingEdit.currentNode');
      message.warning(t('filingEdit.completeMissingFields', { nodeName }));
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('表单验证失败:', error);
    return false;
  }
};

// 初始化节点表单数据
const initNodeForms = () => {
  if (!templateContent.nodes) return;
  
  templateContent.nodes.forEach(node => {
    if (!nodeForms[node.id]) {
      nodeForms[node.id] = {};
    }
    
    // 初始化字段
    node.fields?.forEach(field => {
      if (field.type === 'checkbox') {
        // 如果已有值，不覆盖；否则初始化为空数组
        if (!nodeForms[node.id][field.id]) {
          nodeForms[node.id][field.id] = [];
        }
      } else if (field.type === 'date') {
        // 日期类型保持原样，不做特殊处理
        if (!nodeForms[node.id][field.id]) {
          nodeForms[node.id][field.id] = null;
        }
      } else {
        // 其他类型如果没有值，初始化为空字符串
        if (!nodeForms[node.id][field.id]) {
          nodeForms[node.id][field.id] = '';
        }
      }
    });
    
    // 初始化节点验证状态
    nodeValidationState[node.id] = 'wait';
  });

  // 如果有节点，设置第一个节点为当前活动节点
  if (templateContent.nodes && templateContent.nodes.length > 0) {
    activeTabKey.value = templateContent.nodes[0].id;
  }
  
  // 尝试初始验证所有表单，以更新完成状态
  setTimeout(() => {
    checkAllNodeStatus();
  }, 500);
};

// 检查所有节点状态
const checkAllNodeStatus = async () => {
  for (const nodeId in nodeForms) {
    await validateNodeForm(nodeId);
  }
};

// 检查当前是否正在执行任何操作
const isProcessing = computed(() => {
  return savingInProgress.value || submittingInProgress.value;
});

// 监听表单数据变化，自动验证当前节点
watch(activeTabKey, (newTabKey) => {
  if (newTabKey && formRefs[newTabKey] && !isReadonly.value) {
    setTimeout(() => {
      validateNodeForm(newTabKey);
    }, 500);
  }
});

// 获取备案信息
const fetchFilingInfo = async () => {
  if (!filingId) {
    message.error(t('filingEdit.filingIdRequired'));
    router.push("/user/filing-center");
    return;
  }

  try {
    loading.value = true;
    
    const response = await userTemplateAPI.getAppliedTemplateList({
      pageSize: 1,
      pageNum: 1,
      id: filingId
    });

    if (!response || response.code !== 200) {
      console.error('API返回错误状态:', response);
      message.error(t('filingEdit.getFilingInfoFailed') + (response?.message || t('filingEdit.apiError')));
      router.push("/user/filing-center");
      return;
    }
    
    let templateData = null;
    
    if (response.data?.content && Array.isArray(response.data.content) && response.data.content.length > 0) {
      templateData = response.data.content[0];
    } else if (response.data?.records && Array.isArray(response.data.records) && response.data.records.length > 0) {
      templateData = response.data.records[0];
    } else if (typeof response.data === 'object' && response.data.id) {
      templateData = response.data;
    }
    
    if (templateData && templateData.id) {
      Object.assign(templateInfo, templateData);
      
      // 获取模板内容
      await fetchTemplateContent();
    } else {
      console.error('未找到模板数据或格式不匹配:', response);
      console.error('响应数据详情:', {
        hasData: !!response.data,
        dataType: typeof response.data,
        hasContent: !!(response.data?.content),
        hasRecords: !!(response.data?.records),
        contentLength: response.data?.content?.length,
        recordsLength: response.data?.records?.length
      });
      message.error(t('filingEdit.filingNotFound'));
      router.push("/user/filing-center");
    }
  } catch (error) {
    console.error('获取备案信息失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error(t('filingEdit.getFilingInfoFailed') + ": " + (error.message || t('filingEdit.unknownError')));
    router.push("/user/filing-center");
  } finally {
    loading.value = false;
  }
};

// 获取模板内容
const fetchTemplateContent = async () => {
  if (!filingId) {
    console.warn('没有filingId，无法获取模板内容');
    return;
  }

  try {
    const response = await userTemplateAPI.getTemplateContent(filingId);

    if (!response || response.code !== 200) {
      console.error('模板内容API返回错误:', response);
      message.warning(t('filingEdit.getTemplateContentFailed'));
      return;
    }

    // 处理API响应的模板内容
    let contentData = null;
    
    try {
      if (response.data !== undefined) {
        if (typeof response.data === 'object' && response.data !== null) {
          contentData = response.data;
        } else {
          const dataStr = String(response.data).trim();
          if (dataStr.startsWith('{') && dataStr.endsWith('}')) {
            contentData = JSON.parse(dataStr);
          } else if (dataStr === "" || dataStr === "null" || dataStr === "{}") {
            contentData = null;
          } else {
            console.error('无法识别的数据格式:', dataStr);
            contentData = null;
          }
        }
      }
      
      // 如果用户内容为空或无效，尝试从模板定义获取结构
      if (!contentData || !contentData.nodes || !Array.isArray(contentData.nodes) || contentData.nodes.length === 0) {
        if (templateInfo.templateId) {
          try {
            const templateResponse = await userTemplateAPI.getTemplateDefinition(templateInfo.templateId);

            if (templateResponse && templateResponse.code === 200 && templateResponse.data?.templateContent) {
              let templateContentData;
              if (typeof templateResponse.data.templateContent === 'string') {
                templateContentData = JSON.parse(templateResponse.data.templateContent);
              } else {
                templateContentData = templateResponse.data.templateContent;
              }

              // 转换sections格式为nodes格式（确保与后端一致）
              if (templateContentData.sections && Array.isArray(templateContentData.sections)) {
                const nodes = templateContentData.sections.map((section, sectionIndex) => {
                  const nodeId = `node_${sectionIndex + 1}`;
                  const fields = [];

                  if (section.fields && Array.isArray(section.fields)) {
                    section.fields.forEach((fieldGroup, fieldIndex) => {
                      if (fieldGroup.row && Array.isArray(fieldGroup.row)) {
                        fieldGroup.row.forEach((field, rowIndex) => {
                          const fieldId = `${nodeId}_field_${fieldIndex}_${rowIndex}`;
                          const convertedType = convertFieldType(field.type);
                          const normalizedOptions = normalizeOptions(field.options || field.props?.options || []);
                          
                          fields.push({
                            id: fieldId,
                            label: field.label || t('filingEdit.defaultFieldName', { index: fields.length + 1 }),
                            type: convertedType,
                            required: field.required || false,
                            example: field.example || field.description || t('filingEdit.pleaseInputContent'),
                            guide: field.guide || field.description || '',
                            props: {
                              options: normalizedOptions
                            }
                          });
                        });
                      }
                    });
                  }

                  return {
                    id: nodeId,
                    name: section.name || t('filingEdit.defaultNodeName', { index: sectionIndex + 1 }),
                    fields: fields
                  };
                });

                contentData = {
                  nodes: nodes,
                  formData: {} // 空的表单数据，用户尚未填写
                };
              } else if (templateContentData.nodes && Array.isArray(templateContentData.nodes)) {
                // 已经是nodes格式，但需要规范化options
                const normalizedNodes = templateContentData.nodes.map(node => ({
                  ...node,
                  fields: (node.fields || []).map(field => ({
                    ...field,
                    props: {
                      ...field.props,
                      options: normalizeOptions(field.props?.options || [])
                    }
                  }))
                }));
                
                contentData = {
                  nodes: normalizedNodes,
                  formData: {} // 空的表单数据
                };
              } else {
                console.warn('模板定义格式不识别:', templateContentData);
                contentData = { nodes: [], formData: {} };
              }
            } else {
              console.error('获取模板定义失败:', templateResponse);
              contentData = { nodes: [], formData: {} };
            }
          } catch (templateError) {
            console.error('获取模板定义异常:', templateError);
            contentData = { nodes: [], formData: {} };
          }
        } else {
          console.warn('没有找到templateId，无法获取模板定义');
          contentData = { nodes: [], formData: {} };
        }
      }

      // 更新模板内容和表单数据
      if (contentData && contentData.nodes && Array.isArray(contentData.nodes)) {
        templateContent.nodes = contentData.nodes;

        // 初始化表单数据
        templateContent.nodes.forEach(node => {
          nodeForms[node.id] = {};

          // 如果有已保存的表单数据，则填充
          if (contentData.formData && contentData.formData[node.id]) {
            Object.assign(nodeForms[node.id], contentData.formData[node.id]);
          }
        });

        // 设置第一个节点为活动标签
        if (templateContent.nodes.length > 0) {
          activeTabKey.value = templateContent.nodes[0].id;
        }

        // 初始化表单数据结构
        initNodeForms();
      } else {
        console.error('最终内容数据无效:', contentData);
        templateContent.nodes = [];
      }
    } catch (parseError) {
      console.error('解析模板内容失败:', parseError);
      console.error('原始数据:', response.data);
      templateContent.nodes = [];
    }
  } catch (error) {
    console.error('获取模板内容失败:', error);
    message.error(t('filingEdit.getTemplateContentFailed') + ': ' + (error.message || t('filingEdit.unknownError')));
    templateContent.nodes = [];
  }
};

// 字段类型转换函数
const convertFieldType = (type) => {
  const typeMap = {
    'text': 'input',
    'textarea': 'textarea', 
    'richtext': 'richtext',
    'select': 'select',
    'date': 'date',
    'checkbox': 'checkbox',
    'radio': 'radio'
  };
  return typeMap[type] || 'input';
};

// 选项规范化函数
const normalizeOptions = (options) => {
  if (!options) return [];
  
  if (Array.isArray(options)) {
    return options.map(option => {
      if (typeof option === 'string') {
        return { value: option, label: option };
      } else if (typeof option === 'object' && option !== null) {
        return { 
          value: option.value !== undefined ? option.value : option.key || option.label,
          label: option.label !== undefined ? option.label : option.value || option.key
        };
      }
      return null;
    }).filter(option => option != null);
  }
  
  return [];
};

// 安全获取选择器选项
const getSelectOptions = (field) => {
  if (!field || !field.props) return [];
  
  const options = field.props.options;
  if (!options) return [];
  
  // 如果是数组
  if (Array.isArray(options)) {
    return options.filter(option => option != null); // 过滤掉null和undefined
  }
  
  return [];
};

// 安全获取选项的key
const getOptionKey = (option) => {
  if (typeof option === 'string') {
    return option;
  }
  if (option && typeof option === 'object') {
    return option.value !== undefined ? option.value : option.key || option.label || String(option);
  }
  return String(option);
};

// 安全获取选项的value
const getOptionValue = (option) => {
  if (typeof option === 'string') {
    return option;
  }
  if (option && typeof option === 'object') {
    return option.value !== undefined ? option.value : option.key || option.label;
  }
  return option;
};

// 安全获取选项的label
const getOptionLabel = (option) => {
  if (typeof option === 'string') {
    return option;
  }
  if (option && typeof option === 'object') {
    return option.label !== undefined ? option.label : option.value || option.key || String(option);
  }
  return String(option);
};

// 提交审核
const submitForReview = async () => {
  try {
    // 先保存当前数据 - 确保不会双重编码JSON
    const contentToSave = {
      nodes: templateContent.nodes,
      formData: nodeForms
    };

    // 保存数据
    const saveResponse = await userTemplateAPI.saveTemplateContent(filingId, contentToSave);
    
    if (!saveResponse || saveResponse.code !== 200) {
      throw new Error(saveResponse?.message || t('filingEdit.saveFailed'));
    }

    // 提交审核
    try {
      const reviewResponse = await userTemplateAPI.updateTemplateStatus(
        filingId,
        5, // 状态：审核中
        t('filingEdit.userSubmitReview')
      );

      if (reviewResponse && reviewResponse.code === 200) {
        message.success(t('filingEdit.submitSuccess'));
        
        // 刷新页面状态
        await fetchFilingInfo();
      } else {
        throw new Error(reviewResponse?.message || t('filingEdit.submitFailed'));
      }
    } catch (reviewError) {
      console.error('提交审核失败:', reviewError);
      if (reviewError.response) {
        console.error('错误状态码:', reviewError.response.status);
        console.error('错误详情:', reviewError.response.data);
      }
      throw reviewError;
    }
  } catch (error) {
    console.error('提交失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error(t('filingEdit.submitFailed') + ': ' + (error.message || t('filingEdit.unknownError')));
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchFilingInfo();
});
</script>

<style scoped>
.filing-edit-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 24px;
}

.filing-edit {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: relative;
  padding-bottom: 80px; /* 为底部按钮留出空间 */
  width: 100%;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px 24px 0;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.submission-info {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin: 0;
}

.info-card {
  margin: 0 24px 24px;
}

.progress-card {
  margin: 0 24px 24px;
}

.progress-title {
  font-size: 16px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 16px;
}

.node-progress {
  margin-bottom: 8px;
}

:deep(.ant-steps-item) {
  cursor: pointer;
}

:deep(.ant-steps-item-title) {
  transition: color 0.3s;
}

:deep(.ant-steps-item:hover .ant-steps-item-title) {
  color: #1890ff;
}

.content-card {
  margin: 0 24px 24px;
}

.empty-content {
  margin: 0 24px 24px;
  padding: 48px;
  text-align: center;
  background: #fff;
  border-radius: 2px;
}

.node-content {
  padding: 24px;
  background: #fff;
  border-radius: 2px;
}

.footer-actions {
  padding: 20px 0;
  text-align: center;
  margin-top: 24px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  width: 100%;
  position: absolute;
  bottom: 0;
  left: 0;
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

:deep(.ant-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.ant-checkbox-wrapper) {
  margin-right: 0;
  white-space: nowrap;
}

:deep(.ant-form-item-extra) {
  color: #8c8c8c;
  font-size: 13px;
  font-style: italic;
}

/* 禁用状态样式 */
:deep(.ant-form-item-control-input-content .ant-input[disabled]),
:deep(.ant-form-item-control-input-content .ant-select-disabled),
:deep(.ant-form-item-control-input-content .ant-picker-disabled),
:deep(.ant-form-item-control-input-content .ant-checkbox-disabled + span),
:deep(.ant-form-item-control-input-content .ant-radio-disabled + span),
:deep(.ant-form-item-control-input-content .ant-textarea[disabled]) {
  color: rgba(0, 0, 0, 0.65);
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 1;
}

/* 修改滚动条样式 */
::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 成功和错误状态的节点样式 */
:deep(.ant-steps-item-finish .ant-steps-item-icon) {
  background-color: #52c41a;
  border-color: #52c41a;
}

:deep(.ant-steps-item-error .ant-steps-item-icon) {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
}

:deep(.ant-steps-item-error .ant-steps-item-title) {
  color: #ff4d4f;
}

/* 提交按钮样式 */
.footer-actions :deep(.ant-btn-primary) {
  height: 40px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 表单验证提示样式 */
:deep(.ant-form-item-has-error .ant-form-item-explain) {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style> 