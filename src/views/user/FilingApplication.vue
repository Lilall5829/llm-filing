<template>
  <div class="filing-application">
    <div class="header-container">
      <div class="title-section">
        <h2 class="page-title">备案模板</h2>
      </div>
      <div class="search-section">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索模板名称"
          style="width: 200px;"
          @search="handleSearch"
          :loading="searchLoading"
        />
      </div>
    </div>
    
    <div class="loading-container" v-if="loading">
      <a-spin tip="加载模板中..."></a-spin>
    </div>
    
    <a-empty v-else-if="templates.length === 0" description="暂无可用模板" />
    
    <a-row :gutter="[16, 16]" class="template-container" v-else>
      <a-col :xs="24" :sm="24" :md="12" :lg="8" v-for="template in templates" :key="template.id">
        <a-card class="template-card" hoverable>
          <template #title>
            <div class="card-title">{{ template.templateName }}</div>
          </template>
          
          <div class="card-content">
            <div class="template-info">
              <p><strong>模板编号：</strong>{{ template.templateCode || '无' }}</p>
              <p><strong>模板类型：</strong>{{ template.templateType || '未分类' }}</p>
              <p v-if="template.templateDescription" class="description">
                <strong>模板描述：</strong>{{ template.templateDescription }}
              </p>
              <p><strong>更新时间：</strong>{{ formatDate(template.updateTime) }}</p>
            </div>
          </div>
          
          <template #extra>
            <a-button 
              type="primary" 
              @click="handleApply(template)"
              :loading="submittingId === template.id"
              :disabled="isTemplateApplied(template.id)"
            >
              {{ isTemplateApplied(template.id) ? '已申请' : '申请' }}
            </a-button>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <a-pagination
      v-if="templates.length > 0"
      class="pagination"
      :current="pagination.current"
      :pageSize="pagination.pageSize" 
      :total="pagination.total"
      :showSizeChanger="true"
      :showQuickJumper="true"
      :showTotal="total => `共 ${total} 个模板`"
      @change="handlePageChange"
      @showSizeChange="handleSizeChange"
    />

    <!-- 确认申请对话框 -->
    <a-modal
      v-model:open="confirmModalVisible"
      title="确认申请"
      :maskClosable="false"
      @ok="submitApplication"
      @cancel="cancelApplication"
      :confirmLoading="confirmLoading"
    >
      <p>您确定要申请 "{{ selectedTemplate?.templateName }}" 模板吗？</p>
      <p>申请后将提交给管理员进行审核，请耐心等待。</p>
    </a-modal>
  </div>
</template>

<script setup>
import { templateAPI, userTemplateAPI } from '@/api';
import { message, notification } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

// 模板数据
const templates = ref([]);
const loading = ref(false);
const searchLoading = ref(false);
const searchText = ref('');
const appliedTemplateIds = ref(new Set()); // 存储已申请的模板ID

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 9,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
});

// 申请状态控制
const confirmModalVisible = ref(false);
const confirmLoading = ref(false);
const selectedTemplate = ref(null);
const submittingId = ref(null); // 跟踪按钮加载状态的ID

