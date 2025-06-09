<template>
  <div class="remark-display">
    <!-- 默认显示：仅用户备注 -->
    <div v-if="displayType === 'user'" class="user-remark">
      <span v-if="userRemark" class="remark-content">{{ userRemark }}</span>
      <span v-else class="no-remark">{{ placeholder || $t('remarkDisplay.noUserRemark') }}</span>
    </div>
    
    <!-- 显示系统状态变更信息 -->
    <div v-else-if="displayType === 'system'" class="system-remark">
      <span v-if="systemRemark" class="remark-content system">{{ systemRemark }}</span>
      <span v-else class="no-remark">{{ placeholder || $t('remarkDisplay.noSystemRemark') }}</span>
    </div>
    
    <!-- 显示完整备注 -->
    <div v-else-if="displayType === 'full'" class="full-remark">
      <span v-if="fullRemark" class="remark-content">{{ fullRemark }}</span>
      <span v-else class="no-remark">{{ placeholder || $t('remarkDisplay.noRemark') }}</span>
    </div>
    
    <!-- 分离显示：用户备注和系统信息分开 -->
    <div v-else-if="displayType === 'separated'" class="separated-remark">
      <div v-if="systemRemark" class="system-info">
        <span class="label">{{ $t('remarkDisplay.statusChange') }}:</span>
        <span class="content system">{{ systemRemark }}</span>
      </div>
      <div v-if="userRemark" class="user-info">
        <span class="label">{{ $t('remarkDisplay.userRemark') }}:</span>
        <span class="content">{{ userRemark }}</span>
      </div>
      <span v-if="!systemRemark && !userRemark" class="no-remark">
        {{ placeholder || $t('remarkDisplay.noRemark') }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { extractStatusChangeInfo, extractUserRemark } from '@/utils/remarkUtils';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps({
  // 完整备注内容
  remark: {
    type: String,
    default: ''
  },
  // 显示类型：'user'(仅用户备注), 'system'(仅状态变更), 'full'(完整备注), 'separated'(分离显示)
  displayType: {
    type: String,
    default: 'user'
  },
  // 无备注时的占位文本
  placeholder: {
    type: String,
    default: ''
  }
});

// 计算属性
const fullRemark = computed(() => props.remark || '');

const userRemark = computed(() => {
  return extractUserRemark(props.remark);
});

const systemRemark = computed(() => {
  return extractStatusChangeInfo(props.remark);
});
</script>

<style scoped>
.remark-display {
  word-break: break-word;
  line-height: 1.6;
}

.remark-content {
  color: rgba(0, 0, 0, 0.85);
}

.remark-content.system {
  color: rgba(0, 0, 0, 0.65);
  font-style: italic;
}

.no-remark {
  color: rgba(0, 0, 0, 0.45);
  font-style: italic;
}

.separated-remark .system-info,
.separated-remark .user-info {
  margin-bottom: 8px;
}

.separated-remark .system-info:last-child,
.separated-remark .user-info:last-child {
  margin-bottom: 0;
}

.separated-remark .label {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  margin-right: 8px;
}

.separated-remark .content {
  color: rgba(0, 0, 0, 0.85);
}

.separated-remark .content.system {
  color: rgba(0, 0, 0, 0.65);
  font-style: italic;
}
</style> 