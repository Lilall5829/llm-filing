import { createI18n } from "vue-i18n";
import en from "./locales/en";
import zh from "./locales/zh";

const messages = {
  zh,
  en,
};

// 获取浏览器语言或localStorage中保存的语言
function getDefaultLanguage() {
  const savedLanguage = localStorage.getItem("language");
  if (savedLanguage && messages[savedLanguage]) {
    return savedLanguage;
  }

  // 检查浏览器语言
  const browserLanguage = navigator.language.toLowerCase();
  if (browserLanguage.includes("zh")) {
    return "zh";
  }

  return "zh"; // 默认中文
}

const i18n = createI18n({
  legacy: false,
  locale: getDefaultLanguage(),
  fallbackLocale: "zh",
  messages,
  globalInjection: true,
});

export default i18n;