// 获取模板列表
const fetchTemplates = async (params = {}) => {
  loading.value = true;
  
  try {
    const searchParams = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      templateName: searchText.value || undefined,
      ...params
    };
    
    console.log('获取公开模板列表请求参数:', JSON.stringify(searchParams));
    
    const response = await templateAPI.getPublicTemplates(searchParams);
    console.log('获取公开模板列表响应:', JSON.stringify(response));
    
    if (response && response.code === 200 && response.data) {
      // 修正：Spring Data分页返回的是content和totalElements
      templates.value = response.data.content || [];
      pagination.total = response.data.totalElements || 0;
      console.log('成功获取公开模板列表，数量:', templates.value.length);
      
      if (templates.value.length > 0) {
        console.log('模板示例:', JSON.stringify(templates.value[0]));
      } else {
        console.warn('模板列表为空');
      }
    } else {
      console.warn('API返回异常:', response);
      templates.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('获取模板列表失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error('获取模板列表失败');
    templates.value = [];
  } finally {
    loading.value = false;
  }
};

// 获取用户已申请的模板列表
const fetchAppliedTemplates = async () => {
  try {
    const response = await userTemplateAPI.getAppliedTemplateList({
      pageNum: 1,
      pageSize: 100 // 获取足够多记录来确定用户已申请哪些模板
    });
    
    console.log('获取已申请模板列表响应:', JSON.stringify(response));
    
    if (response && response.code === 200 && response.data) {
      // 提取已申请的模板ID
      appliedTemplateIds.value = new Set(
        response.data.content.map(record => record.templateId)
      );
      console.log('已申请的模板ID:', Array.from(appliedTemplateIds.value));
    }
  } catch (error) {
    console.error('获取已申请模板列表失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
  }
};

// 搜索功能
const handleSearch = (value) => {
  searchText.value = value;
  pagination.current = 1; // 重置页码
  fetchTemplates();
};

// 分页处理
const handlePageChange = (page, pageSize) => {
  pagination.current = page;
  pagination.pageSize = pageSize;
  fetchTemplates();
};

// 每页条数变化
const handleSizeChange = (current, size) => {
  pagination.current = 1;
  pagination.pageSize = size;
  fetchTemplates();
};

// 检查模板是否已被申请
const isTemplateApplied = (templateId) => {
  return appliedTemplateIds.value.has(templateId);
};

// 处理申请按钮点击
const handleApply = (template) => {
  if (isTemplateApplied(template.id)) {
    message.info('您已申请此模板，请勿重复申请');
    return;
  }
  
  selectedTemplate.value = template;
  confirmModalVisible.value = true;
};

// 取消申请
const cancelApplication = () => {
  confirmModalVisible.value = false;
  selectedTemplate.value = null;
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知';
  
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (error) {
    return dateString;
  }
};

// 提交申请
const submitApplication = async () => {
  if (!selectedTemplate.value) return;
  
  confirmLoading.value = true;
  submittingId.value = selectedTemplate.value.id;
  
  try {
    // API会从JWT令牌自动获取当前用户，不需要额外传递用户ID
    // 但由于后端需要userIds字段，我们传入一个空数组，让API内部自动处理
    
    // 调用申请模板API
    const response = await userTemplateAPI.applyTemplate(
      selectedTemplate.value.id,
      [] // 提供空数组，API内部会自动使用当前登录用户
    );
    
    if (response.data) {
      // 申请成功后处理
      notification.success({
        message: '申请成功',
        description: `已成功提交 "${selectedTemplate.value.templateName}" 申请，请等待管理员审核`
      });
      
      // 更新已申请模板列表
      appliedTemplateIds.value.add(selectedTemplate.value.id);
    }
  } catch (error) {
    console.error('申请提交失败:', error);
    message.error('申请提交失败: ' + (error.message || '请稍后重试'));
  } finally {
    confirmLoading.value = false;
    submittingId.value = null;
    confirmModalVisible.value = false;
    selectedTemplate.value = null;
  }
};

// 初始化页面
onMounted(async () => {
  // 获取模板列表
  await fetchTemplates();
  
  // 获取用户已申请的模板列表
  await fetchAppliedTemplates();
});
</script>

<style scoped>
.filing-application {
  background-color: #f0f2f5;
  padding: 24px;
  min-height: 100vh;
  border-radius: 4px;
  position: relative;
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

.template-container {
  margin-top: 16px;
}

.template-card {
  height: 100%;
  transition: all 0.3s;
  background-color: #fff;
}

.template-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-content {
  min-height: 160px;
}

.template-info {
  font-size: 14px;
}

.template-info p {
  margin-bottom: 8px;
}

.description {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.pagination {
  margin-top: 24px;
  text-align: center;
}
</style> 