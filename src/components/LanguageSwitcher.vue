<template>
  <a-dropdown placement="bottomRight">
    <a-button type="text" size="small" class="language-switcher">
      <template #icon>
        <GlobalOutlined />
      </template>
      {{ currentLanguageLabel }}
      <DownOutlined />
    </a-button>
    <template #overlay>
      <a-menu @click="handleLanguageChange">
        <a-menu-item key="zh" :class="{ active: currentLanguage === 'zh' }">
          <span class="language-option">
            <span class="flag">🇨🇳</span>
            <span class="text">中文</span>
            <CheckOutlined v-if="currentLanguage === 'zh'" class="check-icon" />
          </span>
        </a-menu-item>
        <a-menu-item key="en" :class="{ active: currentLanguage === 'en' }">
          <span class="language-option">
            <span class="flag">🇺🇸</span>
            <span class="text">English</span>
            <CheckOutlined v-if="currentLanguage === 'en'" class="check-icon" />
          </span>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import {
CheckOutlined,
DownOutlined,
GlobalOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const { locale, t } = useI18n();

// 当前语言
const currentLanguage = computed(() => locale.value);

// 当前语言标签
const currentLanguageLabel = computed(() => {
  return currentLanguage.value === "zh" ? "中文" : "English";
});

// 处理语言切换
const handleLanguageChange = ({ key }) => {
  if (key !== currentLanguage.value) {
    locale.value = key;
    localStorage.setItem("language", key);

    // 显示切换成功消息
    const successMessage =
      key === "zh" ? "已切换到中文" : "Switched to English";
    message.success(successMessage);

    // 刷新页面以确保所有组件都使用新语言
    setTimeout(() => {
      window.location.reload();
    }, 300);
  }
};
</script>

<style scoped>
.language-switcher {
  color: rgba(0, 0, 0, 0.85);
  height: 32px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.language-switcher:hover {
  background-color: rgba(0, 0, 0, 0.04);
  color: var(--primary-color);
}

.language-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 120px;
}

.flag {
  font-size: 16px;
}

.text {
  flex: 1;
  font-size: 14px;
}

.check-icon {
  color: var(--primary-color);
  font-size: 12px;
}

:deep(.ant-dropdown-menu-item.active) {
  background-color: #f0f5ff;
}

:deep(.ant-dropdown-menu-item:hover) {
  background-color: #f5f5f5;
}
</style>
