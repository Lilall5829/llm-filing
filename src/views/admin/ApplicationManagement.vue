<template>
  <a-card class="application-management" :bordered="false">
    <template #title>
      <div class="title-wrapper">
        <a-typography-title :level="4">申请管理</a-typography-title>
      </div>
    </template>
    
    <div class="table-container">
      <div class="filter-section">
        <a-form layout="inline">
          <a-form-item label="申请状态">
            <a-select v-model:value="filters.status" style="width: 120px" placeholder="全部状态">
              <a-select-option value="all">全部</a-select-option>
              <a-select-option value="pending">待审核</a-select-option>
              <a-select-option value="approved">已通过</a-select-option>
              <a-select-option value="rejected">已拒绝</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="关键词">
            <a-input v-model:value="filters.keyword" placeholder="申请人/申请内容" allowClear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="fetchApplications">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button style="margin-left: 8px" @click="resetFilters">
              重置
            </a-button>
          </a-form-item>
        </a-form>
      </div>
      
      <h2 class="table-title">任务列表</h2>
      
      <a-table
        :columns="columns"
        :data-source="applicationData"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'applyTime'">
            <span>
              <ClockCircleOutlined /> {{ record.applyTime || '未填写' }}
            </span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" @click="handleView(record)">查看</a-button>
              <a-button type="link" @click="handleStatus(record)">状态</a-button>
              <a-button type="link" @click="handleExport(record)">导出</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ClockCircleOutlined, SearchOutlined } from '@ant-design/icons-vue';
import axios from 'axios';
import { message } from 'ant-design-vue';

// 表格加载状态
const loading = ref(false);

// 筛选条件
const filters = reactive({
  status: 'all',
  keyword: ''
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
    width: 120
  },
  {
    title: '用户',
    dataIndex: 'userName',
    key: 'userName',
    width: 180
  },
  {
    title: '使用模板',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 180
  },
  {
    title: '申请时间',
    dataIndex: 'applyTime',
    key: 'applyTime',
    width: 180
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '操作',
    key: 'actions',
    width: 280
  }
];

// 表格数据
const applicationData = ref([]);

// 获取状态颜色
const getStatusColor = (status) => {
  switch (status) {
    case 'pending': return 'blue';
    case 'approved': return 'green';
    case 'rejected': return 'red';
    default: return 'default';
  }
};

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'pending': return '待审核';
    case 'approved': return '已通过';
    case 'rejected': return '已拒绝';
    default: return '未知';
  }
};

// 重置筛选条件
const resetFilters = () => {
  filters.status = 'all';
  filters.keyword = '';
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
    const response = await axios.get('/api/applications', {
      params: {
        page: pagination.current,
        pageSize: pagination.pageSize,
        status: filters.status === 'all' ? '' : filters.status,
        keyword: filters.keyword
      }
    });
    
    if (response.data.code === 0) {
      applicationData.value = response.data.data.items;
      pagination.total = response.data.data.total;
    } else {
      message.error('获取申请数据失败');
    }
  } catch (error) {
    console.error('获取申请数据出错:', error);
    message.error('获取申请数据出错');
  } finally {
    loading.value = false;
  }
};

// 编辑操作
const handleEdit = (record) => {
  console.log('编辑', record);
  message.info(`编辑申请: ${record.id}`);
  // 实现编辑逻辑，例如跳转到编辑页面
};

// 查看操作
const handleView = (record) => {
  console.log('查看', record);
  message.info(`查看申请: ${record.id}`);
  // 实现查看逻辑，例如打开详情弹窗
};

// 状态操作
const handleStatus = async (record) => {
  console.log('状态', record);
  
  // 这里简化为直接切换状态，实际应该弹出确认框
  const newStatus = record.status === 'pending' ? 'approved' : 
                    record.status === 'approved' ? 'rejected' : 'pending';
  
  try {
    const response = await axios.put(`/api/applications/${record.id}/status`, {
      status: newStatus
    });
    
    if (response.data.code === 0) {
      message.success('状态更新成功');
      fetchApplications(); // 刷新列表
    } else {
      message.error('状态更新失败');
    }
  } catch (error) {
    console.error('更新状态出错:', error);
    message.error('更新状态出错');
  }
};

// 导出操作
const handleExport = (record) => {
  console.log('导出', record);
  message.success(`导出申请: ${record.id} 的数据`);
  // 实现导出逻辑，例如生成并下载文件
};

// 页面加载时获取数据
onMounted(() => {
  fetchApplications();
});
</script>

<style scoped>
.application-management {
  width: 100%;
}

.title-wrapper {
  display: flex;
  align-items: center;
}

.filter-section {
  margin-bottom: 16px;
}

.table-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
  color: rgba(0, 0, 0, 0.85);
}

.table-container {
  margin-top: 16px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f5f5f5;
  font-weight: 500;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f0f7ff;
}
</style> 