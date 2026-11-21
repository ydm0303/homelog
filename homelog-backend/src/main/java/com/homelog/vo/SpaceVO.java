package com.homelog.vo;

import com.homelog.entity.Space;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭空间摘要（空间切换器数据源，设计文档 v1.2 §6.5 / §10）。
 *
 * <p>每个条目同时携带「本人在该空间内的角色」——前端据此在切换后立刻更新权限按钮，
 * 无需再等待一次 {@code /auth/detail}。角色语义见 {@code com.homelog.security.Role}。
 */
@Data
public class SpaceVO {

    private Long id;
    private String name;
    private Long ownerId;

    /** 本人在该空间的角色：1=owner 2=admin 3=member 4=readonly */
    private Integer role;

    /** 在籍成员数（含 owner） */
    private Integer memberCount;

    private LocalDateTime createdAt;

    public static SpaceVO from(Space space, Integer role, Integer memberCount) {
        SpaceVO vo = new SpaceVO();
        vo.setId(space.getId());
        vo.setName(space.getName());
        vo.setOwnerId(space.getOwnerId());
        vo.setRole(role);
        vo.setMemberCount(memberCount);
        vo.setCreatedAt(space.getCreatedAt());
        return vo;
    }
}
