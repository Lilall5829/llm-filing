package com.example.filing.constants;

/**
 * 用户模板状态常量
 */
public class UserTemplateStatus {

    /**
     * 待审核 - 用户申请模板后的初始状态
     */
    public static final int PENDING_APPROVAL = 0;

    /**
     * 申请通过 - 管理员批准申请
     */
    public static final int APPLICATION_APPROVED = 1;

    /**
     * 拒绝申请 - 管理员拒绝申请
     */
    public static final int APPLICATION_REJECTED = 2;

    /**
     * 待填写 - 管理员发送模板给用户的初始状态
     */
    public static final int PENDING_FILL = 3;

    /**
     * 填写中 - 用户保存内容但未提交
     */
    public static final int FILLING = 4;

    /**
     * 审核中 - 用户提交内容待管理员审核
     */
    public static final int UNDER_REVIEW = 5;

    /**
     * 审核通过 - 管理员审核通过
     */
    public static final int REVIEW_APPROVED = 6;

    /**
     * 退回 - 管理员退回用户修改
     */
    public static final int RETURNED = 7;

    /**
     * 检查状态转换是否允许
     * 
     * @param currentStatus 当前状态
     * @param newStatus     新状态
     * @param isAdmin       是否是管理员
     * @return 是否允许转换
     */
    public static boolean isStatusTransitionAllowed(int currentStatus, int newStatus, boolean isAdmin) {
        // 普通用户只能从填写中(4)变更为审核中(5)
        if (!isAdmin) {
            return currentStatus == FILLING && newStatus == UNDER_REVIEW;
        }

        // 管理员的状态转换规则
        switch (currentStatus) {
            case PENDING_APPROVAL:
                // 待审核(0) -> 待填写(3)或拒绝申请(2)
                return newStatus == PENDING_FILL || newStatus == APPLICATION_REJECTED;
            case UNDER_REVIEW:
                // 审核中(5) -> 审核通过(6)或退回(7)
                return newStatus == REVIEW_APPROVED || newStatus == RETURNED;
            default:
                // 管理员可以执行任意其他状态变更
                return true;
        }
    }
}