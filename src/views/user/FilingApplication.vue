<template>
  <div class="filing-application">
    <div class="header-container">
      <div class="title-section">
        <h2 class="page-title">备案模板</h2>
      </div>
      <div class="search-section">
        <a-input-search
          placeholder="搜索备案"
          style="width: 200px;"
          @search="onSearch"
        />
      </div>
    </div>
    
    <a-row :gutter="[16, 16]" class="template-container">
      <a-col :xs="24" :sm="24" :md="12" :lg="8" v-for="(template, index) in templates" :key="index">
        <a-card class="template-card" hoverable>
          <template #title>
            <div class="card-title">{{ template.title }}</div>
          </template>
          
          <div class="card-content">
            <div class="template-intro">模板简介</div>
            <div class="template-applies">适用于：</div>
            <ul class="situation-list">
              <li v-for="(situation, idx) in template.situations" :key="idx">{{ situation }}</li>
            </ul>
          </div>
          
          <template #extra>
            <a-button 
              type="primary" 
              @click="handleApply(template)"
              :loading="submittingId === template.id"
            >申请</a-button>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 确认申请对话框 -->
    <a-modal
      v-model:visible="confirmModalVisible"
      title="确认申请"
      :maskClosable="false"
      @ok="submitApplication"
      @cancel="cancelApplication"
      :confirmLoading="confirmLoading"
    >
      <p>您确定要申请 "{{ selectedTemplate?.title }}" 模板吗？</p>
      <p>申请后将提交给管理员进行审核，请耐心等待。</p>
    </a-modal>
  </div>
</template>

<script setup>
import { message } from 'ant-design-vue';
import { ref } from 'vue';

// 模板数据
const templates = ref([
  {
    id: 1,
    title: '备案模板一',
    situations: ['情况1', '情况2', '情况3']
  },
  {
    id: 2,
    title: '备案模板二',
    situations: ['情况1', '情况2', '情况3']
  },
  {
    id: 3,
    title: '备案模板三',
    situations: ['情况1', '情况2', '情况3']
  },
  {
    id: 4,
    title: '备案模板一',
    situations: ['情况1', '情况2', '情况3']
  },
  {
    id: 5,
    title: '备案模板二',
    situations: ['情况1', '情况2', '情况3']
  },
  {
    id: 6,
    title: '备案模板三',
    situations: ['情况1', '情况2', '情况3']
  }
]);

// 搜索功能
const onSearch = (value) => {
  message.info(`搜索: ${value}`);
};

// 申请状态控制
const confirmModalVisible = ref(false);
const confirmLoading = ref(false);
const selectedTemplate = ref(null);
const submittingId = ref(null); // 跟踪按钮加载状态的ID

// 处理申请按钮点击
const handleApply = (template) => {
  selectedTemplate.value = template;
  confirmModalVisible.value = true;
};

// 取消申请
const cancelApplication = () => {
  confirmModalVisible.value = false;
  selectedTemplate.value = null;
};

// 提交申请
const submitApplication = async () => {
  if (!selectedTemplate.value) return;
  
  confirmLoading.value = true;
  submittingId.value = selectedTemplate.value.id;
  
  try {
    // 这里模拟API调用，实际项目中应替换为真实API
    // const response = await axios.post('/api/applications', {
    //   templateId: selectedTemplate.value.id,
    //   userId: getCurrentUserId(), // 从用户状态获取当前用户ID
    // });
    
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    // 提交成功后处理
    message.success(`已成功提交 "${selectedTemplate.value.title}" 申请，请等待管理员审核`);
    
    // 可以在这里更新应用状态，例如将这个模板标记为"已申请"
    // ...
    
  } catch (error) {
    // 处理错误
    message.error('申请提交失败，请稍后重试');
    console.error('Application submission error:', error);
  } finally {
    confirmLoading.value = false;
    submittingId.value = null;
    confirmModalVisible.value = false;
    selectedTemplate.value = null;
  }
};
</script>

<style scoped>
.filing-application {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: relative;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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
}

.template-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.card-title {
  font-size: 16px;
  font-weight: 500;
}

.card-content {
  min-height: 150px;
}

.template-intro {
  font-size: 14px;
  margin-bottom: 16px;
}

.template-applies {
  font-size: 14px;
  margin-bottom: 8px;
}

.situation-list {
  list-style-type: disc;
  padding-left: 20px;
  color: rgba(0, 0, 0, 0.65);
}

.situation-list li {
  margin-bottom: 4px;
  font-size: 14px;
}
</style> 