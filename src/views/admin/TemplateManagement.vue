<template>
  <div class="template-management">
    <!-- <a-page-header
      title="模板管理"
      :back-icon="false"
    /> -->
    
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
            <a-form-item label="模板名称">
              <a-input
                v-model:value="filters.templateName"
                placeholder="请输入模板名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="审核状态">
              <a-select
                v-model:value="filters.status"
                placeholder="请选择审核状态"
                style="width: 100%"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="blank">未填写</a-select-option>
                <a-select-option value="draft">未提交</a-select-option>
                <a-select-option value="reviewing">审核中</a-select-option>
                <a-select-option value="approved">已通过</a-select-option>
                <a-select-option value="rejected">未通过</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="创建时间">
              <a-date-picker
                v-model:value="filters.createdAt"
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
                <a-button type="primary" @click="handleCreateTemplate">
                  <template #icon><PlusOutlined /></template>
                  新建模板
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
        :data-source="templates"
        :pagination="pagination"
        @change="handleTableChange"
        :loading="loading"
        :scroll="{ x: 'max-content' }"
        :row-key="record => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space :size="8" wrap>
              <a-button type="link" @click="handleEdit(record.id)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button type="link" danger @click="handleDelete(record.id)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
              <a-button type="link" @click="handleSend(record)">
                <template #icon><SendOutlined /></template>
                发送
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import templateData from '@/mock/template.json';
import {
DeleteOutlined,
EditOutlined,
PlusOutlined,
ReloadOutlined,
SearchOutlined,
SendOutlined
} from '@ant-design/icons-vue';
import { Modal, message } from 'ant-design-vue';
import { h, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);

// 筛选条件
const filters = reactive({
  filingNumber: '',
  templateName: '',
  status: '',
  createdAt: null
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
    title: '模板名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '模板类型',
    dataIndex: 'type',
    key: 'type',
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
    width: 200,
    fixed: 'right',
  },
];

// 模板数据
const templates = ref([]);

// 获取模板列表
const fetchTemplates = async () => {
  loading.value = true;
  try {
    // 模拟API调用
    // 实际项目中这里应该调用后端API
    const response = await new Promise(resolve => {
      setTimeout(() => {
        resolve({
          data: [
            {
              id: templateData.id,
              filingNumber: 'BAN2023001',
              name: templateData.name,
              type: templateData.type,
              createdAt: '2023-10-01 09:30:45',
              updatedAt: '2023-10-15 14:20:33'
            }
          ]
        });
      }, 500);
    });
    templates.value = response.data;
  } catch (error) {
    console.error('获取模板列表失败:', error);
    message.error('获取模板列表失败');
  } finally {
    loading.value = false;
  }
};

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 1,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
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
    templateName: '',
    status: '',
    createdAt: null
  });
};

// 新建模板按钮点击事件
const handleCreateTemplate = () => {
  router.push('/admin/template');
};

// 编辑按钮点击事件
const handleEdit = (id) => {
  router.push(`/admin/template?id=${id}`);
};

// 删除按钮点击事件
const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个模板吗？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      console.log('删除模板：', id);
      // 实际项目中这里应该调用API删除数据
      templates.value = templates.value.filter(item => item.id !== id);
      message.success('删除成功');
    },
  });
};

// 发送按钮点击事件
const handleSend = (record) => {
  Modal.confirm({
    title: '发送模板',
    content: '请选择要发送的用户',
    icon: h(SendOutlined),
    okText: '发送',
    cancelText: '取消',
    onOk() {
      // 这里应该调用API发送模板
      console.log('发送模板：', record.id);
      message.success('模板发送成功');
    },
  });
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
  fetchTemplates();
});
</script>

<style scoped>
.template-management {
  padding: 24px;
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