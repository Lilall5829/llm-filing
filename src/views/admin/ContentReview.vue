<template>
  <div class="content-review-container">
    <div class="content-review">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">内容审核</h2>
          <div class="user-info">
            <a-tag color="blue">用户：{{ recordInfo.userName }}</a-tag>
            <a-tag color="green">模板：{{ recordInfo.templateName }}</a-tag>
          </div>
        </div>
        <div class="actions-section">
          <a-space>
            <a-button @click="goBack">
              <template #icon><ArrowLeftOutlined /></template>
              返回列表
            </a-button>
          </a-space>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-card class="info-card">
          <a-descriptions title="申请信息" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item label="申请ID">{{ recordInfo.id || '-' }}</a-descriptions-item>
            <a-descriptions-item label="模板编号">{{ recordInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item label="模板名称">{{ recordInfo.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="申请用户">{{ recordInfo.userName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="申请时间">{{ recordInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(recordInfo.status)">
                {{ recordInfo.statusDesc || getStatusText(recordInfo.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="最后更新">{{ recordInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ recordInfo.remarks || '无' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card class="content-card" v-if="!loading && templateContent.nodes && templateContent.nodes.length > 0">
          <template #title>
            <div class="content-title">
              <span>用户提交的内容</span>
              <a-tag color="processing">共 {{ templateContent.nodes?.length || 0 }} 个节点</a-tag>
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
                        
                        <template v-else-if="field.type === 'input'">
                          <a-input
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || `请输入${field.label}`"
                            disabled
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'select'">
                          <a-select
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || `请选择${field.label}`"
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
                            :placeholder="field.example || `请选择${field.label}`"
                            style="width: 100%"
                            disabled
                          />
                        </template>
                        
                        <template v-else-if="field.type === 'textarea'">
                          <a-textarea
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || `请输入${field.label}`"
                            :rows="4"
                            disabled
                          />
                        </template>
                        
                        <template v-else>
                          <a-input
                            :value="nodeForms[node.id]?.[field.id] || ''"
                            :placeholder="field.example || `请输入${field.label}`"
                            disabled
                          />
                        </template>
                        
                        <!-- 显示字段值是否已填写 -->
                        <div class="field-status" v-if="nodeForms[node.id]?.[field.id]">
                          <a-tag color="success" size="small">已填写</a-tag>
                        </div>
                        <div class="field-status" v-else>
                          <a-tag color="default" size="small">未填写</a-tag>
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
          <a-empty description="暂无内容数据">
            <template #description>
              <p>用户尚未提交任何内容。</p>
            </template>
          </a-empty>
        </div>

        <div class="review-section" v-if="!loading && canReviewContent(recordInfo.status)">
          <a-card title="审核操作">
            <a-form :model="reviewForm" layout="vertical">
              <a-form-item label="审核结果" required>
                <a-radio-group v-model:value="reviewForm.status">
                  <a-radio :value="6">通过审核</a-radio>
                  <a-radio :value="7">退回修改</a-radio>
                </a-radio-group>
              </a-form-item>
              <a-form-item label="审核意见">
                <a-textarea 
                  v-model:value="reviewForm.remarks" 
                  placeholder="请输入审核意见（可选）"
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
                    {{ reviewForm.status === 6 ? '通过审核' : '退回修改' }}
                  </a-button>
                  <a-button @click="resetReviewForm">
                    重置
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <div class="readonly-notice" v-else-if="!loading">
          <a-alert 
            message="提示" 
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
import {
  ArrowLeftOutlined,
  CheckOutlined
} from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
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
  const statusMap = {
    0: '待审核',
    1: '申请通过',
    2: '拒绝申请',
    3: '待填写',
    4: '填写中',
    5: '审核中',
    6: '审核通过',
    7: '退回'
  };
  return statusMap[Number(status)] || '未知';
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
    return '该申请已审核通过，无需再次审核。';
  } else if (statusNum === 7) {
    return '该申请已退回用户修改，等待用户重新提交。';
  } else if (statusNum < 5) {
    return '用户尚未提交内容审核，请等待用户提交后再进行审核。';
  } else {
    return '当前状态不支持内容审核操作。';
  }
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
    message.error('记录ID不能为空');
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

    console.log('获取记录详情响应:', JSON.stringify(response));
    
    if (!response || response.code !== 200) {
      console.error('API返回错误状态:', response);
      message.error('获取记录信息失败: ' + (response?.message || 'API返回错误状态'));
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
      console.log('获取到记录数据:', JSON.stringify(recordData));
      Object.assign(recordInfo, recordData);
      
      // 获取模板内容
      await fetchTemplateContent();
    } else {
      console.error('未找到记录数据:', response);
      message.error('未找到该记录');
      goBack();
    }
  } catch (error) {
    console.error('获取记录信息失败:', error);
    message.error('获取记录信息失败: ' + (error.message || '未知错误'));
    goBack();
  } finally {
    loading.value = false;
  }
};

// 获取模板内容
const fetchTemplateContent = async () => {
  try {
    console.log('开始获取模板内容: recordId =', recordId);
    
    // 获取用户模板内容
    const response = await userTemplateAPI.getTemplateContent(recordId);
    console.log('获取模板内容响应:', JSON.stringify(response));
    
    if (!response || response.code !== 200) {
      console.error('模板内容API返回错误:', response);
      message.warning('获取模板内容失败，可能用户尚未填写内容');
      return;
    }
    
    if (response.data !== undefined) {
      try {
        let contentData;
        
        // 解析响应数据
        if (typeof response.data === 'object' && response.data !== null) {
          contentData = response.data;
        } else {
          const dataStr = String(response.data).trim();
          if (dataStr.startsWith('{') && dataStr.endsWith('}')) {
            contentData = JSON.parse(dataStr);
          } else if (dataStr === "" || dataStr === "null" || dataStr === "{}") {
            contentData = null;
          } else {
            contentData = null;
          }
        }
        
        // 如果用户内容为空，尝试从模板定义获取结构
        if (!contentData || !contentData.nodes || !Array.isArray(contentData.nodes) || contentData.nodes.length === 0) {
          console.log('用户内容为空，尝试从模板定义获取结构');
          
          if (recordInfo.templateId) {
            try {
              const templateResponse = await userTemplateAPI.getTemplateDefinition(recordInfo.templateId);
              console.log('模板定义响应:', JSON.stringify(templateResponse));
              
              if (templateResponse && templateResponse.code === 200 && templateResponse.data?.templateContent) {
                let templateContentData;
                if (typeof templateResponse.data.templateContent === 'string') {
                  templateContentData = JSON.parse(templateResponse.data.templateContent);
                } else {
                  templateContentData = templateResponse.data.templateContent;
                }
                
                // 转换sections格式为nodes格式
                if (templateContentData.sections && Array.isArray(templateContentData.sections)) {
                  const nodes = templateContentData.sections.map((section, index) => {
                    const nodeId = `node_${index + 1}`;
                    const fields = [];
                    
                    if (section.fields && Array.isArray(section.fields)) {
                      section.fields.forEach((fieldGroup, fieldIndex) => {
                        if (fieldGroup.row && Array.isArray(fieldGroup.row)) {
                          fieldGroup.row.forEach((field, rowIndex) => {
                            const fieldId = `${nodeId}_field_${fieldIndex}_${rowIndex}`;
                            fields.push({
                              id: fieldId,
                              label: field.label || `字段${fields.length + 1}`,
                              type: field.type === 'text' ? 'input' : field.type || 'input',
                              required: field.required || false,
                              example: field.example || `请输入${field.label || '内容'}`,
                              guide: field.guide || '',
                              props: field.props || {}
                            });
                          });
                        }
                      });
                    }
                    
                    return {
                      id: nodeId,
                      name: section.name || `节点${index + 1}`,
                      fields: fields
                    };
                  });
                  
                  contentData = {
                    nodes: nodes,
                    formData: {} // 空的表单数据，用户尚未填写
                  };
                } else if (templateContentData.nodes && Array.isArray(templateContentData.nodes)) {
                  contentData = {
                    nodes: templateContentData.nodes || [],
                    formData: {} // 空的表单数据
                  };
                }
              }
            } catch (templateError) {
              console.error('获取模板定义异常:', templateError);
              message.warning('无法获取模板结构，可能是模板配置有误');
            }
          }
        }
        
        // 更新模板内容和表单数据
        if (contentData && contentData.nodes && Array.isArray(contentData.nodes)) {
          console.log('更新模板节点:', contentData.nodes.length, '个节点');
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
        } else {
          console.warn('无有效的模板内容数据');
          templateContent.nodes = [];
        }
      } catch (parseError) {
        console.error('解析模板内容失败:', parseError);
        message.warning('模板内容格式错误');
        templateContent.nodes = [];
      }
    } else {
      console.warn('API响应中没有data字段:', response);
      templateContent.nodes = [];
    }
  } catch (error) {
    console.error('获取模板内容失败:', error);
    message.warning('获取模板内容失败: ' + (error.message || '未知错误'));
    templateContent.nodes = [];
  }
};

// 处理审核
const handleReview = async () => {
  if (!reviewForm.status) {
    message.warning('请选择审核结果');
    return;
  }
  
  const statusText = reviewForm.status === 6 ? '通过审核' : '退回修改';
  
  Modal.confirm({
    title: '确认审核',
    content: `确定要${statusText}吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      submittingReview.value = true;
      try {
        console.log('开始审核:', {
          id: recordId,
          status: reviewForm.status,
          remarks: reviewForm.remarks
        });
        
        const response = await userTemplateAPI.reviewTemplate(
          recordId,
          reviewForm.status,
          reviewForm.remarks
        );
        
        console.log('审核响应:', response);
        
        if (response && response.code === 200) {
          message.success(`${statusText}成功`);
          // 重新获取记录信息以更新状态
          await fetchRecordInfo();
          // 重置审核表单
          resetReviewForm();
        } else {
          throw new Error(response?.message || '审核失败');
        }
      } catch (error) {
        console.error('审核失败:', error);
        message.error('审核失败: ' + (error.message || '未知错误'));
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