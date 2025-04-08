import axios from "axios";

// 检查用户会话状态
export const checkSession = async () => {
  try {
    const response = await axios.get("/api/auth/check-session");
    return response.data.isLoggedIn;
  } catch (error) {
    console.error("Failed to check session:", error);
    return false;
  }
};

// 用户登录
export const login = async (phone, verificationCode) => {
  try {
    const response = await axios.post("/api/auth/login", {
      phone,
      verificationCode,
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// 获取验证码
export const getVerificationCode = async (phone) => {
  try {
    const response = await axios.post("/api/auth/verification-code", {
      phone,
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};
