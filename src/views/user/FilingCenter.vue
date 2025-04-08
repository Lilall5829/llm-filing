<template>
  <div class="filing-center">
    <div class="header-container">
      <div class="title-section">
        <h2 class="page-title">备案中心</h2>
      </div>
    </div>
    
    <a-card class="filter-card">
      <a-form :model="filters">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="备案编号">
              <a-input
                v-model:value="filters.filingNumber"
                placeholder="请输入备案编号"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="备案名称">
              <a-input
                v-model:value="filters.filingName"
                placeholder="请输入备案名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="备案状态">
              <a-select
                v-model:value="filters.status"
                placeholder="请选择备案状态"
                style="width: 100%"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="pending">审核中</a-select-option>
                <a-select-option value="approved">已通过</a-select-option>
                <a-select-option value="rejected">未通过</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="提交时间">
              <a-date-picker
                v-model:value="filters.submittedAt"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          
          <a-col :span="24">
            <a-form-item>
              <a-space :size="16" wrap>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
    
    <a-card class="table-card" v-if="filings.length > 0">
      <a-table
        :columns="columns"
        :data-source="filings"
        :pagination="pagination"
        @change="handleTableChange"
        :loading="loading"
        :scroll="{ x: 'max-content' }"
        :row-key="record => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space :size="8" wrap>
              <a-button type="link" @click="handleEdit(record)" :disabled="!canEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <a-card class="table-card" v-else>
      <a-empty description="暂无备案数据">
        <template #image>
          <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="暂无数据" />
        </template>
        <template #description>
          <p>暂无备案数据，您可以前往备案申请页面提交新的备案</p>
        </template>
        <a-button type="primary" @click="goToFilingApplication">前往申请</a-button>
      </a-empty>
    </a-card>
  </div>
</template>

<script setup>
import {
EditOutlined,
ReloadOutlined,
SearchOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);

// 筛选条件
const filters = reactive({
  filingNumber: '',
  filingName: '',
  status: '',
  submittedAt: null
});

// 表格列定义
const columns = [
  {
    title: '备案编号',
    dataIndex: 'filingNumber',
    key: 'filingNumber',
    width: 120,
  },
  {
    title: '备案名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '模板类型',
    dataIndex: 'templateType',
    key: 'templateType',
    width: 120,
  },
  {
    title: '提交时间',
    dataIndex: 'submittedAt',
    key: 'submittedAt',
    width: 180,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right',
  },
];

// 判断是否可以编辑（未提交或未通过状态才可编辑）
const canEdit = (record) => {
  return record.status === 'rejected' || record.status === 'draft';
};

// 备案数据
const filings = ref([]);

// 获取备案列表
const fetchFilings = async () => {
  loading.value = true;
  try {
    // 模拟API调用
    // 实际项目中这里应该调用后端API
    const response = await new Promise(resolve => {
      setTimeout(() => {
        resolve({
          data: [
            {
              id: '1',
              filingNumber: 'BA2023001',
              name: '人工智能大模型备案',
              templateType: '模型备案',
              submittedAt: '2023-10-01 09:30:45',
              status: 'pending'
            },
            {
              id: '2',
              filingNumber: 'BA2023002',
              name: '推荐系统算法备案',
              templateType: '算法备案',
              submittedAt: '2023-10-15 14:20:33',
              status: 'approved'
            },
            {
              id: '3',
              filingNumber: 'BA2023003',
              name: '内容生成模型备案',
              templateType: '模型备案',
              submittedAt: '2023-10-20 16:45:12',
              status: 'rejected'
            }
          ]
        });
      }, 500);
    });
    filings.value = response.data;
  } catch (error) {
    console.error('获取备案列表失败:', error);
    message.error('获取备案列表失败');
  } finally {
    loading.value = false;
  }
};

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 3,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条备案`
});

// 搜索按钮点击事件
const handleSearch = () => {
  console.log('搜索条件：', filters);
  loading.value = true;
  // 模拟API调用
  setTimeout(() => {
    loading.value = false;
  }, 500);
};

// 重置按钮点击事件
const handleReset = () => {
  Object.assign(filters, {
    filingNumber: '',
    filingName: '',
    status: '',
    submittedAt: null
  });
};

// 编辑按钮点击事件
const handleEdit = (record) => {
  if (!canEdit(record)) {
    message.warning('只有未提交或未通过的备案才能编辑');
    return;
  }
  
  // 跳转到编辑页面，传递备案ID参数
  router.push(`/user/filing-edit/${record.id}`);
};

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

// 前往备案申请页面
const goToFilingApplication = () => {
  router.push('/user');
};

// 表格变化事件
const handleTableChange = (pag, filters, sorter) => {
  console.log('表格变化：', pag, filters, sorter);
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  // 实际项目中这里应该调用API获取对应页的数据
};

// 组件挂载时获取数据
onMounted(() => {
  fetchFilings();
});
</script>

<style scoped>
.filing-center {
  padding: 24px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.filter-card {
  margin-bottom: 24px;
}

.table-card {
  margin-bottom: 24px;
}

:deep(.ant-table-cell-fix-right) {
  background: #fff;
  z-index: 2;
}

:deep(.ant-table-cell-fix-left) {
  background: #fff;
  z-index: 2;
}
</style>