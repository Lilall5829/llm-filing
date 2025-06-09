/**
 * 备注解析工具函数
 * 用于从后端返回的完整备注中提取用户输入的具体备注
 */

/**
 * 从完整备注中提取用户备注
 * @param {string} fullRemark - 后端返回的完整备注信息
 * @returns {string} 用户输入的具体备注，如果没有则返回空字符串
 */
export function extractUserRemark(fullRemark) {
  if (!fullRemark || typeof fullRemark !== "string") {
    return "";
  }

  // 查找"备注："关键字
  const remarkPrefix = "，备注：";
  const remarkIndex = fullRemark.indexOf(remarkPrefix);

  if (remarkIndex !== -1) {
    // 提取"备注："之后的内容
    return fullRemark.substring(remarkIndex + remarkPrefix.length).trim();
  }

  // 如果没有找到"备注："，检查是否整个字符串都是用户备注
  // 这种情况可能发生在某些特殊场景下
  const systemPrefixes = [
    "状态从【",
    "管理员操作",
    "用户操作",
    "审核通过",
    "退回修改",
    "申请通过",
    "拒绝申请",
    "提交审核",
  ];

  // 如果备注不包含系统生成的关键字，认为整个都是用户备注
  const hasSystemContent = systemPrefixes.some((prefix) =>
    fullRemark.includes(prefix)
  );

  if (!hasSystemContent) {
    return fullRemark.trim();
  }

  // 没有找到用户备注
  return "";
}

/**
 * 从完整备注中提取状态变更信息
 * @param {string} fullRemark - 后端返回的完整备注信息
 * @returns {string} 状态变更信息
 */
export function extractStatusChangeInfo(fullRemark) {
  if (!fullRemark || typeof fullRemark !== "string") {
    return "";
  }

  // 查找"备注："关键字
  const remarkPrefix = "，备注：";
  const remarkIndex = fullRemark.indexOf(remarkPrefix);

  if (remarkIndex !== -1) {
    // 返回"备注："之前的内容
    return fullRemark.substring(0, remarkIndex).trim();
  }

  // 如果没有找到"备注："，返回完整内容
  return fullRemark.trim();
}

/**
 * 检查备注是否包含用户输入的内容
 * @param {string} fullRemark - 后端返回的完整备注信息
 * @returns {boolean} 是否包含用户备注
 */
export function hasUserRemark(fullRemark) {
  const userRemark = extractUserRemark(fullRemark);
  return userRemark.length > 0;
}

/**
 * 格式化显示备注（根据显示需求选择显示内容）
 * @param {string} fullRemark - 后端返回的完整备注信息
 * @param {string} displayType - 显示类型：'user'(仅用户备注), 'system'(仅状态变更), 'full'(完整备注)
 * @returns {string} 格式化后的备注
 */
export function formatRemarkForDisplay(fullRemark, displayType = "user") {
  if (!fullRemark) {
    return "";
  }

  switch (displayType) {
    case "user":
      return extractUserRemark(fullRemark);
    case "system":
      return extractStatusChangeInfo(fullRemark);
    case "full":
    default:
      return fullRemark;
  }
}

/* 
// 使用示例：
// 
// 完整备注："状态从【填写中】变更为【审核中】，用户操作，提交审核，备注：需要尽快处理"
// 
// extractUserRemark(fullRemark) 
// 返回：'需要尽快处理'
// 
// extractStatusChangeInfo(fullRemark)
// 返回：'状态从【填写中】变更为【审核中】，用户操作，提交审核'
// 
// formatRemarkForDisplay(fullRemark, 'user')
// 返回：'需要尽快处理'
// 
// formatRemarkForDisplay(fullRemark, 'system')
// 返回：'状态从【填写中】变更为【审核中】，用户操作，提交审核'
// 
// formatRemarkForDisplay(fullRemark, 'full')
// 返回：'状态从【填写中】变更为【审核中】，用户操作，提交审核，备注：需要尽快处理'
*/
