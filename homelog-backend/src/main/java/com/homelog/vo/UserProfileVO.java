package com.homelog.vo;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户资料（含空间与角色），供 /api/auth/detail 返回。
 */
@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String email;
    private String nickName;
    private String avatar;
    private Integer role;
    private Long spaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileVO from(User user, SpaceMember member) {
        if (user == null) {
            return null;
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickName(user.getNickName());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        if (member != null) {
            vo.setRole(member.getRole());
            vo.setSpaceId(member.getSpaceId());
        } else {
            // 无成员关系：兜底最保守的只读档，且不伪造任何 spaceId
            // （早先填 1 会让前端误以为仍在 space 1，多空间下尤易误导）
            vo.setRole(com.homelog.security.Role.READONLY);
            vo.setSpaceId(null);
        }
        return vo;
    }
}
