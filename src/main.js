import { createApp } from "vue";
import App from "./App.vue";
import i18n from "./i18n";
import router from "./router";

// 引入Ant Design Vue
import Antd from "ant-design-vue";
import "ant-design-vue/dist/reset.css";

// 如果需要使用Mock数据，取消注释下面的行
// import './mock'
// if (process.env.NODE_ENV === 'development') {
//     console.log("Mock服务已加载");
// }

const app = createApp(App);

app.use(router);
app.use(Antd);
app.use(i18n);

app.mount("#app");
