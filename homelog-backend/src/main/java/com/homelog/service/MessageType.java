package com.homelog.service;

/**
 * 站内通知类型常量（与 {@code jj_message.type} 列对应，V3 迁移引入）。
 *
 * <p>取代早先 {@code MessageService.inferType} 靠嗅探文案关键字（"到期"/"@"）
 * 来分类的脆弱做法。新增业务通知时必须在此登记，前端据 type 决定图标与跳转。
 */
public final class MessageType {

    private MessageType() {
    }

    /** 记录相关（指派、变更等），也是历史数据的默认值 */
    public static final String RECORD = "record";

    /** 到期提醒 */
    public static final String REMIND = "remind";

    /** @提及 */
    public static final String MENTION = "mention";

    /** 入家邀请（ref_id 指向 jj_invitation.id） */
    public static final String FAMILY_INVITATION = "family_invitation";

    /** 邀请回执（对方同意/拒绝，ref_id 指向 jj_invitation.id） */
    public static final String FAMILY_RESULT = "family_result";

    /** 成员主动退出家庭空间（告知空间所有者，无需操作） */
    public static final String FAMILY_MEMBER_LEAVE = "family_member_leave";
}
