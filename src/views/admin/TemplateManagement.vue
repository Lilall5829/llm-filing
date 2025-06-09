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
            <a-form-item :label="$t('template.code')">
              <a-input
                v-model:value="filters.templateCode"
                :placeholder="$t('template.pleaseInputCode')"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('template.name')">
              <a-input
                v-model:value="filters.templateName"
                :placeholder="$t('template.pleaseInputName')"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('common.startDate')">
              <a-date-picker
                v-model:value="filters.startTime"
                style="width: 100%"
                :placeholder="$t('common.startDate')"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('common.endDate')">
              <a-date-picker
                v-model:value="filters.endTime"
                style="width: 100%"
                :placeholder="$t('common.endDate')"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item>
              <a-space :size="16" wrap>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  {{ $t("common.search") }}
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  {{ $t("common.reset") }}
                </a-button>
                <a-button type="primary" @click="handleCreateTemplate">
                  <template #icon><PlusOutlined /></template>
                  {{ $t("template.add") }}
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
        :row-key="
          (record) => {
            return record.id;
          }
        "
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space :size="8" wrap>
              <a-button type="link" @click="handleEdit(record.id)">
                <template #icon><EditOutlined /></template>
                {{ $t("common.edit") }}
              </a-button>
              <a-button type="link" danger @click="handleDelete(record)">
                <template #icon><DeleteOutlined /></template>
                {{ $t("common.delete") }}
              </a-button>
              <a-button type="link" @click="handleSend(record)">
                <template #icon><SendOutlined /></template>
                {{ $t("template.send") }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 发送模板对话框 -->
    <a-modal
      v-model:open="sendModalVisible"
      :title="$t('template.sendTemplate')"
      @ok="confirmSendTemplate"
      :confirmLoading="loading"
      width="700px"
    >
      <a-spin :spinning="loading">
        <p v-if="currentTemplate">
          {{ $t("template.sendTo").replace("：", "") }} 「{{
            currentTemplate.templateName
          }}」
        </p>
        <a-table
          :columns="userColumns"
          :data-source="users"
          :row-selection="{
            selectedRowKeys: selectedUserIds,
            onChange: handleUserSelectionChange,
          }"
          :pagination="{ pageSize: 5 }"
          :scroll="{ y: 300 }"
          size="small"
          :row-key="(record) => record.id"
        />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { templateAPI, userTemplateAPI } from "@/api";
import { getUserList } from "@/api/auth";
import {
DeleteOutlined,
EditOutlined,
PlusOutlined,
ReloadOutlined,
SearchOutlined,
SendOutlined,
} from "@ant-design/icons-vue";
import { Modal, message } from "ant-design-vue";
import { computed, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";

const router = useRouter();
const { t } = useI18n();
const loading = ref(false);

// 筛选条件
const filters = reactive({
  templateCode: "",
  templateName: "",
  status: "",
  startTime: null,
  endTime: null,
});

// 表格列定义 - 改为computed属性以支持语言切换
const columns = computed(() => [
  {
    title: t("template.code"),
    dataIndex: "templateCode",
    key: "templateCode",
    width: 120,
  },
  {
    title: t("template.name"),
    dataIndex: "templateName",
    key: "templateName",
    width: 200,
  },
  {
    title: t("template.type"),
    dataIndex: "templateType",
    key: "templateType",
    width: 120,
  },
  {
    title: t("common.createTime"),
    dataIndex: "createTime",
    key: "createTime",
    width: 180,
  },
  {
    title: t("common.updateTime"),
    dataIndex: "updateTime",
    key: "updateTime",
    width: 180,
  },
  {
    title: t("common.actions"),
    key: "action",
    width: 200,
    fixed: "right",
  },
]);

// 用户表格列定义 - 改为computed属性以支持语言切换
const userColumns = computed(() => [
  { title: t("userManagement.userId"), dataIndex: "id", width: 100 },
  { title: t("userManagement.username"), dataIndex: "userName", width: 150 },
  { title: t("userManagement.loginName"), dataIndex: "loginName", width: 150 },
]);

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
      startTime: filters.startTime
        ? filters.startTime.format("YYYY-MM-DD")
        : undefined,
      endTime: filters.endTime
        ? filters.endTime.format("YYYY-MM-DD")
        : undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      // 添加时间戳参数，避免缓存问题
      _t: new Date().getTime(),
    };

    // 调用API获取模板列表
    const response = await templateAPI.getTemplateList(params);

    if (response.data) {
      templates.value = response.data.records || [];
      pagination.total = response.data.total || 0;
    } else {
      console.warn("获取模板列表返回空数据");
      templates.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error("获取模板列表失败:", error);
    if (error.response) {
      console.error("错误响应:", error.response);
      message.error(
        `获取模板列表失败: ${error.response.data?.message || error.message}`
      );
    } else {
      message.error(`获取模板列表失败: ${error.message || "未知错误"}`);
    }
    templates.value = [];
  } finally {
    loading.value = false;
  }
};

