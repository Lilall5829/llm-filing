<template>
  <div class="filing-edit-container">
    <div class="filing-edit">
      <div class="header-container">
        <div class="title-section">
          <h2 class="page-title">{{ isReadonly ? '查看备案' : '编辑备案' }}</h2>
          <div class="submission-info" v-if="!isReadonly">
            <a-tag color="processing">
              完成进度: {{ completedSteps }} / {{ templateContent.nodes?.length || 0 }} 节点
            </a-tag>
          </div>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-card class="info-card">
          <a-descriptions title="基本信息" bordered :column="{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item label="模板编号">{{ templateInfo.templateCode || '-' }}</a-descriptions-item>
            <a-descriptions-item label="模板名称">{{ templateInfo.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="申请时间">{{ templateInfo.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ templateInfo.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(templateInfo.status)">
                {{ templateInfo.statusDesc || getStatusText(templateInfo.status) }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 新增节点完成进度组件 -->
        <a-card class="progress-card" v-if="!loading && templateContent.nodes && templateContent.nodes.length > 0">
          <div class="progress-title">节点完成进度</div>
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

        <div v-if="!loading && (!templateContent.nodes || templateContent.nodes.length === 0)" class="empty-content">
          <a-empty description="暂无模板内容数据">
            <template #description>
              <p>抱歉，当前没有可用的模板内容，请联系管理员。</p>
            </template>
          </a-empty>
        </div>

        <div class="footer-actions" v-if="!isReadonly">
          <a-space size="middle">
            <a-button type="primary" @click="handleSave" :loading="savingInProgress" :disabled="submittingInProgress">
              <template #icon><SaveOutlined /></template>
              保存
            </a-button>
            <a-button type="primary" @click="handleSubmit" :loading="submittingInProgress" 
                      :disabled="savingInProgress || !canSubmit">
              <template #icon><CheckOutlined /></template>
              提交审核
            </a-button>
          </a-space>
        </div>

        <div class="footer-actions" v-else>
          <a-space size="middle">
            <a-button @click="goBack">
              <template #icon><ArrowLeftOutlined /></template>
              返回
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
import { message, Modal } from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
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
      message.warning(`请完成"${templateContent.nodes.find(node => node.id === firstErrorNodeId)?.name || '当前'}"节点的必填项`);
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

// 获取模板详情信息
const fetchTemplateInfo = async () => {
  if (!filingId) {
    message.error('备案ID不能为空');
    router.push('/user/filing-center');
    return;
  }

  loading.value = true;
  try {
    // 获取用户模板关系信息
    const response = await userTemplateAPI.getAppliedTemplateList({
      pageSize: 1,
      pageNum: 1,
      id: filingId
    });

    console.log('获取模板详情响应:', JSON.stringify(response));
    
    // 检查响应格式，支持多种可能的分页结构
    let templateData = null;
    
    if (response.data) {
      // 检查是否使用Spring Data分页结构 (content/totalElements)
      if (response.data.content && response.data.content.length > 0) {
        templateData = response.data.content[0];
        console.log('使用Spring Data分页格式获取到模板数据');
      } 
      // 检查是否使用通用分页结构 (records/total)
      else if (response.data.records && response.data.records.length > 0) {
        templateData = response.data.records[0];
        console.log('使用通用分页格式获取到模板数据');
      }
      // 可能是单个对象直接返回
      else if (typeof response.data === 'object' && response.data.id) {
        templateData = response.data;
        console.log('获取到单个模板对象');
      }
    }
    
    if (templateData) {
      console.log('获取到模板数据:', JSON.stringify(templateData));
      Object.assign(templateInfo, templateData);
      
      // 获取模板内容
      await fetchTemplateContent();
    } else {
      console.error('未找到模板数据或格式不匹配:', response);
      message.error('未找到备案信息');
      router.push('/user/filing-center');
    }
  } catch (error) {
    console.error('获取备案信息失败:', error);
    message.error('获取备案信息失败: ' + (error.message || '未知错误'));
    router.push('/user/filing-center');
  } finally {
    loading.value = false;
  }
};

// 获取模板内容
const fetchTemplateContent = async () => {
  try {
    console.log('开始获取模板内容: filingId =', filingId);
    
    // 获取用户模板内容
    const response = await userTemplateAPI.getTemplateContent(filingId);
    console.log('获取模板内容响应:', JSON.stringify(response));
    
    // 检查响应格式
    if (!response || response.code !== 200) {
      console.error('模板内容API返回错误:', response);
      message.error('获取模板内容失败: ' + (response?.message || '未知错误'));
      return;
    }
    
    if (response.data !== undefined) {
      try {
        // 打印接收到的原始数据以便调试
        console.log('接收到的模板内容数据类型:', typeof response.data);
        console.log('接收到的模板内容数据:', response.data);
        
        let contentData;
        
        // 检查响应数据是否已经是对象
        if (typeof response.data === 'object' && response.data !== null) {
          contentData = response.data;
          console.log('响应数据是对象类型，直接使用');
        } else {
          // 检查响应数据是否可能是JSON字符串
          const dataStr = String(response.data).trim();
          console.log('响应数据是字符串类型，尝试解析为JSON');
          
          // 尝试判断数据格式
          if (dataStr.startsWith('{') && dataStr.endsWith('}')) {
            // 看起来是JSON字符串，尝试解析
            try {
              contentData = JSON.parse(dataStr);
              console.log('成功解析JSON字符串');
            } catch (parseError) {
              console.error('JSON解析失败:', parseError);
              message.warning('模板内容格式解析失败，将使用默认模板');
              contentData = { nodes: [], formData: {} };
            }
          } else if (dataStr.includes('=') && dataStr.includes('&')) {
            // 看起来是URL参数格式，尝试解析
            console.log('检测到URL参数格式数据，尝试解析');
            const params = new URLSearchParams(dataStr);
            contentData = {
              nodes: [],
              formData: {}
            };
            
            // 将URL参数转换为对象
            params.forEach((value, key) => {
              try {
                // 尝试解析每个参数值，可能是JSON
                contentData.formData[key] = JSON.parse(value);
              } catch (e) {
                // 如果解析失败，保持原始值
                contentData.formData[key] = value;
              }
            });
            
            message.warning('模板内容格式特殊，已尝试进行转换');
          } else if (dataStr === "" || dataStr === "null") {
            // 空内容，使用默认结构
            console.log('模板内容为空，使用默认结构');
            contentData = { nodes: [], formData: {} };
            message.info('模板内容尚未填写，请填写内容后保存');
          } else {
            // 未知格式，无法解析，返回默认结构
            console.error('无法识别的数据格式:', dataStr);
            message.warning('模板内容格式无法识别，将使用默认模板');
            contentData = { nodes: [], formData: {} };
          }
        }
        
        // 更新模板内容
        if (contentData && contentData.nodes && Array.isArray(contentData.nodes)) {
          console.log('更新模板节点:', contentData.nodes.length, '个节点');
          templateContent.nodes = contentData.nodes;
          
          // 初始化表单
          initNodeForms();
          
          // 如果有已保存的表单数据
          if (contentData.formData) {
            console.log('更新表单数据');
            Object.keys(contentData.formData).forEach(nodeId => {
              if (nodeForms[nodeId]) {
                Object.assign(nodeForms[nodeId], contentData.formData[nodeId]);
              }
            });
          }
          
          // 检查所有节点的完成状态
          setTimeout(() => {
            checkAllNodeStatus();
          }, 500);
        } else {
          // 尝试从模板内容中推断结构
          console.warn('响应中没有有效的nodes数组，尝试推断结构', contentData);
          
          // 如果内容为空或节点为空，可能是新模板，从模板定义中获取结构
          if (templateInfo.templateId) {
            // TODO: 可以从模板定义中获取结构
            console.log('尝试从模板定义获取结构');
            // 这里可以调用其他API获取模板定义，本次修复暂不实现
          }
          
          templateContent.nodes = [];
          message.warning('模板结构不完整，请联系管理员或重新保存');
        }
      } catch (parseError) {
        console.error('解析模板内容失败:', parseError);
        console.error('原始数据:', response.data);
        message.error('模板内容格式不正确: ' + parseError.message);
        templateContent.nodes = [];
      }
    } else {
      console.warn('API响应中没有data字段:', response);
      message.warning('未获取到模板内容数据');
      templateContent.nodes = [];
    }
  } catch (error) {
    console.error('获取模板内容失败:', error);
    message.error('获取模板内容失败: ' + (error.message || '未知错误'));
    templateContent.nodes = [];
  }
};

// 保存草稿
const handleSave = async () => {
  if (isReadonly.value || savingInProgress.value || submittingInProgress.value) return;
  
  savingInProgress.value = true;
  try {
    // 显示加载消息
    const loadingMessage = message.loading('正在保存数据...', 0);
    
    // 检查所有节点状态
    await checkAllNodeStatus();
    
    // 构建保存的数据
    const contentData = {
      nodes: templateContent.nodes,
      formData: { ...nodeForms }
    };
    
    // 将数据转换为JSON字符串
    const content = JSON.stringify(contentData);
    
    console.log('开始保存模板数据...');
    console.log('节点数量:', templateContent.nodes.length);
    console.log('表单字段:', Object.keys(nodeForms));
    console.log('发送的数据长度:', content.length);
    
    // 仅保存表单内容，不尝试更新模板状态
    const saveResponse = await userTemplateAPI.saveTemplateContent(filingId, content);
    
    // 关闭加载消息
    loadingMessage();
    
    if (!saveResponse.code || saveResponse.code !== 200) {
      throw new Error(saveResponse.message || '保存失败');
    }
    
    // 刷新模板信息，获取最新状态
    await fetchTemplateInfo();
  } catch (error) {
    console.error('保存失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error('保存失败: ' + (error.message || '未知错误'));
  } finally {
    savingInProgress.value = false;
  }
};

// 提交审核
const handleSubmit = async () => {
  if (isReadonly.value || submittingInProgress.value || savingInProgress.value || !canSubmit.value) return;
  
  // 验证所有表单
  const valid = await validateForms();
  if (!valid) {
    message.error('请填写所有必填项');
    return;
  }
  
  // 弹出确认对话框
  Modal.confirm({
    title: '提交确认',
    content: '提交后将无法再次编辑，确定要提交审核吗？',
    okText: '确认提交',
    cancelText: '暂不提交',
    onOk: async () => {
      submittingInProgress.value = true;
      try {
        // 显示加载消息
        const loadingMessage = message.loading('正在保存备案数据...', 0);
        
        // 先保存表单数据
        const contentData = {
          nodes: templateContent.nodes,
          formData: { ...nodeForms }
        };
        
        // 将数据转换为JSON字符串
        const content = JSON.stringify(contentData);
        
        console.log('提交前保存模板数据...');
        console.log('节点数量:', templateContent.nodes.length);
        console.log('表单字段:', Object.keys(nodeForms));
        console.log('发送的数据长度:', content.length);
        
        // 调用API保存数据
        const saveResponse = await userTemplateAPI.saveTemplateContent(filingId, content);
        
        // 关闭加载消息
        loadingMessage();
        
        if (!saveResponse.code || saveResponse.code !== 200) {
          throw new Error(saveResponse.message || '保存数据失败');
        }
        
        // 显示新的加载消息
        const submitMessage = message.loading('正在提交审核...', 0);
        
        try {
          // 使用submitForReview API提交审核
          // 这个API会自动将状态修改为"审核中"(5)
          const reviewResponse = await userTemplateAPI.submitForReview(filingId);
          
          if (!reviewResponse.code || reviewResponse.code !== 200) {
            throw new Error(reviewResponse.message || '提交审核失败');
          }
          
          // 关闭加载消息
          submitMessage();
          
          // 显示成功消息
          message.success('备案信息已成功提交，请等待管理员审核');
          
          // 跳转回备案中心
          setTimeout(() => {
            router.push('/user/filing-center');
          }, 1500);
        } catch (reviewError) {
          submitMessage();
          console.error('提交审核失败:', reviewError);
          if (reviewError.response) {
            console.error('错误状态码:', reviewError.response.status);
            console.error('错误详情:', reviewError.response.data);
          }
          message.error('提交审核失败: ' + (reviewError.message || '未知错误'));
        }
      } catch (error) {
        console.error('提交失败:', error);
        if (error.response) {
          console.error('错误状态码:', error.response.status);
          console.error('错误详情:', error.response.data);
        }
        message.error('提交失败: ' + (error.message || '未知错误'));
      } finally {
        submittingInProgress.value = false;
      }
    }
  });
};

// 组件挂载时获取数据
onMounted(() => {
  fetchTemplateInfo();
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