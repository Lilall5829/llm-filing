<template>
  <div class="filing-center">
    <div class="header-container">
      <div class="title-section">
        <h2 class="page-title">{{ $t("filingCenter.title") }}</h2>
      </div>
    </div>

    <a-card class="filter-card">
      <a-form :model="filters">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('filingCenter.templateCode')">
              <a-input
                v-model:value="filters.templateCode"
                :placeholder="$t('filingCenter.templateCodePlaceholder')"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('filingCenter.templateName')">
              <a-input
                v-model:value="filters.templateName"
                :placeholder="$t('filingCenter.templateNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('common.status')">
              <a-select
                v-model:value="filters.status"
                :placeholder="$t('filingCenter.statusPlaceholder')"
                style="width: 100%"
                allow-clear
              >
                <a-select-option value="">{{
                  $t("filingCenter.allStatus")
                }}</a-select-option>
                <a-select-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item :label="$t('filingCenter.applicationTime')">
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
                  {{ $t("common.search") }}
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  {{ $t("common.reset") }}
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
        :row-key="(record) => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ t(`status.${record.status}`) }}
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
                {{ $t("filingCenter.view") }}
              </a-button>
              <a-button
                type="link"
                @click="handleEdit(record)"
                :disabled="isActionDisabled(record, 'edit')"
              >
                <template #icon><EditOutlined /></template>
                {{ $t("filingCenter.edit") }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <a-empty
        v-if="!loading && filings.length === 0"
        :description="$t('filingCenter.noFilingData')"
      >
        <template #image>
          <img
            src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
            alt="暂无数据"
          />
        </template>
        <template #description>
          <p>{{ $t("filingCenter.noFilingDataDescription") }}</p>
        </template>
        <a-button type="primary" @click="goToFilingApplication">{{
          $t("filingCenter.goToApplication")
        }}</a-button>
      </a-empty>
    </a-card>
  </div>
</template>

<script setup>
import { userTemplateAPI } from "@/api";
import {
EditOutlined,
EyeOutlined,
ReloadOutlined,
SearchOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { computed, h, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";

const router = useRouter();
const { t } = useI18n();
const loading = ref(false);
const dateRange = ref([]);

// 状态选项 - 改为computed属性以支持语言切换
const statusOptions = computed(() => [
  { value: "0", label: t("status.0") },
  { value: "1", label: t("status.1") },
  { value: "2", label: t("status.2") },
  { value: "3", label: t("status.3") },
  { value: "4", label: t("status.4") },
  { value: "5", label: t("status.5") },
  { value: "6", label: t("status.6") },
  { value: "7", label: t("status.7") },
]);

// 筛选条件
const filters = reactive({
  templateCode: "",
  templateName: "",
  status: "",
  startTime: undefined,
  endTime: undefined,
});

// 日期范围变化事件
const onDateChange = (dates) => {
  if (dates && dates.length === 2) {
    filters.startTime = dates[0]?.format("YYYY-MM-DD");
    filters.endTime = dates[1]?.format("YYYY-MM-DD");
  } else {
    filters.startTime = undefined;
    filters.endTime = undefined;
  }
};

// 表格列定义 - 改为computed属性以支持语言切换
const columns = computed(() => [
  {
    title: t("filingCenter.templateId"),
    dataIndex: "id",
    key: "id",
    width: 100,
  },
  {
    title: t("filingCenter.templateCode"),
    dataIndex: "templateCode",
    key: "templateCode",
    width: 120,
  },
  {
    title: t("filingCenter.templateName"),
    dataIndex: "templateName",
    key: "templateName",
    width: 180,
  },
  {
    title: t("filingCenter.applicationTime"),
    dataIndex: "createTime",
    key: "createTime",
    width: 170,
  },
  {
    title: t("common.updateTime"),
    dataIndex: "updateTime",
    key: "updateTime",
    width: 170,
  },
  {
    title: t("common.status"),
    dataIndex: "status",
    key: "status",
    width: 100,
    customRender: ({ record }) => {
      const statusText = t(`status.${Number(record.status)}`);
      const color = getStatusColor(Number(record.status));
      return h('a-tag', { color: color }, statusText);
    },
  },
  {
    title: t("filingCenter.operation"),
    key: "action",
    width: 180,
    fixed: "right",
  },
]);

// 判断操作是否被禁用
const isActionDisabled = (record, action) => {
  const status = Number(record.status);

  if (action === "view") {
    // 状态0, 2禁用查看，其他状态都可查看
    return [0, 2].includes(status);
  }

  if (action === "edit") {
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
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    };

    // 添加搜索条件
    if (filters.templateCode) {
      params.templateCode = filters.templateCode;
    }
    if (filters.templateName) {
      params.templateName = filters.templateName;
    }
    if (filters.status !== undefined && filters.status !== '') {
      params.status = filters.status;
    }

    const response = await userTemplateAPI.getAppliedTemplateList(params);

    if (response && response.code === 200) {
      filings.value = (response.data.content || response.data.records || []).map(filing => ({
        ...filing,
        key: filing.id
      }));
      pagination.total = response.data.totalElements || response.data.total || 0;

      if (filings.value.length === 0) {
        // 备案列表为空，但不需要console输出
      }
    } else {
      console.warn("API返回异常:", response);
      message.error(t("filingCenter.getFilingsFailed"));
    }
  } catch (error) {
    console.error("获取备案列表失败:", error);
    if (error.response) {
      console.error("错误状态码:", error.response.status);
      console.error("错误详情:", error.response.data);
    }
    message.error(t("filingCenter.getFilingsFailed"));
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
  showTotal: (total) => t("filingCenter.totalFilings", { total }),
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
    templateCode: "",
    templateName: "",
    status: "",
    startTime: undefined,
    endTime: undefined,
  });

  // 重置日期选择器
  dateRange.value = [];

  // 重置分页并重新加载数据
  pagination.current = 1;
  fetchFilings();
};

// 查看按钮点击事件
const handleView = (record) => {
  if (isActionDisabled(record, "view")) {
    message.warning(t("filingCenter.viewDisabled"));
    return;
  }

  // 跳转到编辑页面，设置只读模式
  router.push({
    path: `/user/filing-edit`,
    query: {
      id: record.id,
      readonly: "true",
    },
  });
};

// 编辑按钮点击事件
const handleEdit = (record) => {
  if (isActionDisabled(record, "edit")) {
    message.warning(t("filingCenter.editDisabled"));
    return;
  }

  // 跳转到编辑页面，默认为可编辑模式
  router.push({
    path: `/user/filing-edit`,
    query: {
      id: record.id,
    },
  });
};

// 获取状态颜色
const getStatusColor = (status) => {
  const statusMap = {
    0: "default", // 待审核
    1: "green", // 申请通过
    2: "red", // 拒绝申请
    3: "orange", // 待填写
    4: "purple", // 填写中
    5: "processing", // 审核中
    6: "success", // 审核通过
    7: "error", // 退回
  };
  return statusMap[status] || "default";
};

// 前往备案申请页面
const goToFilingApplication = () => {
  router.push("/user/filing-application");
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
