<template>
  <div class="filing-edit-container">
    <div class="filing-edit">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">编辑备案</h2>
        </div>
      </div>

      <a-card class="info-card">
        <a-descriptions title="基本信息" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
          <a-descriptions-item label="备案编号">{{ filingData.filingNumber }}</a-descriptions-item>
          <a-descriptions-item label="备案名称">{{ filingData.name }}</a-descriptions-item>
          <a-descriptions-item label="模板类型">{{ filingData.templateType }}</a-descriptions-item>
          <a-descriptions-item label="提交时间">{{ filingData.submittedAt || '未提交' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(filingData.status)">
              {{ getStatusText(filingData.status) }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 新增节点完成进度组件 -->
      <a-card class="progress-card">
        <div class="progress-title">节点完成进度</div>
        <a-steps class="node-progress" :current="completedSteps">
          <a-step 
            v-for="node in template.nodes" 
            :key="node.id" 
            :title="node.name"
            :status="getNodeStatus(node.id)"
            @click="handleStepClick(node.id)"
          />
        </a-steps>
      </a-card>

      <a-card class="content-card">
        <a-tabs v-model:activeKey="activeTabKey">
          <a-tab-pane 
            v-for="node in template.nodes" 
            :key="node.id"
            :tab="node.name"
          >
            <div class="node-content">
              <a-form
                :model="nodeForms[node.id]"
                layout="vertical"
                :ref="el => formRefs[node.id] = el"
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
                      :rules="[{ required: field.required, message: `请${field.type === 'select' ? '选择' : '输入'}${field.label}` }]"
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
                      
                      <template v-else-if="field.type === 'input'">
                        <a-input
                          v-model:value="nodeForms[node.id][field.id]"
                          :placeholder="field.example || `请输入${field.label}`"
                          v-bind="field.props"
                        />
                      </template>
                      
                      <template v-else-if="field.type === 'select'">
                        <a-select
                          v-model:value="nodeForms[node.id][field.id]"
                          :placeholder="field.example || `请选择${field.label}`"
                          v-bind="field.props"
                        >
                          <a-select-option 
                            v-for="option in field.props && field.props.options ? field.props.options : []" 
                            :key="option.value" 
                            :value="option.value"
                          >
                            {{ option.label }}
                          </a-select-option>
                        </a-select>
                      </template>
                      
                      <template v-else-if="field.type === 'date'">
                        <a-date-picker
                          v-model:value="nodeForms[node.id][field.id]"
                          :placeholder="field.example || `请选择${field.label}`"
                          style="width: 100%"
                          v-bind="field.props"
                        />
                      </template>
                      
                      <template v-else-if="field.type === 'textarea'">
                        <a-textarea
                          v-model:value="nodeForms[node.id][field.id]"
                          :placeholder="field.example || `请输入${field.label}`"
                          :rows="4"
                          v-bind="field.props"
                        />
                      </template>
                      
                      <template v-else>
                        <a-input
                          v-model:value="nodeForms[node.id][field.id]"
                          :placeholder="field.example || `请输入${field.label}`"
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

      <div class="footer-actions">
        <a-space size="middle">
          <a-button type="primary" @click="handleSave" :loading="submitting">
            <template #icon><SaveOutlined /></template>
            保存
          </a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">
            <template #icon><CheckOutlined /></template>
            提交审核
          </a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>

<script setup>
import templateData from '@/mock/template.json';
import {
CheckOutlined,
SaveOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { computed, onBeforeMount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const filingId = route.params.id;
const submitting = ref(false);
const activeTabKey = ref('algorithm');
const nodeValidationState = reactive({});

// 模板数据
const template = reactive({
  nodes: []
});

// 备案数据
const filingData = reactive({
  id: '',
  filingNumber: '',
  name: '',
  templateType: '',
  submittedAt: '',
  status: 'draft'
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
  switch (status) {
    case 'draft': return '未提交';
    case 'pending': return '审核中';
    case 'approved': return '已通过';
    case 'rejected': return '未通过';
    default: return '未知';
  }
};

// 获取状态颜色
const getStatusColor = (status) => {
  switch (status) {
    case 'draft': return 'default';
    case 'pending': return 'blue';
    case 'approved': return 'green';
    case 'rejected': return 'red';
    default: return 'default';
  }
};

// 获取节点状态
const getNodeStatus = (nodeId) => {
  return nodeValidationState[nodeId] || 'wait';
};

// 节点步骤点击处理
const handleStepClick = (nodeId) => {
  activeTabKey.value = nodeId;
};

// 验证单个节点表单
const validateNodeForm = async (nodeId) => {
  try {
    if (formRefs[nodeId]) {
      await formRefs[nodeId].validate();
      nodeValidationState[nodeId] = 'finish';
      return true;
    }
    return false;
  } catch (error) {
    nodeValidationState[nodeId] = 'error';
    return false;
  }
};

// 初始化节点表单数据
const initNodeForms = () => {
  template.nodes.forEach(node => {
    if (!nodeForms[node.id]) {
      nodeForms[node.id] = {};
    }
    
    // 初始化字段
    node.fields.forEach(field => {
      if (field.type === 'checkbox') {
        nodeForms[node.id][field.id] = [];
      } else {
        nodeForms[node.id][field.id] = '';
      }
    });
    
    // 初始化节点验证状态
    nodeValidationState[node.id] = 'wait';
  });
};

// 检查所有节点表单完成状态
const checkAllNodeStatus = async () => {
  for (const nodeId in nodeForms) {
    await validateNodeForm(nodeId);
  }
};

// 验证所有表单
const validateForms = async () => {
  try {
    // 遍历所有表单进行验证
    for (const nodeId in formRefs) {
      if (formRefs[nodeId]) {
        await formRefs[nodeId].validate();
        nodeValidationState[nodeId] = 'finish';
      }
    }
    return true;
  } catch (error) {
    return false;
  }
};

// 监听表单数据变化，自动验证当前节点
watch(activeTabKey, (newTabKey) => {
  if (newTabKey && formRefs[newTabKey]) {
    setTimeout(() => {
      validateNodeForm(newTabKey);
    }, 500);
  }
});

// 获取备案数据
const fetchFilingData = async () => {
  try {
    // 实际项目中这里应调用API获取数据
    // 模拟API调用
    const response = await new Promise(resolve => {
      setTimeout(() => {
        resolve({
          data: {
            id: filingId,
            filingNumber: 'BA2023003',
            name: '内容生成模型备案',
            templateType: '模型备案',
            submittedAt: '',
            status: 'draft',
            formData: {
              algorithm: {
                algorithm_type: '生成式对话模型',
                algorithm_name: '智能内容生成器',
                launch_time: null,
                version: 'v1.2.0',
                application_fields: ['social_media', 'info_ugc', 'finance_info']
              },
              risk: {
                risk_type: 'content_risk',
                assessment_method: '通过模拟用户输入和红蓝对抗测试，评估模型输出的安全性和合规性',
                control_measures: '建立内容审核机制，设置敏感词过滤，提供用户反馈渠道，定期更新安全规则',
                evaluation_frequency: 'monthly'
              },
              governance: {
                responsible_person: '张三',
                contact_info: 'zhangsan@example.com',
                department: '安全合规部',
                governance_structure: '由技术部、安全部、法务部组成的算法治理委员会，定期召开会议评估算法风险',
                emergency_plan: '发现重大安全风险时，立即暂停相关功能，组织专项小组分析处理，发布修复方案'
              }
            }
          }
        });
      }, 500);
    });

    // 填充数据
    Object.assign(filingData, response.data);
    
    // 填充各个表单数据
    if (response.data.formData) {
      Object.keys(response.data.formData).forEach(nodeId => {
        if (nodeForms[nodeId]) {
          Object.assign(nodeForms[nodeId], response.data.formData[nodeId]);
        }
      });
    }
    
    // 检查所有节点的完成状态
    setTimeout(() => {
      checkAllNodeStatus();
    }, 500);
    
  } catch (error) {
    console.error('获取备案数据失败:', error);
    message.error('获取备案数据失败');
  }
};

// 保存草稿
const handleSave = async () => {
  submitting.value = true;
  try {
    // 检查所有节点状态
    await checkAllNodeStatus();
    
    // 构建保存的数据
    const saveData = {
      id: filingData.id,
      formData: { ...nodeForms },
      status: 'draft'
    };
    
    // 调用API保存数据
    // 实际项目中这里应该是API调用
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    message.success('保存成功');
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败');
  } finally {
    submitting.value = false;
  }
};

// 提交审核
const handleSubmit = async () => {
  // 验证所有表单
  const valid = await validateForms();
  if (!valid) {
    message.error('请填写所有必填项');
    return;
  }
  
  submitting.value = true;
  try {
    // 构建提交的数据
    const submitData = {
      id: filingData.id,
      formData: { ...nodeForms },
      status: 'pending'
    };
    
    // 调用API提交数据
    // 实际项目中这里应该是API调用
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    message.success('提交成功，请等待审核');
    
    // 跳转回备案中心
    router.push('/user/filing-center');
  } catch (error) {
    console.error('提交失败:', error);
    message.error('提交失败');
  } finally {
    submitting.value = false;
  }
};

// 组件挂载前加载模板数据
onBeforeMount(() => {
  // 加载模板数据
  template.nodes = [...templateData.nodes];
  
  // 初始化节点表单数据
  initNodeForms();
});

// 组件挂载时获取数据
onMounted(() => {
  fetchFilingData();
});
</script>

<style scoped>
.filing-edit-container {
  width: 100%;
  overflow-x: auto;
  min-width: 1000px; /* 设置最小宽度，小于此宽度时出现滚动条 */
}

.filing-edit {
  padding: 24px;
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
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin: 0;
}

.info-card {
  margin-bottom: 24px;
}

.progress-card {
  margin-bottom: 24px;
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
  margin-bottom: 24px;
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
</style> 