// 获取用户列表
const fetchUsers = async () => {
  try {
    const response = await getUserList({
      pageNum: 1,
      pageSize: 1000, // 获取所有用户用于发送选择
    });

    if (response && response.code === 200) {
      // 筛选出普通用户
      users.value = (response.data.content || response.data.records || [])
        .filter(user => user.role === 2) // 只显示普通用户
        .map(user => ({
          ...user,
          key: user.id
        }));

      if (users.value.length === 0) {
        console.warn("警告: 没有可选择的普通用户");
      }
    } else {
      console.error("获取用户列表返回异常:", response);
      message.error(t("template.sendTemplateFailed"));
    }
  } catch (error) {
    console.error("获取用户列表失败:", error);
    if (error.response) {
      console.error("错误状态码:", error.response.status);
      console.error("错误详情:", error.response.data);
    }
    message.error(t("template.sendTemplateFailed"));
  }
};

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => t("pagination.total", { total }),
});

// 搜索按钮点击事件
const handleSearch = () => {
  pagination.current = 1; // 重置到第一页
  fetchTemplates();
};

// 重置按钮点击事件
const handleReset = () => {
  Object.assign(filters, {
    templateCode: "",
    templateName: "",
    status: "",
    startTime: null,
    endTime: null,
  });
  pagination.current = 1;
  fetchTemplates();
};

// 新建模板按钮点击事件
const handleCreateTemplate = () => {
  router.push("/admin/template");
};

// 编辑按钮点击事件
const handleEdit = (id) => {
  router.push(`/admin/template?id=${id}`);
};

// 删除按钮点击事件
const handleDelete = async (record) => {
  if (!record?.id) {
    console.error("删除模板失败: 无效的模板ID");
    message.error(t("template.deleteInvalidId"));
    return;
  }

  Modal.confirm({
    title: t("template.confirmDelete"),
    content: t("template.deleteTemplateConfirm"),
    okText: t("common.confirm"),
    cancelText: t("common.cancel"),
    onOk: async () => {
      try {
        loading.value = true;
        const response = await templateAPI.deleteTemplate(record.id);

        if (response && response.code === 200) {
          message.success(t("template.deleteSuccessWithId", { id: record.id }));
          
          // 检查删除是否生效
          await new Promise(resolve => setTimeout(resolve, 500));
          
          const deletedIndex = templates.value.findIndex(t => t.id === record.id);
          if (deletedIndex !== -1) {
            templates.value.splice(deletedIndex, 1);
            pagination.total = Math.max(0, pagination.total - 1);
            
            if (templates.value.length === 0 && pagination.current > 1) {
              pagination.current = pagination.current - 1;
            }
          } else {
            console.warn("警告: 删除后模板仍然存在于列表中");
          }
          
          try {
            await fetchTemplates();
          } catch (refreshError) {
            console.error("刷新数据失败:", refreshError);
            message.warning(t("template.refreshDataFailed"));
          }
        } else {
          message.error(t("template.deleteStatusAbnormal"));
        }
      } catch (error) {
        console.error("删除模板失败:", error);
        
        if (error.response) {
          console.error("错误响应状态:", error.response.status);
          if (error.response.status === 0) {
            message.error(t("template.serverNotResponding"));
          } else {
            message.error(`${t("template.deleteFailed")}: ${error.response.data?.message || error.message}`);
          }
        } else if (error.request) {
          console.error("请求已发送但无响应:", error.request);
          message.error(t("template.serverNotResponding"));
        } else {
          message.error(`${t("template.deleteFailed")}: ${error.message}`);
        }
      } finally {
        loading.value = false;
      }
    }
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
    message.warning("请选择至少一个用户");
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
        const updatePromises = userTemplateIds.map((id) =>
          userTemplateAPI.updateTemplateStatus(
            id,
            3, // 状态值：3-待填写
            "管理员发送模板，设置为待填写状态"
          )
        );

        // 等待所有状态更新完成
        await Promise.all(updatePromises);
      }

      message.success('模板发送成功，初始状态已设置为"待填写"');
    } else {
      message.success("模板发送成功");
    }

    sendModalVisible.value = false;
  } catch (error) {
    console.error("发送模板失败:", error);
    message.error("发送模板失败: " + (error.message || "未知错误"));
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
