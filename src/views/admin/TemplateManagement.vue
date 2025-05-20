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
                v-model:value="filters.templateCode"
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
            <a-form-item label="开始日期">
              <a-date-picker
                v-model:value="filters.startTime"
                style="width: 100%"
                placeholder="开始日期"
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="结束日期">
              <a-date-picker
                v-model:value="filters.endTime"
                style="width: 100%"
                placeholder="结束日期"
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
        :row-key="record => {
          //console.log('表格行键值:', record.id, '类型:', typeof record.id);
          return record.id;
        }"
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

    <!-- 发送模板对话框 -->
    <a-modal
      v-model:open="sendModalVisible"
      title="发送模板"
      @ok="confirmSendTemplate"
      :confirmLoading="loading"
      width="700px"
    >
      <a-spin :spinning="loading">
        <p v-if="currentTemplate">将模板「{{ currentTemplate.templateName }}」发送给以下用户：</p>
        <a-table
          :columns="[
            { title: '用户ID', dataIndex: 'id', width: 100 },
            { title: '用户名', dataIndex: 'userName', width: 150 },
            { title: '登录名', dataIndex: 'loginName', width: 150 }
          ]"
          :data-source="users"
          :row-selection="{ 
            selectedRowKeys: selectedUserIds,
            onChange: handleUserSelectionChange 
          }"
          :pagination="{ pageSize: 5 }"
          :scroll="{ y: 300 }"
          size="small"
          :row-key="record => record.id"
        />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { templateAPI, userAPI, userTemplateAPI } from '@/api';
import {
DeleteOutlined,
EditOutlined,
PlusOutlined,
ReloadOutlined,
SearchOutlined,
SendOutlined
} from '@ant-design/icons-vue';
import { Modal, message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);

// 筛选条件
const filters = reactive({
  templateCode: '',
  templateName: '',
  status: '',
  startTime: null,
  endTime: null
});

