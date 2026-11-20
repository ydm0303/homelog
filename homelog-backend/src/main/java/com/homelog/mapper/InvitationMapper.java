package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Invitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间邀请 Mapper
 *
 * <p>接受 / 拒绝用<b>原子条件更新</b>（{@code WHERE ... AND status = pending}）而非「先查后改」，
 * 以满足设计文档 §7 的并发安全要求：两个请求同时接受同一邀请时，只有一个 UPDATE 影响行数为 1，
 * 另一个得到 0 —— 调用方据此判定「已被处理」。
 */
@Mapper
public interface InvitationMapper extends BaseMapper<Invitation> {

    /** 该空间是否已有针对某用户的待处理邀请（去重用） */
    @Select("SELECT * FROM jj_invitation " +
            "WHERE space_id = #{spaceId} AND invitee_id = #{inviteeId} AND status = 1 " +
            "ORDER BY id DESC LIMIT 1")
    Invitation selectPendingBySpaceAndInvitee(@Param("spaceId") Long spaceId, @Param("inviteeId") Long inviteeId);

    /**
     * 原子接受：仅当邀请属于本人且仍待处理才置为 accepted。
     *
     * @return 受影响行数（1=成功，0=不存在/非本人/已处理）
     */
    @Update("UPDATE jj_invitation SET status = 2, responded_at = #{now} " +
            "WHERE id = #{id} AND invitee_id = #{inviteeId} AND status = 1")
    int acceptAtomically(@Param("id") Long id, @Param("inviteeId") Long inviteeId, @Param("now") LocalDateTime now);

    /**
     * 原子拒绝：仅当邀请属于本人且仍待处理才置为 rejected。
     *
     * @return 受影响行数（1=成功，0=不存在/非本人/已处理）
     */
    @Update("UPDATE jj_invitation SET status = 3, responded_at = #{now} " +
            "WHERE id = #{id} AND invitee_id = #{inviteeId} AND status = 1")
    int rejectAtomically(@Param("id") Long id, @Param("inviteeId") Long inviteeId, @Param("now") LocalDateTime now);

    /** 邀请人主动撤销自己的待处理邀请 */
    @Update("UPDATE jj_invitation SET status = 4, responded_at = #{now} " +
            "WHERE id = #{id} AND inviter_id = #{inviterId} AND status = 1")
    int cancelAtomically(@Param("id") Long id, @Param("inviterId") Long inviterId, @Param("now") LocalDateTime now);

    /** 级联撤销：某邀请人在某空间的待处理邀请（邀请人被移除/降权、空间解散时调用） */
    @Update("UPDATE jj_invitation SET status = 4, responded_at = #{now} " +
            "WHERE space_id = #{spaceId} AND inviter_id = #{inviterId} AND status = 1")
    int cancelPendingByInviter(@Param("spaceId") Long spaceId, @Param("inviterId") Long inviterId,
                               @Param("now") LocalDateTime now);

    /**
     * 我收到的全部邀请（按新到旧）。
     * <p>刻意不用动态 SQL：单人收到的邀请量天然很小，状态筛选放 service 内存里做，
     * 让本 Mapper 保持纯静态注解，规避 {@code <script>} 在注解模式下的转义/兼容风险。
     */
    @Select("SELECT * FROM jj_invitation WHERE invitee_id = #{userId} ORDER BY id DESC")
    List<Invitation> selectReceived(@Param("userId") Long userId);

    /** 我在某空间发出的全部邀请（按新到旧），说明同上 */
    @Select("SELECT * FROM jj_invitation WHERE inviter_id = #{userId} AND space_id = #{spaceId} ORDER BY id DESC")
    List<Invitation> selectSent(@Param("spaceId") Long spaceId, @Param("userId") Long userId);
}
