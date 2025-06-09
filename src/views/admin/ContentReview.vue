<template>
  <div class="content-review-container">
    <div class="content-review">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">{{ $t("contentReview.title") }}</h2>
          <div class="user-info">
            <a-tag color="blue">{{ $t("contentReview.user") }}：{{ recordInfo.userName }}</a-tag>
            <a-tag color="green">{{ $t("contentReview.template") }}：{{ recordInfo.templateName }}</a-tag>
          </div>
        </div>
        <div class="actions-section">
          <a-space>
            <a-button @click="goBack">
              <template #icon><ArrowLeftOutlined /></template>
              {{ $t("contentReview.backToList") }}
            </a-button>
          </a-space>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-card class="info-card">
          <a-descriptions :title="$t('contentReview.applicationInfo')" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item :label="$t('contentReview.applicationId')">{{ recordInfo.id || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.templateCode')">{{ recordInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.templateName')">{{ recordInfo.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.applicantUser')">{{ recordInfo.userName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.applicationTime')">{{ recordInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.status')">
              <a-tag :color="getStatusColor(recordInfo.status)">
                {{ $t(`status.${recordInfo.status}`) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.lastUpdate')">{{ recordInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('contentReview.remarks')" :span="2">{{ formatRemarkForDisplay(recordInfo.remarks) || $t('contentReview.noRemarks') }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card class="content-card" v-if="!loading && templateContent.nodes && templateContent.nodes.length > 0">
          <template #title>
            <div class="content-title">
              <span>{{ $t("contentReview.userSubmittedContent") }}</span>
              <a-tag color="processing">{{ $t("contentReview.totalNodes", { count: templateContent.nodes?.length || 0 }) }}</a-tag>
            </div>
          </template>
          
          <a-tabs v-model:activeKey="activeTabKey">
            <a-tab-pane 
              v-for="node in templateContent.nodes" 
              :key="node.id"
              :tab="node.name"
            >
              <div class="node-content">
                <a-form
                  :model="nodeForms[node.id] || {}"
                  layout="vertical"
                  disabled
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
                      >
                        <template #extra>{{ field.guide }}</template>
                        
                        <template v-if="field.type === 'checkbox' && field.id === 'application_fields'">
                          <div class="application-fields-container">
                            <a-checkbox-group :value="nodeForms[node.id]?.[field.id] || []" disabled>
                              <div 
                                v-for="category in field.props?.options || []" 
                                :key="category.value"
                                class="category-group"
                              >
                                <div class="category-title">{{ category.label }}</div>
                                <div class="category-options">
                                  <a-checkbox 
                                    v-for="child in category.children || []" 
                                    :key="child.value"
                                    :value="child.value"
                                    disabled
                                  >
                                    {{ child.label }}
                                  </a-checkbox>
                                </div>
                              </div>
                            </a-checkbox-group>
                          </div>
                        </template>
                        
                        <template v-else-if="field.type === 'checkbox'">
                          <a-checkbox-group :value="nodeForms[node.id]?.[field.id] || []" disabled>
                            <a-checkbox 
                              v-for="option in getSelectOptions(field)" 
                              :key="getOptionKey(option)" 
                              :value="getOptionValue(option)"
                              disabled
                            >
                              {{ getOptionLabel(option) }}
                            </a-checkbox>
                          </a-checkbox-group>
                        </template>

                        <template v-else-if="field.type === 'radio'">
                          <a-radio-group :value="nodeForms[node.id]?.[field.id] || ''" disabled>
                            <a-radio 
                              v-for="option in getSelectOptions(field)" 
                              :key="getOptionKey(option)" 
                              :value="getOptionValue(option)"
                              disabled
                            >
                              {{ getOptionLabel(option) }}
                            </a-radio>
                          </a-radio-group>
                        </template>
                        
                        <template v-else-if="field.type === 'input'">
                          <a-input
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || $t('contentReview.pleaseInput', { field: field.label })"
                            disabled
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'select'">
                          <a-select
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || $t('contentReview.pleaseSelect', { field: field.label })"
                            disabled
                          >
                            <a-select-option 
                              v-for="option in field.props?.options || []" 
                              :key="option.value" 
                              :value="option.value"
                            >
                              {{ option.label }}
                            </a-select-option>
                          </a-select>
                        </template>
                        
                        <template v-else-if="field.type === 'date'">
                          <a-date-picker
                            :value="nodeForms[node.id]?.[field.id]"
                            :placeholder="field.example || $t('contentReview.pleaseSelect', { field: field.label })"
                            style="width: 100%"
                            disabled
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'textarea'">
                          <a-textarea
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || $t('contentReview.pleaseInput', { field: field.label })"
                            :rows="4"
                            disabled
                          />
                        </template>
                        
                        <!-- 显示字段值是否已填写 -->
                        <div class="field-status" v-if="nodeForms[node.id]?.[field.id]">
                          <a-tag color="success" size="small">{{ $t("contentReview.filled") }}</a-tag>
                        </div>
                        <div class="field-status" v-else>
                          <a-tag color="default" size="small">{{ $t("contentReview.notFilled") }}</a-tag>
                        </div>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>

        <div v-if="!loading && (!templateContent.nodes || templateContent.nodes.length === 0)" class="empty-content">
          <a-empty :description="$t('contentReview.noContentData')">
            <template #description>
              <p>{{ $t("contentReview.userNotSubmitted") }}</p>
            </template>
          </a-empty>
        </div>

        <div class="review-section" v-if="!loading && canReviewContent(recordInfo.status)">
          <a-card :title="$t('contentReview.reviewAction')">
            <a-form :model="reviewForm" layout="vertical">
              <a-form-item :label="$t('contentReview.reviewResult')" required>
                <a-radio-group v-model:value="reviewForm.status">
                  <a-radio :value="6">{{ $t("contentReview.approveReview") }}</a-radio>
                  <a-radio :value="7">{{ $t("contentReview.returnForRevision") }}</a-radio>
                </a-radio-group>
              </a-form-item>
              <a-form-item :label="$t('contentReview.reviewComment')">
                <a-textarea 
                  v-model:value="reviewForm.remarks" 
                  :placeholder="$t('contentReview.pleaseInputReviewComment')"
                  :rows="4"
                />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button 
                    type="primary" 
                    @click="handleReview"
                    :loading="submittingReview"
                    :disabled="!reviewForm.status"
                  >
                    <template #icon><CheckOutlined /></template>
                    {{ reviewForm.status === 6 ? $t("contentReview.approveReview") : $t("contentReview.returnForRevision") }}
                  </a-button>
                  <a-button @click="resetReviewForm">
                    {{ $t("contentReview.reset") }}
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <div class="readonly-notice" v-else-if="!loading">
          <a-alert 
            :message="$t('contentReview.notice')" 
            :description="getReadonlyReason(recordInfo.status)"
            type="info" 
            show-icon 
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { userTemplateAPI } from '@/api';
import { formatRemarkForDisplay } from '@/utils/remarkUtils';
import {
ArrowLeftOutlined,
CheckOutlined
} from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const recordId = route.query.id;

const loading = ref(true);
const submittingReview = ref(false);
const activeTabKey = ref('');

// 记录信息
const recordInfo = reactive({
  id: '',
  templateId: '',
  templateCode: '',
  templateName: '',
  userName: '',
  createTime: '',
  updateTime: '',
  status: '',
  statusDesc: '',
  remarks: ''
});

// 模板内容
const templateContent = reactive({
  nodes: []
});

// 节点表单数据
const nodeForms = reactive({});

// 审核表单
const reviewForm = reactive({
  status: 6, // 默认通过
  remarks: ''
});

// 获取状态文本
const getStatusText = (status) => {
  return t(`status.${Number(status)}`);
};

// 获取状态颜色
const getStatusColor = (status) => {
  const statusMap = {
    0: 'default',   // 待审核
    1: 'green',     // 申请通过
    2: 'red',       // 拒绝申请
    3: 'orange',    // 待填写
    4: 'purple',    // 填写中
    5: 'processing', // 审核中
    6: 'success',   // 审核通过
    7: 'error',     // 退回
  };
  return statusMap[Number(status)] || 'default';
};

// 检查是否可以进行内容审核
const canReviewContent = (status) => {
  return Number(status) === 5; // 只有审核中状态才能审核
};

// 获取只读原因
const getReadonlyReason = (status) => {
  const statusNum = Number(status);
  if (statusNum === 6) {
    return t('contentReview.alreadyApproved');
  } else if (statusNum === 7) {
    return t('contentReview.alreadyReturned');
  } else if (statusNum < 5) {
    return t('contentReview.waitingForSubmission');
  } else {
    return t('contentReview.statusNotSupported');
  }
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

// 返回上一页
const goBack = () => {
  router.push('/admin/task-board');
};

// 重置审核表单
const resetReviewForm = () => {
  reviewForm.status = 6;
  reviewForm.remarks = '';
};

// 获取记录详情
const fetchRecordInfo = async () => {
  if (!recordId) {
    message.error(t('contentReview.recordIdCannotBeEmpty'));
    goBack();
    return;
  }

  try {
    // 获取记录基本信息
    const response = await userTemplateAPI.getAppliedTemplateList({
      pageSize: 1,
      pageNum: 1,
      id: recordId
    });

    if (!response || response.code !== 200) {
      console.error('API返回错误状态:', response);
      message.error(t('contentReview.failedToGetRecordInfo') + (response?.message || t('contentReview.apiReturnError')));
      goBack();
      return;
    }
    
    let recordData = null;
    if (response.data?.content && Array.isArray(response.data.content) && response.data.content.length > 0) {
      recordData = response.data.content[0];
    } else if (response.data?.records && Array.isArray(response.data.records) && response.data.records.length > 0) {
      recordData = response.data.records[0];
    } else if (typeof response.data === 'object' && response.data.id) {
      recordData = response.data;
    }
    
    if (recordData && recordData.id) {
      Object.assign(recordInfo, recordData);
      
      // 获取模板内容
      await fetchTemplateContent();
    } else {
      console.error('未找到记录数据:', response);
      message.error(t('contentReview.recordNotFound'));
      goBack();
    }
  } catch (error) {
    console.error('获取记录信息失败:', error);
    message.error(t('contentReview.failedToGetRecordInfo') + (error.message || t('contentReview.unknownError')));
    goBack();
  } finally {
    loading.value = false;
  }
};

// 获取模板内容
const fetchTemplateContent = async () => {
  if (!recordId) {
    console.warn('没有recordId，无法获取模板内容');
    return;
  }

  try {
    const response = await userTemplateAPI.getTemplateContent(recordId);

    if (response && response.code === 200) {
      // 解析用户填写的内容
      if (response.data && typeof response.data === 'string' && response.data.trim()) {
        try {
          const contentData = JSON.parse(response.data);
          if (contentData && contentData.nodes && Array.isArray(contentData.nodes)) {
            templateContent.nodes = contentData.nodes;
            
            // 关键修复：设置用户填写的表单数据
            if (contentData.formData && typeof contentData.formData === 'object') {
              // 清空现有的nodeForms
              Object.keys(nodeForms).forEach(key => {
                delete nodeForms[key];
              });
              
              // 设置用户填写的表单数据
              Object.assign(nodeForms, contentData.formData);
            }
            
            // 设置第一个节点为活动标签
            if (templateContent.nodes.length > 0) {
              activeTabKey.value = templateContent.nodes[0].id;
            }
            
            return;
          }
        } catch (parseError) {
          console.warn('用户内容解析失败，可能未填写内容:', parseError);
        }
      }
      
      // 如果用户内容为空，尝试从模板定义获取结构
      if (!recordInfo.templateId) {
        console.warn('没有找到templateId，无法获取模板定义');
        return;
      }
      
      const templateResponse = await userTemplateAPI.getTemplateDefinition(recordInfo.templateId);
      
      if (templateResponse && templateResponse.code === 200 && templateResponse.data?.templateContent) {
        try {
          const templateContentData = typeof templateResponse.data.templateContent === 'string' 
            ? JSON.parse(templateResponse.data.templateContent) 
            : templateResponse.data.templateContent;
          
          let contentData = { nodes: [] };
          
          // 处理sections格式转nodes格式
          if (templateContentData.sections && Array.isArray(templateContentData.sections)) {
            contentData.nodes = templateContentData.sections.map((section, index) => ({
              id: section.id || `node_${index}`,
              name: section.name || `节点${index + 1}`,
              fields: []
            }));
            
            templateContentData.sections.forEach((section, sectionIndex) => {
              if (section.fields && Array.isArray(section.fields)) {
                section.fields.forEach((fieldGroup) => {
                  if (fieldGroup.row && Array.isArray(fieldGroup.row)) {
                    fieldGroup.row.forEach((field) => {
                      contentData.nodes[sectionIndex].fields.push({
                        id: field.id || `field_${Date.now()}_${Math.random()}`,
                        label: field.label || '未命名字段',
                        type: field.type || 'text',
                        required: field.required || false,
                        example: field.example || field.guide || '',
                        props: field.props || {}
                      });
                    });
                  }
                });
              }
            });
          } else if (templateContentData.nodes && Array.isArray(templateContentData.nodes)) {
            contentData = templateContentData;
          }
          
          templateContent.nodes = contentData.nodes || [];
          
          // 初始化空的表单数据（用户未填写）
          templateContent.nodes.forEach(node => {
            if (!nodeForms[node.id]) {
              nodeForms[node.id] = {};
            }
          });
          
          // 设置第一个节点为活动标签
          if (templateContent.nodes.length > 0) {
            activeTabKey.value = templateContent.nodes[0].id;
          }
          
        } catch (templateParseError) {
          console.error('解析模板定义失败:', templateParseError);
          message.warning(t('contentReview.unableToGetTemplateStructure'));
        }
      } else {
        console.error('获取模板定义异常:', templateResponse);
        message.warning(t('contentReview.unableToGetTemplateStructure'));
      }
    } else {
      console.error('模板内容API返回错误:', response);
      message.warning(t('contentReview.failedToGetTemplateContent'));
    }
  } catch (error) {
    console.error('获取模板内容失败:', error);
    message.warning(t('contentReview.failedToGetTemplateContent'));
  }
};

// 处理审核
const handleReview = async () => {
  if (!reviewForm.status) {
    message.warning(t('contentReview.pleaseSelectReviewResult'));
    return;
  }
  
  const statusText = reviewForm.status === 6 ? t('contentReview.approveReview') : t('contentReview.returnForRevision');
  const isApproval = reviewForm.status === 6; // 判断是否为批准操作
  
  Modal.confirm({
    title: t('contentReview.confirmReview'),
    content: t('contentReview.confirmReviewAction', { action: statusText }),
    okText: t('contentReview.confirm'),
    cancelText: t('contentReview.cancel'),
    onOk: async () => {
      submittingReview.value = true;
      try {
        const response = await userTemplateAPI.updateTemplateStatus(
          recordId,
          reviewForm.status,
          reviewForm.remarks || (reviewForm.status === 7 ? t('contentReview.returnForRevision') : t('contentReview.approveReview'))
        );
        
        if (response && response.code === 200) {
          message.success(t('contentReview.reviewSuccessMessage', { action: statusText }));
          
          // 如果是批准操作，延迟1.5秒后跳转到任务看板
          if (isApproval) {
            setTimeout(() => {
              router.push('/admin/task-board');
            }, 1500);
          } else {
            // 如果是退回操作，重新获取记录信息以更新状态
            await fetchRecordInfo();
            // 重置审核表单
            resetReviewForm();
          }
        } else {
          throw new Error(response?.message || t('contentReview.reviewFailed'));
        }
      } catch (error) {
        console.error('审核失败:', error);
        message.error(t('contentReview.reviewFailed') + (error.message || t('contentReview.unknownError')));
      } finally {
        submittingReview.value = false;
      }
    }
  });
};

// 组件挂载时获取数据
onMounted(() => {
  fetchRecordInfo();
});
</script>

<style scoped>
.content-review-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 24px;
}

.content-review {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
  flex: 1;
}

.user-info {
  display: flex;
  gap: 8px;
}

.actions-section {
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin: 0;
}

.info-card,
.content-card,
.review-section {
  margin: 0 24px 24px;
}

.content-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-content {
  margin: 0 24px 24px;
  padding: 48px;
  text-align: center;
  background: #fff;
  border-radius: 2px;
}

.readonly-notice {
  margin: 0 24px 24px;
}

.node-content {
  padding: 24px;
  background: #fff;
  border-radius: 2px;
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

.field-status {
  margin-top: 4px;
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

/* 审核区域样式 */
.review-section :deep(.ant-card-head-title) {
  color: #1890ff;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .title-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .user-info {
    flex-wrap: wrap;
  }
}
</style> 