// 导出所有API模块
import * as authAPI from "./auth";
import * as fileAPI from "./file";
import * as filingAPI from "./filing";
import * as templateAPI from "./template";
import * as userAPI from "./user";
import * as userTemplateAPI from "./userTemplate";

export { authAPI, fileAPI, filingAPI, templateAPI, userAPI, userTemplateAPI };

// 导出请求实例
export { default as request } from "./auth";
