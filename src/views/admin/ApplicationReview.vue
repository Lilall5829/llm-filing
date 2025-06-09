<template>
  <div class="application-review-container">
    <div class="application-review">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">{{ $t("applicationReview.title") }}</h2>
          <div class="user-info">
            <a-tag color="blue">{{ $t("applicationReview.user") }}：{{ recordInfo.userName }}</a-tag>
            <a-tag color="green">{{ $t("applicationReview.template") }}：{{ recordInfo.templateName }}</a-tag>
          </div>
        </div>
        <div class="actions-section">
          <a-space>
            <a-button @click="goBack">
              <template #icon><ArrowLeftOutlined /></template>
              {{ $t("applicationReview.backToList") }}
            </a-button>
          </a-space>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-card class="info-card">
          <a-descriptions :title="$t('applicationReview.applicationInfo')" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item :label="$t('applicationReview.applicationId')">{{ recordInfo.id || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.templateCode')">{{ recordInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.templateName')">{{ recordInfo.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.applicantUser')">{{ recordInfo.userName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.applicationTime')">{{ recordInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.status')">
              <a-tag :color="getStatusColor(recordInfo.status)">
                {{ $t(`status.${recordInfo.status}`) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.lastUpdate')">{{ recordInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.remarks')" :span="2">
              {{ formatRemarkForDisplay(recordInfo.remarks) || $t('applicationReview.noRemarks') }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card class="template-info-card" v-if="!loading && templateInfo.templateContent">
          <template #title>
            <div class="template-title">
              <span>{{ $t("applicationReview.templateInfoPreview") }}</span>
              <a-tag color="processing">{{ templateInfo.templateName }}</a-tag>
            </div>
          </template>
          
          <a-descriptions bordered :column="{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }">
            <a-descriptions-item :label="$t('applicationReview.templateCode')">{{ templateInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.templateType')">{{ templateInfo.templateType || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.createTime')">{{ templateInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.lastUpdate')">{{ templateInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('applicationReview.templateDescription')" :span="2">{{ templateInfo.templateDescription || $t('applicationReview.noDescription') }}</a-descriptions-item>
          </a-descriptions>

          <a-divider>{{ $t("applicationReview.templateStructure") }}</a-divider>
          
          <div class="template-structure" v-if="templateStructure.length > 0">
            <a-collapse v-model:activeKey="activeKeys" ghost>
              <a-collapse-panel 
                v-for="(section, index) in templateStructure" 
                :key="index"
                :header="$t('applicationReview.sectionHeader', { name: section.name, count: section.fieldCount })"
              >
                <a-table 
                  :columns="fieldColumns" 
                  :data-source="section.fields" 
                  :pagination="false"
                  size="small"
                />
              </a-collapse-panel>
            </a-collapse>
          </div>
          
          <div v-else class="empty-structure">
            <a-empty :description="$t('applicationReview.noTemplateStructure')" />
          </div>
        </a-card>

        <div class="review-section" v-if="!loading && canReviewApplication(recordInfo.status)">
          <a-card :title="$t('applicationReview.reviewAction')">
            <a-alert 
              :message="$t('applicationReview.reviewInstructions')" 
              :description="$t('applicationReview.reviewInstructionsDescription')"
              type="info" 
              show-icon 
              style="margin-bottom: 24px;"
            />
            
            <a-form :model="reviewForm" layout="vertical">
              <a-form-item :label="$t('applicationReview.reviewResult')" required>
                <a-radio-group v-model:value="reviewForm.status">
                  <a-radio :value="3">{{ $t("applicationReview.approveApplication") }}</a-radio>
                  <a-radio :value="2">{{ $t("applicationReview.rejectApplication") }}</a-radio>
                </a-radio-group>
              </a-form-item>
              <a-form-item :label="$t('applicationReview.reviewComment')">
                <a-textarea 
                  v-model:value="reviewForm.remarks" 
                  :placeholder="$t('applicationReview.pleaseInputReviewComment')"
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
                    {{ reviewForm.status === 3 ? $t("applicationReview.approveApplication") : $t("applicationReview.rejectApplication") }}
                  </a-button>
                  <a-button @click="resetReviewForm">
                    {{ $t("applicationReview.reset") }}
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <div class="readonly-notice" v-else-if="!loading">
          <a-alert 
            :message="$t('applicationReview.notice')" 
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const recordId = route.query.id;

const loading = ref(true);
const submittingReview = ref(false);
const activeKeys = ref([0]); // 默认展开第一个面板

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

// 模板信息
const templateInfo = reactive({
  templateCode: '',
  templateName: '',
  templateType: '',
  templateDescription: '',
  templateContent: '',
  createTime: '',
  updateTime: ''
});

// 模板结构
const templateStructure = ref([]);

// 审核表单
const reviewForm = reactive({
  status: 3, // 默认通过
  remarks: ''
});

// 字段表格列定义
const fieldColumns = computed(() => [
  {
    title: t('applicationReview.fieldName'),
    dataIndex: 'label',
    key: 'label',
  },
  {
    title: t('applicationReview.fieldType'),
    dataIndex: 'type',
    key: 'type',
  },
  {
    title: t('applicationReview.isRequired'),
    dataIndex: 'required',
    key: 'required',
    render: (required) => required ? t('applicationReview.yes') : t('applicationReview.no')
  },
  {
    title: t('applicationReview.exampleDescription'),
    dataIndex: 'example',
    key: 'example',
  }
]);

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

// 检查是否可以进行申请审核
const canReviewApplication = (status) => {
  return Number(status) === 0; // 只有待审核状态才能审核
};

// 获取只读原因
const getReadonlyReason = (status) => {
  const statusNum = Number(status);
  if (statusNum === 1 || statusNum === 3) {
    return t('applicationReview.alreadyApproved');
  } else if (statusNum === 2) {
    return t('applicationReview.alreadyRejected');
  } else if (statusNum > 3) {
    return t('applicationReview.userStartedFilling');
  } else {
    return t('applicationReview.statusNotSupported');
  }
};

// 返回上一页
const goBack = () => {
  router.push('/admin/task-board');
};

// 重置审核表单
const resetReviewForm = () => {
  reviewForm.status = 3;
  reviewForm.remarks = '';
};

// 解析模板结构
const parseTemplateStructure = (templateContent) => {
  try {
    let contentData;
    if (typeof templateContent === 'string') {
      contentData = JSON.parse(templateContent);
    } else {
      contentData = templateContent;
    }
    
    const structure = [];
    
    if (contentData.sections && Array.isArray(contentData.sections)) {
      contentData.sections.forEach((section, index) => {
        const fields = [];
        let fieldCount = 0;
        
        if (section.fields && Array.isArray(section.fields)) {
          section.fields.forEach((fieldGroup) => {
            if (fieldGroup.row && Array.isArray(fieldGroup.row)) {
              fieldGroup.row.forEach((field) => {
                fields.push({
                  label: field.label || t('applicationReview.unnamedField'),
                  type: field.type || 'text',
                  required: field.required || false,
                  example: field.example || field.guide || '-'
                });
                fieldCount++;
              });
            }
          });
        }
        
        structure.push({
          name: section.name || t('applicationReview.node', { index: index + 1 }),
          fields: fields,
          fieldCount: fieldCount
        });
      });
    } else if (contentData.nodes && Array.isArray(contentData.nodes)) {
      contentData.nodes.forEach((node) => {
        const fields = [];
        let fieldCount = 0;
        
        if (node.fields && Array.isArray(node.fields)) {
          node.fields.forEach((field) => {
            fields.push({
              label: field.label || t('applicationReview.unnamedField'),
              type: field.type || 'text',
              required: field.required || false,
              example: field.example || field.guide || '-'
            });
            fieldCount++;
          });
        }
        
        structure.push({
          name: node.name || t('applicationReview.unnamedNode'),
          fields: fields,
          fieldCount: fieldCount
        });
      });
    }
    
    return structure;
  } catch (error) {
    console.error('解析模板结构失败:', error);
    return [];
  }
};

// 获取记录详情
const fetchRecordInfo = async () => {
  if (!recordId) {
    message.error(t('applicationReview.recordIdCannotBeEmpty'));
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
      message.error(t('applicationReview.failedToGetRecordInfo') + (response?.message || t('applicationReview.apiReturnError')));
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
      
      // 获取模板定义信息
      await fetchTemplateInfo();
    } else {
      console.error('未找到记录数据:', response);
      message.error(t('applicationReview.recordNotFound'));
      goBack();
    }
  } catch (error) {
    console.error('获取记录信息失败:', error);
    message.error(t('applicationReview.failedToGetRecordInfo') + (error.message || t('applicationReview.unknownError')));
    goBack();
  } finally {
    loading.value = false;
  }
};

// 获取模板信息
const fetchTemplateInfo = async () => {
  if (!recordInfo.templateId) {
    console.warn('没有模板ID，跳过获取模板信息');
    return;
  }

  try {
    const response = await userTemplateAPI.getTemplateDefinition(recordInfo.templateId);
    
    if (response && response.code === 200 && response.data) {
      Object.assign(templateInfo, response.data);
      
      // 解析模板结构
      if (templateInfo.templateContent) {
        templateStructure.value = parseTemplateStructure(templateInfo.templateContent);
      }
    } else {
      console.warn('获取模板信息失败:', response);
      message.warning(t('applicationReview.unableToGetTemplateInfo'));
    }
  } catch (error) {
    console.error('获取模板信息失败:', error);
    message.warning(t('applicationReview.failedToGetTemplateInfo') + (error.message || t('applicationReview.unknownError')));
  }
};

// 处理审核
const handleReview = async () => {
  if (!reviewForm.status) {
    message.warning(t('applicationReview.pleaseSelectReviewResult'));
    return;
  }
  
  const statusText = reviewForm.status === 3 ? t('applicationReview.approveApplication') : t('applicationReview.rejectApplication');
  
  Modal.confirm({
    title: t('applicationReview.confirmReview'),
    content: t('applicationReview.confirmReviewAction', { action: statusText }),
    okText: t('applicationReview.confirm'),
    cancelText: t('applicationReview.cancel'),
    onOk: async () => {
      submittingReview.value = true;
      try {
        const response = await userTemplateAPI.updateTemplateStatus(
          recordId,
          reviewForm.status,
          reviewForm.remarks || (reviewForm.status === 2 ? t('applicationReview.rejectApplication') : t('applicationReview.approveApplicationSetWaitingStatus'))
        );
        
        if (response && response.code === 200) {
          message.success(t('applicationReview.reviewSuccessMessage', { action: statusText }));
          // 重新获取记录信息以更新状态
          await fetchRecordInfo();
          // 重置审核表单
          resetReviewForm();
        } else {
          throw new Error(response?.message || t('applicationReview.reviewFailed'));
        }
      } catch (error) {
        console.error('审核失败:', error);
        message.error(t('applicationReview.reviewFailed') + (error.message || t('applicationReview.unknownError')));
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
.application-review-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 24px;
}

.application-review {
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
.template-info-card,
.review-section {
  margin: 0 24px 24px;
}

.template-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-structure {
  margin-top: 16px;
}

.empty-structure {
  margin: 24px 0;
  text-align: center;
}

.readonly-notice {
  margin: 0 24px 24px;
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