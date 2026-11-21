package com.homelog.controller;

import com.homelog.dto.InvitationCreateRequest;
import com.homelog.dto.InvitationRespondRequest;
import com.homelog.service.InvitationService;
import com.homelog.vo.InvitationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 空间邀请控制器（设计文档 v1.2 §10 / 落地路线步 4）。
 *
 * <p>规范：只使用 GET/POST，参数通过 query 或 body 传递。
 * 权限与身份校验全部在 {@link InvitationService} 内完成，控制器不重复判断；
 * 越权以 AccessDeniedException 抛出，由 GlobalExceptionHandler 统一映射为 403。
 */
@RestController
@RequestMapping("/api/space/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * 发起邀请
     * POST /api/space/invitations/create
     */
    @PostMapping("/create")
    public InvitationVO invite(@Valid @RequestBody InvitationCreateRequest request) {
        log.info("收到邀请请求：email={}", request.getEmail());
        return invitationService.invite(request.getEmail(), request.getRole());
    }

    /**
     * 邀请列表
     * GET /api/space/invitations/list?direction=received
     * GET /api/space/invitations/list?direction=sent&pendingOnly=false
     *
     * <p>received=我收到的（被邀请人视角，含接受/拒绝入口）；
     * sent=我在当前空间发出的（邀请人查看对方是否已应答）。
     */
    @GetMapping("/list")
    public List<InvitationVO> list(
            @RequestParam(defaultValue = "received") String direction,
            @RequestParam(defaultValue = "true") boolean pendingOnly) {
        if ("sent".equalsIgnoreCase(direction)) {
            return invitationService.listSent(pendingOnly);
        }
        return invitationService.listReceived(pendingOnly);
    }

    /**
     * 接受邀请（仅被邀请人本人）
     * POST /api/space/invitations/accept
     */
    @PostMapping("/accept")
    public InvitationVO accept(@Valid @RequestBody InvitationRespondRequest request) {
        log.info("接受邀请：invitationId={}", request.getInvitationId());
        return invitationService.accept(request.getInvitationId());
    }

    /**
     * 拒绝邀请（仅被邀请人本人）
     * POST /api/space/invitations/reject
     */
    @PostMapping("/reject")
    public InvitationVO reject(@Valid @RequestBody InvitationRespondRequest request) {
        log.info("拒绝邀请：invitationId={}", request.getInvitationId());
        return invitationService.reject(request.getInvitationId());
    }

    /**
     * 撤销邀请（仅邀请人本人，且邀请仍待处理）
     * POST /api/space/invitations/cancel
     */
    @PostMapping("/cancel")
    public void cancel(@Valid @RequestBody InvitationRespondRequest request) {
        log.info("撤销邀请：invitationId={}", request.getInvitationId());
        invitationService.cancel(request.getInvitationId());
    }
}
