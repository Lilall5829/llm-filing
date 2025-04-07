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
                :value="statistics.total"
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
                :value="statistics.pending"
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
                :value="statistics.completed"
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
              v-model:value="searchText"
              placeholder="搜索任务"
              style="width: 200px"
              @search="handleSearch"
            />
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
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ record.status }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="handleEdit(record)">
                  编辑
                </a-button>
                <a-button type="link" @click="handleView(record)">
                  查看
                </a-button>
                <a-button type="link" @click="handleStatus(record)">
                  状态
                </a-button>
                <a-button type="link" @click="handleExport(record)">
                  导出
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
import taskData from '@/mock/tasks.json';
import {
CheckCircleOutlined,
ClockCircleOutlined,
FileOutlined,
SearchOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

// 统计数据
const statistics = ref({
  total: 0,
  pending: 0,
  completed: 0
});

// 搜索条件
const searchText = ref('');
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
    dataIndex: 'user',
    key: 'user',
    width: 150,
  },
  {
    title: '使用模板',
    dataIndex: 'template',
    key: 'template',
    width: 150,
  },
  {
    title: '模板状态',
    dataIndex: 'status',
    key: 'status',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
  },
  {
    title: '最后修改时间',
    dataIndex: 'updatedAt',
    key: 'updatedAt',
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
  const colorMap = {
    '未填写': 'default',
    '未提交': 'warning',
    '审核中': 'processing',
    '已通过': 'success',
    '未通过': 'error',
  };
  return colorMap[status] || 'default';
};

// 获取任务数据
const fetchTasks = async () => {
  loading.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500));
    tasks.value = taskData.tasks;
    statistics.value = taskData.statistics;
    pagination.total = taskData.tasks.length;
  } catch (error) {
    console.error('获取任务数据失败:', error);
    message.error('获取任务数据失败');
  } finally {
    loading.value = false;
  }
};

// 搜索处理
const handleSearch = () => {
  if (!searchText.value) {
    tasks.value = taskData.tasks;
  } else {
    tasks.value = taskData.tasks.filter(task => 
      task.id.toLowerCase().includes(searchText.value.toLowerCase()) ||
      task.user.toLowerCase().includes(searchText.value.toLowerCase()) ||
      task.template.toLowerCase().includes(searchText.value.toLowerCase())
    );
  }
  pagination.current = 1;
};

// 操作处理函数
const handleEdit = (record) => {
  console.log('编辑任务:', record);
  message.info('编辑功能开发中');
};

const handleView = (record) => {
  console.log('查看任务:', record);
  message.info('查看功能开发中');
};

const handleStatus = (record) => {
  console.log('修改状态:', record);
  message.info('状态修改功能开发中');
};

const handleExport = (record) => {
  console.log('导出任务:', record);
  message.info('导出功能开发中');
};

// 初始化
onMounted(() => {
  fetchTasks();
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
</style> 