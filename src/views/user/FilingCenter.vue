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
            <a-form-item label="模板编号">
              <a-input
                v-model:value="filters.templateCode"
                placeholder="请输入模板编号"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="模板名称">
              <a-input
                v-model:value="filters.templateName"
                placeholder="请输入模板名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="状态">
              <a-select
                v-model:value="filters.status"
                placeholder="请选择状态"
                style="width: 100%"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="申请时间">
              <a-range-picker
                v-model:value="dateRange"
                style="width: 100%"
                @change="onDateChange"
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
    
    <a-card class="table-card">
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
              {{ record.statusDesc || getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space :size="8" wrap>
              <a-button 
                type="link" 
                @click="handleView(record)" 
                :disabled="isActionDisabled(record, 'view')"
              >
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-button 
                type="link" 
                @click="handleEdit(record)" 
                :disabled="isActionDisabled(record, 'edit')"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      
      <a-empty 
        v-if="!loading && filings.length === 0" 
        description="暂无备案数据"
      >
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
import { userTemplateAPI } from '@/api';
import {
EditOutlined,
EyeOutlined,
ReloadOutlined,
SearchOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);
const dateRange = ref([]);

// 状态选项
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
  templateCode: '',
  templateName: '',
  status: '',
  startTime: undefined,
  endTime: undefined
});

// 日期范围变化事件
const onDateChange = (dates) => {
  if (dates && dates.length === 2) {
    filters.startTime = dates[0]?.format('YYYY-MM-DD');
    filters.endTime = dates[1]?.format('YYYY-MM-DD');
  } else {
    filters.startTime = undefined;
    filters.endTime = undefined;
  }
};

// 表格列定义
const columns = [
  {
    title: '模板ID',
    dataIndex: 'id',
    key: 'id',
    width: 100,
  },
  {
    title: '模板编号',
    dataIndex: 'templateCode',
    key: 'templateCode',
    width: 120,
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 180,
  },
  {
    title: '申请时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 170,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: 170,
  },
  {
    title: '状态',
    dataIndex: 'statusDesc',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
  },
];

// 判断操作是否被禁用
const isActionDisabled = (record, action) => {
  const status = Number(record.status);
  
  if (action === 'view') {
    // 状态0, 2禁用查看，其他状态都可查看
    return [0, 2].includes(status);
  } 
  
  if (action === 'edit') {
    // 状态1, 3, 4, 7可以编辑，其他状态禁用编辑
    return ![1, 3, 4, 7].includes(status);
  }
  
  return false;
};

// 备案数据
const filings = ref([]);

// 获取备案列表
const fetchFilings = async () => {
  loading.value = true;
  try {
    // 构建查询参数
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      templateCode: filters.templateCode || undefined,
      templateName: filters.templateName || undefined,
      status: filters.status || undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
      userId: 'current' // 显式指定查询当前用户的记录
    };
    
    console.log('备案中心请求参数:', JSON.stringify(params));
    
    // 调用API获取用户模板列表
    // 注意：API会根据当前用户token自动筛选结果
    // 普通用户只能看到自己申请的或管理员发送给自己的模板
    // 无需在前端显式添加用户ID筛选
    const response = await userTemplateAPI.getAppliedTemplateList(params);
    console.log('备案中心响应:', JSON.stringify(response));
    
    if (response && response.code === 200 && response.data) {
      // 修正：Spring Data分页返回的是content和totalElements
      filings.value = response.data.content || [];
      pagination.total = response.data.totalElements || 0;
      console.log('成功获取备案列表，数量:', filings.value.length);
      
      // 打印示例记录，帮助调试
      if (filings.value.length > 0) {
        console.log('备案记录示例:', JSON.stringify(filings.value[0]));
      } else {
        console.warn('备案列表为空，可能没有相关记录');
      }
    } else {
      console.warn('API返回异常:', response);
      filings.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('获取备案列表失败:', error);
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误详情:', error.response.data);
    }
    message.error('获取备案列表失败: ' + (error.message || '未知错误'));
    filings.value = [];
  } finally {
    loading.value = false;
  }
};

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条备案`
});

// 搜索按钮点击事件
const handleSearch = () => {
  pagination.current = 1; // 重置为第一页
  fetchFilings();
};

// 重置按钮点击事件
const handleReset = () => {
  // 重置筛选条件
  Object.assign(filters, {
    templateCode: '',
    templateName: '',
    status: '',
    startTime: undefined,
    endTime: undefined
  });
  
  // 重置日期选择器
  dateRange.value = [];
  
  // 重置分页并重新加载数据
  pagination.current = 1;
  fetchFilings();
};

// 查看按钮点击事件
const handleView = (record) => {
  if (isActionDisabled(record, 'view')) {
    message.warning('当前状态下不可查看此备案');
    return;
  }
  
  // 跳转到编辑页面，设置只读模式
  router.push({
    path: `/user/filing-edit`,
    query: { 
      id: record.id,
      readonly: 'true' 
    }
  });
};

// 编辑按钮点击事件
const handleEdit = (record) => {
  if (isActionDisabled(record, 'edit')) {
    message.warning('当前状态下不可编辑此备案');
    return;
  }
  
  // 跳转到编辑页面，默认为可编辑模式
  router.push({
    path: `/user/filing-edit`,
    query: { 
      id: record.id
    }
  });
};

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
  return statusMap[status] || '未知';
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
  return statusMap[status] || 'default';
};

// 前往备案申请页面
const goToFilingApplication = () => {
  router.push('/user/filing-application');
};

// 表格变化事件
const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchFilings();
};

// 组件挂载时获取数据
onMounted(() => {
  fetchFilings();
});
</script>

<style scoped>
.filing-center {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;
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
  background-color: #fff;
}

.table-card {
  margin-bottom: 24px;
  background-color: #fff;
}

:deep(.ant-table-cell-fix-right) {
  background: #fff !important;
  z-index: 2;
}

:deep(.ant-table-cell-fix-left) {
  background: #fff !important;
  z-index: 2;
}

:deep(.ant-empty) {
  margin: 32px 0;
}
</style>