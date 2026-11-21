package com.homelog.dto;

import lombok.Data;

/**
 * 解散空间请求（设计文档 v1.2 §6.8）。
 *
 * <p>二次确认：须显式 {@code confirm=true} 且 {@code name} 与当前空间名一致，
 * 防止误触——解散后该空间将从所有成员的空间列表中消失（数据按决策「逻辑标记+延迟清理」保留）。
 */
@Data
public class SpaceDissolveRequest {

    private Boolean confirm;

    /** 需与当前空间名一致 */
    private String name;
}
