package com.example.filing.dto;

/**
 * 使用Java 17密封类特性的文档状态类
 * 通过sealed关键字限制哪些类可以继承它
 */
public sealed abstract class DocumentStatus permits
        DocumentStatus.Pending,
        DocumentStatus.Reviewing,
        DocumentStatus.Approved,
        DocumentStatus.Rejected {

    private final int code;
    private final String description;

    protected DocumentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 待审核状态
    public static final class Pending extends DocumentStatus {
        public Pending() {
            super(0, "待审核");
        }
    }

    // 审核中状态
    public static final class Reviewing extends DocumentStatus {
        public Reviewing() {
            super(5, "审核中");
        }
    }

    // 审核通过状态
    public static final class Approved extends DocumentStatus {
        public Approved() {
            super(6, "审核通过");
        }
    }

    // 退回状态
    public static final class Rejected extends DocumentStatus {
        public Rejected() {
            super(7, "退回");
        }
    }

    // 使用Java 17兼容的instanceof模式匹配和switch表达式
    public static String getStatusDescription(DocumentStatus status) {
        return switch (status.getCode()) {
            case 0 -> "等待管理员审核";
            case 5 -> "正在由管理员审核";
            case 6 -> "已审核通过，流程结束";
            case 7 -> "被退回，需要修改";
            default -> "未知状态";
        };
    }
}