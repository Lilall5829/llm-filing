<template>
  <div class="task-board">
    <div class="content-wrapper">
      <!-- 统计看板 -->
      <div class="statistics-board">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-card class="statistic-card">
              <a-statistic
                title="总模板数"
                :value="statistics.totalTemplates"
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
                :value="statistics.pendingCount"
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
                title="已完成备案"
                :value="statistics.approvedCount"
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

      <!-- 任务列表 -->
      <a-card class="task-list-card" title="任务列表">
        <template #extra>
          <a-space>
            <a-input-search
              v-model:value="filters.templateName"
              placeholder="搜索模板名称"
              style="width: 200px"
              @search="handleSearch"
            />
            <a-select
              v-model:value="filters.status"
              placeholder="状态筛选"
              style="width: 120px"
              allowClear
              @change="handleSearch"
            >
              <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-select-option>
            </a-select>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
          </a-space>
        </template>

        <a-table
          :columns="columns"
          :data-source="tasks"
          :pagination="pagination"
          :loading="loading"
          :scroll="{ x: 'max-content' }"
          :row-key="record => record.id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ record.statusDesc }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button 
                  type="link" 
                  @click="handleApplicationReview(record)"
                  :disabled="!canReviewApplication(record.status)"
                  :class="{ 'disabled-btn': !canReviewApplication(record.status) }"
                >
                  申请审核
                </a-button>
                <a-button 
                  type="link" 
                  @click="handleContentReview(record)"
                  :disabled="!canReviewContent(record.status)"
                  :class="{ 'disabled-btn': !canReviewContent(record.status) }"
                >
                  内容审核
                </a-button>
                <a-button 
                  type="link" 
                  @click="handleDownload(record)"
                  :disabled="!canDownload(record.status)"
                  :class="{ 'disabled-btn': !canDownload(record.status) }"
                >
                  下载
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { userTemplateAPI } from '@/api';
import {
CheckCircleOutlined,
ClockCircleOutlined,
FileOutlined,
SearchOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 统计数据
const statistics = ref({
  totalTemplates: 0,
  pendingCount: 0,
  approvedCount: 0
});

// 状态选项列表
const statusOptions = ref([
  { value: '0', label: '待审核' },
  { value: '1', label: '申请通过' },
  { value: '2', label: '拒绝申请' },
  { value: '3', label: '待填写' },
  { value: '4', label: '填写中' },
  { value: '5', label: '审核中' },
  { value: '6', label: '审核通过' },
  { value: '7', label: '退回' }
]);

// 筛选条件
const filters = reactive({
  templateName: '',
  templateCode: '',
  status: undefined,
  startTime: undefined,
  endTime: undefined
});

const loading = ref(false);

// 表格列定义
const columns = [
  {
    title: '编号',
    dataIndex: 'id',
    key: 'id',
    width: 100,
  },
  {
    title: '用户',
    dataIndex: 'userName',
    key: 'userName',
    width: 150,
  },
  {
    title: '模板编号',
    dataIndex: 'templateCode',
    key: 'templateCode',
    width: 150,
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 150,
  },
  {
    title: '状态',
    dataIndex: 'statusDesc',
    key: 'status',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 300,
    fixed: 'right',
  },
];

// 任务数据
const tasks = ref([]);

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`,
});

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

// 检查是否可以进行内容审核操作
const canReviewContent = (status) => {
  // 只有状态为审核中(5)的任务才能进行内容审核
  return Number(status) === 5;
};

// 检查是否可以下载
const canDownload = (status) => {
  // 只有状态为审核通过(6)的任务才能下载
  return Number(status) === 6;
};

// 获取任务数据
const fetchTasks = async () => {
  loading.value = true;
  try {
    // 构建查询参数
    const params = {
      templateName: filters.templateName || undefined,
      templateCode: filters.templateCode || undefined,
      status: filters.status || undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userId: 'all' // 明确获取所有用户的任务
    };
    
    console.log('任务看板API请求参数:', JSON.stringify(params));
    
    // 调用API获取用户模板列表
    const response = await userTemplateAPI.getAppliedTemplateList(params);
    console.log('任务看板API响应:', JSON.stringify(response));
    
    if (response && response.code === 200 && response.data) {
      tasks.value = response.data.content || [];
      pagination.total = response.data.totalElements || 0;
      console.log('成功获取任务数据，条数:', tasks.value.length);
      
      // 打印任务记录的详细信息，帮助调试
      if (tasks.value.length > 0) {
        console.log('任务记录示例:', JSON.stringify(tasks.value[0]));
      } else {
        console.warn('任务列表为空');
      }
    } else {
      console.warn('API返回异常:', response);
      tasks.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('获取任务数据失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error('获取任务数据失败');
    tasks.value = [];
  } finally {
    loading.value = false;
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

// 获取状态选项
const fetchStatusOptions = async () => {
  try {
    // 调用专门的API获取状态选项
    const response = await userTemplateAPI.getStatusOptions();
    
    if (response && response.code === 200 && response.data) {
      // 更新状态选项
      statusOptions.value = response.data;
    }
  } catch (error) {
    console.error('获取状态选项失败:', error);
    // 使用默认选项，不显示错误信息
  }
};

// 搜索处理
const handleSearch = () => {
  pagination.current = 1;
  fetchTasks();
};

// 表格变化事件
const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchTasks();
};

// 操作处理函数 - 移除原有的编辑、查看等方法，改为申请审核、内容审核
const handleApplicationReview = (record) => {
  if (!canReviewApplication(record.status)) return;
  
  router.push({
    path: '/admin/application-review',
    query: { id: record.id }
  });
};

const handleContentReview = (record) => {
  if (!canReviewContent(record.status)) return;
  
  router.push({
    path: '/admin/content-review',
    query: { id: record.id }
  });
};

const handleDownload = async (record) => {
  if (!canDownload(record.status)) return;
  
  try {
    // 实现下载逻辑
    // 如: window.open(`${baseURL}/api/file/downloadword?id=${record.id}`);
    message.info('下载功能正在实现中');
  } catch (error) {
    console.error('下载文件失败:', error);
    message.error('下载文件失败');
  }
};

// 初始化
onMounted(() => {
  // 获取状态选项
  fetchStatusOptions();
  // 获取任务数据
  fetchTasks();
  // 获取统计数据
  fetchStatistics();
});
</script>

<style scoped>
.task-board {
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

.task-list-card {
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
.task-board::-webkit-scrollbar {
  height: 8px;
}

.task-board::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.task-board::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.task-board::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* Firefox 滚动条样式 */
.task-board {
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