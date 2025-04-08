import Antd from "ant-design-vue";
import "ant-design-vue/dist/reset.css";
import { createPinia } from "pinia";
import { createApp } from "vue";
import App from "./App.vue";
import "./assets/main.css";
import router from "./router";

// 开发环境下引入Mock数据服务
if (process.env.NODE_ENV === "development") {
  import("./mock/index.js").then(() => {
    console.log("Mock服务已加载");
  });
}

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(Antd);

app.mount("#app");
