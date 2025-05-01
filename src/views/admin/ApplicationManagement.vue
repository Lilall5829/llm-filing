<template>
  <div class="application-management">
    <div class="content-wrapper">
      <!-- 统计看板 -->
      <div class="statistics-board">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-card class="statistic-card">
              <a-statistic
                title="申请总数"
                :value="statistics.totalApplications || 0"
                :value-style="{ color: '#1890ff' }"
              >
                <template #prefix>
                  <FileOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-card class="statistic-card">
              <a-statistic
                title="待处理申请"
                :value="statistics.pendingCount || 0"
                :value-style="{ color: '#faad14' }"
              >
                <template #prefix>
                  <ClockCircleOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-card class="statistic-card">
              <a-statistic
                title="已完成申请"
                :value="statistics.approvedCount || 0"
                :value-style="{ color: '#52c41a' }"
              >
                <template #prefix>
                  <CheckCircleOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- 申请列表 -->
      <a-card class="app-list-card" :bordered="false" title="申请列表">
        <template #extra>
          <a-space>
            <a-input-search
              v-model:value="filters.templateName"
              placeholder="搜索模板名称"
              style="width: 200px"
              @search="fetchApplications"
            />
            <a-select
              v-model:value="filters.status"
              placeholder="状态筛选"
              style="width: 120px"
              allowClear
              @change="fetchApplications"
            >
              <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-select-option>
            </a-select>
            <a-button type="primary" @click="fetchApplications">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button @click="resetFilters">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </template>

        <a-table
          :columns="columns"
          :data-source="applicationData"
          :pagination="pagination"
          :loading="loading"
          :scroll="{ x: 'max-content' }"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ record.statusDesc }}
              </a-tag>
            </template>
            <template v-if="column.key === 'actions'">
              <a-space>
                <a-button 
                  type="link" 
                  @click="handleReview(record)"
                  :disabled="!canReviewApplication(record.status)"
                  :class="{ 'disabled-btn': !canReviewApplication(record.status) }"
                >
                  审核
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 审核对话框 -->
    <a-modal
      v-model:visible="reviewModalVisible"
      title="申请审核"
      @ok="confirmReview"
      :confirm-loading="submitting"
    >
      <a-form :model="reviewForm" layout="vertical">
        <a-form-item label="审核结果">
          <a-radio-group v-model:value="reviewForm.status">
            <a-radio :value="1">通过申请</a-radio>
            <a-radio :value="2">拒绝申请</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea 
            v-model:value="reviewForm.remarks" 
            placeholder="请输入审核意见"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { userTemplateAPI } from '@/api';