// 表格列定义
const columns = [
  {
    title: '备案编号',
    dataIndex: 'templateCode',
    key: 'templateCode',
    width: 120,
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 200,
  },
  {
    title: '模板类型',
    dataIndex: 'templateType',
    key: 'templateType',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '最后修改时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
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
// 用户列表（用于发送模板）
const users = ref([]);
// 发送模板对话框
const sendModalVisible = ref(false);
// 当前选择的模板（用于发送）
const currentTemplate = ref(null);
// 选择的用户ID列表
const selectedUserIds = ref([]);

// 获取模板列表
const fetchTemplates = async () => {
  loading.value = true;
  try {
    // 构建查询参数
    const params = {
      templateCode: filters.templateCode || undefined,
      templateName: filters.templateName || undefined,
      startTime: filters.startTime ? filters.startTime.format('YYYY-MM-DD') : undefined,
      endTime: filters.endTime ? filters.endTime.format('YYYY-MM-DD') : undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      // 添加时间戳参数，避免缓存问题
      _t: new Date().getTime()
    };
    
    // 调用API获取模板列表
    const response = await templateAPI.getTemplateList(params);
    
    if (response.data) {
      templates.value = response.data.records || [];
      pagination.total = response.data.total || 0;
    } else {
      console.warn('获取模板列表返回空数据');
      templates.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('获取模板列表失败:', error);
    if (error.response) {
      console.error('错误响应:', error.response);
      message.error(`获取模板列表失败: ${error.response.data?.message || error.message}`);
    } else {
      message.error(`获取模板列表失败: ${error.message || '未知错误'}`);
    }
    templates.value = [];
  } finally {
    loading.value = false;
  }
};

// 获取用户列表（用于发送模板）
const fetchUsers = async () => {
  try {
    const response = await userAPI.getUserList({
      pageNum: 1,
      pageSize: 100 // 获取足够多的用户
    });
    
    if (response.data && response.data.records) {
      users.value = response.data.records.filter(user => user.role !== 1); // 过滤出非管理员用户
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    message.error('获取用户列表失败');
  }
};

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
});

// 搜索按钮点击事件
const handleSearch = () => {
  pagination.current = 1; // 重置到第一页
  fetchTemplates();
};

// 重置按钮点击事件
const handleReset = () => {
  Object.assign(filters, {
    templateCode: '',
    templateName: '',
    status: '',
    startTime: null,
    endTime: null
  });
  pagination.current = 1;
  fetchTemplates();
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
  // 验证ID
  if (!id) {
    console.error('删除模板失败: 无效的模板ID');
    message.error('无法删除：模板ID无效');
    return;
  }

  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个模板吗？此操作不可恢复。',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        loading.value = true;
        
        const response = await templateAPI.deleteTemplate(id);
        
        if (response && response.code === 200) {
          message.success(`删除成功，模板ID: ${id}`);
        } else {
          console.warn('删除请求异常，响应状态码:', response ? response.code : '无响应');
          message.warning(`删除状态异常，请检查后台日志`);
        }
        
        // 延迟后强制刷新数据，确保后端删除操作完成
        setTimeout(async () => {
          try {
            // 重置到第一页
            pagination.current = 1;
            // 强制刷新数据
            await fetchTemplates();
            
            // 检查删除是否有效 - 验证模板是否从列表中移除
            const stillExists = templates.value.some(template => template.id === id);
            if (stillExists) {
              console.warn('警告: 删除后模板仍然存在于列表中');
              message.warning('删除可能未完全生效，请刷新页面');
            }
          } catch (refreshError) {
            console.error('刷新数据失败:', refreshError);
            message.error('刷新数据失败，请手动刷新页面');
          } finally {
            loading.value = false;
          }
        }, 1000); // 延迟1秒后刷新
      } catch (error) {
        console.error('删除模板失败:', error);
        
        if (error.response) {
          console.error('错误响应状态:', error.response.status);
          message.error(`删除失败: ${error.response.data?.message || error.message}`);
        } else if (error.request) {
          console.error('请求已发送但无响应:', error.request);
          message.error('删除失败: 服务器未响应，请检查网络连接');
        } else {
          message.error(`删除失败: ${error.message || '未知错误'}`);
        }
        loading.value = false;
      }
    },
  });
};

// 打开发送模板对话框
const handleSend = (record) => {
  currentTemplate.value = record;
  selectedUserIds.value = [];
  sendModalVisible.value = true;
  // 获取用户列表
  fetchUsers();
};

// 发送模板
const confirmSendTemplate = async () => {
  if (!currentTemplate.value || !selectedUserIds.value.length) {
    message.warning('请选择至少一个用户');
    return;
  }
  
  try {
    loading.value = true;
    
    // 调用API发送模板
    const response = await userTemplateAPI.applyTemplate(
      currentTemplate.value.id,
      selectedUserIds.value
    );
    
    if (response.code === 200 && response.data) {
      // 获取生成的用户模板关系ID列表
      const userTemplateIds = response.data;
      
      // 确保返回了用户模板关系ID列表
      if (Array.isArray(userTemplateIds) && userTemplateIds.length > 0) {
        // 处理每个关系
        const updatePromises = userTemplateIds.map(id => 
          userTemplateAPI.updateTemplateStatus(
            id,
            3, // 状态值：3-待填写
            '管理员发送模板，设置为待填写状态'
          )
        );
        
        // 等待所有状态更新完成
        await Promise.all(updatePromises);
      }
      
      message.success('模板发送成功，初始状态已设置为"待填写"');
    } else {
      message.success('模板发送成功');
    }
    
    sendModalVisible.value = false;
  } catch (error) {
    console.error('发送模板失败:', error);
    message.error('发送模板失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 表格变化事件
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchTemplates();
};

// 用户选择变化
const handleUserSelectionChange = (selectedRowKeys) => {
  selectedUserIds.value = selectedRowKeys;
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