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
            <template v-if="column.key === 'status'">
              <a-badge 
                :status="record.status === 1 ? 'success' : 'error'" 
                :text="record.status === 1 ? '启用' : '禁用'" 
              />
            </template>
            <template v-if="column.key === 'role'">
              <a-tag :color="record.role === 1 ? 'blue' : 'green'">
                {{ record.role === 1 ? '管理员' : '普通用户' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="handleEdit(record)">
                  编辑
                </a-button>
                <a-button type="link" @click="handleResetPassword(record)">
                  重置密码
                </a-button>
                <a-popconfirm
                  title="确定要删除此用户吗？"
                  @confirm="handleDelete(record)"
                  okText="确定"
                  cancelText="取消"
                >
                  <a-button type="link" danger>
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <a-modal
      v-model:open="modalVisible"
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
        <a-form-item label="登录名" name="loginName" v-if="modalTitle === '添加用户'">
          <a-input v-model:value="formState.loginName" placeholder="请输入登录名" />
        </a-form-item>
        <a-form-item label="用户名称" name="userName">
          <a-input v-model:value="formState.userName" placeholder="请输入用户名称" />
        </a-form-item>
        <a-form-item label="密码" name="password" v-if="modalTitle === '添加用户'">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码对话框 -->
    <a-modal
      v-model:open="resetPasswordVisible"
      title="重置密码"
      @ok="handleResetPasswordOk"
      @cancel="() => resetPasswordVisible = false"
    >
      <a-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="新密码" name="newPassword">
          <a-input-password v-model:value="resetPasswordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="resetPasswordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { deleteUser, getUserList, register, resetPassword, updateUser } from '@/api/auth';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

// 搜索条件
const searchUsername = ref('');
const loading = ref(false);

// 表格列定义
const columns = [
  {
    title: '用户ID',
    dataIndex: 'id',
    key: 'id',
    width: 220,
  },
  {
    title: '登录名',
    dataIndex: 'loginName',
    key: 'loginName',
    width: 150,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName',
    width: 150,
  },
  {
    title: '角色',
    dataIndex: 'role',
    key: 'role',
    width: 100,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  {
    title: '最后登录时间',
    dataIndex: 'loginTime',
    key: 'loginTime',
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
    width: 250,
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
  loginName: '',
  userName: '',
  password: '',
  status: 1,
});

const rules = {
  loginName: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    { min: 3, max: 20, message: '登录名长度应在 3-20 个字符之间', trigger: 'blur' },
  ],
  userName: [
    { required: true, message: '请输入用户名称', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名称长度应在 2-20 个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6-20 个字符之间', trigger: 'blur' },
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' },
  ],
};

// 重置密码表单相关
const resetPasswordVisible = ref(false);
const resetPasswordFormRef = ref();
const currentUserId = ref('');
const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
});

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6-20 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        if (value !== resetPasswordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致');
        }
        return Promise.resolve();
      },
      trigger: 'blur'
    }
  ]
};

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true;
  try {
    const params = {
      current: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchUsername.value || undefined
    };
    
    const res = await getUserList(params);
    
    if (res.data) {
      // 使用Spring Data Page格式
      users.value = res.data.content;  // Spring Data使用content而不是records
      pagination.total = res.data.totalElements;  // 使用totalElements而不是total
    } else {
      message.error('获取用户列表失败');
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    message.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.current = 1;
  fetchUsers();
};

// 添加用户
const handleAddUser = () => {
  modalTitle.value = '添加用户';
  formState.id = '';
  formState.loginName = '';
  formState.userName = '';
  formState.password = '';
  formState.status = 1;
  modalVisible.value = true;
};

// 编辑用户
const handleEdit = (record) => {
  modalTitle.value = '编辑用户';
  formState.id = record.id;
  formState.userName = record.userName;
  formState.status = record.status;
  modalVisible.value = true;
};

// 重置密码
const handleResetPassword = (record) => {
  currentUserId.value = record.id;
  resetPasswordForm.newPassword = '';
  resetPasswordForm.confirmPassword = '';
  resetPasswordVisible.value = true;
};

// 确认重置密码
const handleResetPasswordOk = () => {
  resetPasswordFormRef.value
    .validate()
    .then(async () => {
      try {
        await resetPassword(currentUserId.value, resetPasswordForm.newPassword);
        message.success('密码重置成功');
        resetPasswordVisible.value = false;
      } catch (error) {
        console.error('密码重置失败:', error);
        message.error('密码重置失败');
      }
    })
    .catch(error => {
      console.log('验证失败:', error);
    });
};

// 删除用户
const handleDelete = async (record) => {
  try {
    await deleteUser(record.id);
    message.success('删除成功');
    fetchUsers();
  } catch (error) {
    console.error('删除用户失败:', error);
    message.error('删除用户失败');
  }
};

// 表单提交
const handleModalOk = () => {
  formRef.value
    .validate()
    .then(async () => {
      try {
        if (formState.id) {
          // 编辑用户
          const updateData = {
            userName: formState.userName,
            status: formState.status
          };
          await updateUser(formState.id, updateData);
          message.success('编辑用户成功');
        } else {
          // 添加用户
          const registerData = {
            loginName: formState.loginName,
            userName: formState.userName,
            password: formState.password
          };
          console.log('添加用户请求数据:', registerData); // 添加调试日志
          await register(registerData);
          message.success('添加用户成功');
        }
        modalVisible.value = false;
        fetchUsers();
      } catch (error) {
        console.error(`${modalTitle.value}失败:`, error);
        message.error(`${modalTitle.value}失败`);
      }
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
const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchUsers();
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