import {
CheckCircleOutlined,
ClockCircleOutlined,
FileOutlined,
ReloadOutlined,
SearchOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 统计数据
const statistics = ref({
  totalApplications: 0,
  pendingCount: 0,
  approvedCount: 0
});

// 表格加载状态
const loading = ref(false);
const submitting = ref(false);

// 审核模态框
const reviewModalVisible = ref(false);
const currentRecord = ref(null);
const reviewForm = reactive({
  status: 1,
  remarks: ''
});

// 状态选项
const statusOptions = ref([]);

// 筛选条件
const filters = reactive({
  status: undefined,
  templateName: ''
});

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条数据`
});

// 表格列定义
const columns = [
  {
    title: '编号',
    dataIndex: 'id',
    key: 'id',
    width: 100
  },
  {
    title: '用户',
    dataIndex: 'userName',
    key: 'userName',
    width: 150
  },
  {
    title: '模板编号',
    dataIndex: 'templateCode',
    key: 'templateCode',
    width: 150
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 180
  },
  {
    title: '状态',
    dataIndex: 'statusDesc',
    key: 'status',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right'
  }
];

// 表格数据
const applicationData = ref([]);

// 获取状态颜色
const getStatusColor = (status) => {
  const statusMap = {
    0: 'default',   // 待审核
    1: 'blue',      // 申请通过
    2: 'red',       // 拒绝申请
    3: 'orange',    // 待填写
    4: 'purple',    // 填写中
    5: 'processing', // 审核中
    6: 'success',   // 审核通过
    7: 'error',     // 退回
  };
  return statusMap[status] || 'default';
};

// 检查是否可以进行申请审核操作
const canReviewApplication = (status) => {
  // 只有状态为待审核(0)的任务才能进行申请审核
  return Number(status) === 0;
};

// 获取状态选项
const fetchStatusOptions = async () => {
  try {
    const response = await userTemplateAPI.getStatusOptions();
    if (response && response.code === 200 && response.data) {
      statusOptions.value = response.data;
    }
  } catch (error) {
    console.error('获取状态选项失败:', error);
  }
};

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const response = await userTemplateAPI.getTemplateStatistics();
    if (response.data) {
      statistics.value = response.data;
    }
  } catch (error) {
    console.error('获取统计数据失败:', error);
    message.error('获取统计数据失败');
  }
};

// 重置筛选条件
const resetFilters = () => {
  filters.status = undefined;
  filters.templateName = '';
  fetchApplications();
};

// 处理表格变化（分页、排序、筛选）
const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchApplications();
};

// 从API获取申请数据
const fetchApplications = async () => {
  loading.value = true;
  try {
    // 过滤参数，只获取状态为待审核(0)、申请通过(1)和拒绝申请(2)的记录
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: filters.status || undefined,
      templateName: filters.templateName || undefined,
      applicationOnly: true // 假设后端API支持这个参数来只返回申请记录
    };
    
    // 使用userTemplateAPI获取数据
    const response = await userTemplateAPI.getAppliedTemplateList(params);
    
    if (response.data) {
      // 过滤只获取申请记录 (状态 0、1、2)
      applicationData.value = (response.data.records || []).filter(
        record => [0, 1, 2].includes(Number(record.status))
      );
      pagination.total = response.data.total || 0;
    }
  } catch (error) {
    console.error('获取申请数据出错:', error);
    message.error('获取申请数据出错: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 申请审核
const handleReview = (record) => {
  if (!canReviewApplication(record.status)) return;
  
  currentRecord.value = record;
  reviewForm.status = 1; // 默认选择通过
  reviewForm.remarks = '';
  reviewModalVisible.value = true;
};

// 确认审核
const confirmReview = async () => {
  if (!currentRecord.value) return;
  
  submitting.value = true;
  try {
    console.log(`Processing application review for record:`, currentRecord.value.id);
    console.log(`Sending application review with status: ${reviewForm.status}`);
    
    const response = await userTemplateAPI.reviewApplication(
      currentRecord.value.id,
      reviewForm.status,
      reviewForm.remarks
    );
    
    console.log('Application review response:', response);
    message.success('申请审核操作成功');
    reviewModalVisible.value = false;
    fetchApplications(); // 刷新列表
    fetchStatistics(); // 刷新统计数据
  } catch (error) {
    console.error('审核操作失败:', error);
    message.error(`审核操作失败: ${error.message || '未知错误'}`);
  } finally {
    submitting.value = false;
  }
};

// 页面加载时获取数据
onMounted(() => {
  fetchStatusOptions();
  fetchApplications();
  fetchStatistics();
});
</script>

<style scoped>
.application-management {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
  overflow-x: auto;
  min-width: 375px;
}

.content-wrapper {
  min-width: 800px;
}

.statistics-board {
  margin-bottom: 24px;
}

.statistic-card {
  background: #fff;
  height: 100%;
}

.app-list-card {
  background: #fff;
}

:deep(.ant-card-head) {
  border-bottom: none;
}

:deep(.ant-table-cell-fix-right) {
  background: #fff;
  z-index: 2;
}

/* 自定义滚动条样式 */
.application-management::-webkit-scrollbar {
  height: 8px;
}

.application-management::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.application-management::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.application-management::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* Firefox 滚动条样式 */
.application-management {
  scrollbar-width: thin;
  scrollbar-color: #888 #f1f1f1;
}

@media screen and (max-width: 576px) {
  .statistics-board .ant-col {
    margin-bottom: 16px;
  }
}

.disabled-btn {
  color: rgba(0, 0, 0, 0.25) !important;
  cursor: not-allowed !important;
}

.disabled-btn:hover {
  color: rgba(0, 0, 0, 0.25) !important;
}
</style> 