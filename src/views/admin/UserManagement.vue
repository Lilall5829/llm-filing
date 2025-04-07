<template>
  <div class="user-management">
    <div class="content-wrapper">
      <a-card class="filter-card">
        <a-space :size="16">
          <a-input-search
            v-model:value="searchUsername"
            placeholder="用户姓名"
            style="width: 200px"
            @search="handleSearch"
          />
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button type="primary" @click="handleAddUser">
            <template #icon><PlusOutlined /></template>
            添加用户
          </a-button>
        </a-space>
      </a-card>

      <a-card class="table-card">
        <a-table
          :columns="columns"
          :data-source="users"
          :pagination="pagination"
          :loading="loading"
          :scroll="{ x: 'max-content' }"
          :row-key="record => record.id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="handleEdit(record)">
                  编辑
                </a-button>
                <a-button type="link" danger @click="handleDelete(record)">
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="用户名称" name="username">
          <a-input v-model:value="formState.username" placeholder="请输入用户名称" />
        </a-form-item>
        <!-- 可以根据需要添加更多表单项 -->
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import userData from '@/mock/users.json';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

// 搜索条件
const searchUsername = ref('');
const loading = ref(false);

// 表格列定义
const columns = [
  {
    title: '编号',
    dataIndex: 'id',
    key: 'id',
    width: 120,
  },
  {
    title: '用户',
    dataIndex: 'username',
    key: 'username',
    width: 200,
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

// 用户数据
const users = ref([]);

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`,
});

// 表单相关
const modalVisible = ref(false);
const modalTitle = ref('添加用户');
const formRef = ref();
const formState = reactive({
  id: '',
  username: '',
});

const rules = {
  username: [
    { required: true, message: '请输入用户名称', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名称长度应在 2-20 个字符之间', trigger: 'blur' },
  ],
};

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500));
    users.value = userData.users;
    pagination.total = userData.users.length;
  } catch (error) {
    console.error('获取用户列表失败:', error);
    message.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  if (!searchUsername.value) {
    users.value = userData.users;
  } else {
    users.value = userData.users.filter(user => 
      user.username.toLowerCase().includes(searchUsername.value.toLowerCase())
    );
  }
  pagination.current = 1;
};

// 添加用户
const handleAddUser = () => {
  modalTitle.value = '添加用户';
  formState.id = '';
  formState.username = '';
  modalVisible.value = true;
};

// 编辑用户
const handleEdit = (record) => {
  modalTitle.value = '编辑用户';
  Object.assign(formState, record);
  modalVisible.value = true;
};

// 删除用户
const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户"${record.username}"吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      users.value = users.value.filter(user => user.id !== record.id);
      message.success('删除成功');
    },
  });
};

// 表单提交
const handleModalOk = () => {
  formRef.value
    .validate()
    .then(() => {
      if (formState.id) {
        // 编辑用户
        const index = users.value.findIndex(user => user.id === formState.id);
        if (index !== -1) {
          users.value[index] = { ...users.value[index], ...formState };
        }
      } else {
        // 添加用户
        const newUser = {
          id: `ID${Date.now()}`,
          username: formState.username,
          createdAt: new Date().toLocaleDateString(),
          updatedAt: new Date().toLocaleDateString(),
        };
        users.value.unshift(newUser);
      }
      modalVisible.value = false;
      message.success(`${modalTitle.value}成功`);
    })
    .catch(error => {
      console.log('验证失败:', error);
    });
};

// 取消表单
const handleModalCancel = () => {
  formRef.value?.resetFields();
  modalVisible.value = false;
};

// 表格变化事件
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
};

// 初始化
onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.user-management {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
  overflow-x: auto;
  min-width: 375px;
}

.content-wrapper {
  min-width: 800px;
}

.filter-card {
  margin-bottom: 24px;
  background: #fff;
}

.filter-card :deep(.ant-card-body) {
  overflow-x: auto;
}

.filter-card :deep(.ant-space) {
  flex-wrap: nowrap;
}

.table-card {
  background: #fff;
}

:deep(.ant-table-cell-fix-right) {
  background: #fff;
  z-index: 2;
}

/* 自定义滚动条样式 */
.user-management::-webkit-scrollbar {
  height: 8px;
}

.user-management::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.user-management::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.user-management::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* Firefox 滚动条样式 */
.user-management {
  scrollbar-width: thin;
  scrollbar-color: #888 #f1f1f1;
}
</style> 