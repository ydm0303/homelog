package com.homelog.vo;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间成员 VO
 */
@Data
public class SpaceMemberVO {

    private Long id;
    private Long spaceId;
    private Long userId;
    private Integer role; // 1=owner 2=admin 3=member 4=readonly（语义见 com.homelog.security.Role）
    private Integer receiveMail;
    private LocalDateTime createdAt;

    // 关联用户信息
    private String username;
    private String nickName;
    private String email;

    public static SpaceMemberVO from(SpaceMember member, User user) {
        SpaceMemberVO vo = new SpaceMemberVO();
        vo.setId(member.getId());
        vo.setSpaceId(member.getSpaceId());
        vo.setUserId(member.getUserId());
        vo.setRole(member.getRole());
        vo.setReceiveMail(member.getReceiveMail());
        vo.setCreatedAt(member.getCreatedAt());

        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickName(user.getNickName());
            vo.setEmail(user.getEmail());
        }
        return vo;
    }
}