package com.homelog.task;

import com.homelog.entity.Message;
import com.homelog.entity.Module;
import com.homelog.entity.Record;
import com.homelog.entity.Space;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.MessageMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.SpaceMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.Role;
import com.homelog.security.SpaceMembershipReader;
import com.homelog.service.MailService;
import com.homelog.service.MessageType;
import com.homelog.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时提醒任务
 * 每天 08:00 扫描即将到期的记录，写入消息中心并发送邮件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemindTask {

    /** 模块 key 查不到时的占位值，仅用于内部判空，不会拼进跳转链接 */
    private static final String MODULE_KEY_UNKNOWN = "unknown";

    private final RecordMapper recordMapper;
    private final SpaceMapper spaceMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final ModuleMapper moduleMapper;
    private final MailService mailService;
    private final PushService pushService;

    @Value("${homelog.remind.lead-days:7,1,0}")
    private String leadDaysStr;

    @Value("${homelog.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * 每天 08:00 执行到期扫描
     *
     * <p>步 3 起支持多空间，故这里<b>遍历所有正常状态的空间</b>逐个处理。
     * 早期实现把 space_id 写死成 1，单空间时代成立；新家庭空间产生后，
     * 除最早那一个家以外所有人的到期提醒都会静默失效 —— 属于必须随步 3 一起改的连带缺陷。
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void scanDueRecords() {
        log.info("开始扫描到期记录...");

        List<Space> spaces = spaceMapper.selectAllActive();
        if (spaces == null || spaces.isEmpty()) {
            log.info("无可处理的家庭空间，跳过到期扫描");
            return;
        }

        String[] leadDaysArray = leadDaysStr.split(",");
        for (String dayStr : leadDaysArray) {
            int leadDays;
            try {
                leadDays = Integer.parseInt(dayStr.trim());
            } catch (NumberFormatException e) {
                // 配置写错（如 "7,1,0," 尾随逗号）只跳过该档，不能让它拖垮整个提醒任务
                log.error("homelog.remind.lead-days 配置项非法，已跳过：{}", dayStr);
                continue;
            }
            LocalDate targetDate = LocalDate.now().plusDays(leadDays);

            for (Space space : spaces) {
                try {
                    processSpace(space.getId(), leadDays, targetDate);
                } catch (Exception e) {
                    // 单个家庭失败不得影响其它家庭：此处吞掉异常后继续下一个空间。
                    // 注意：外层 @Transactional 仍在，失败空间已写入的消息会随其它空间一起提交，
                    // 而不是整批回滚 —— 对每日提醒而言「少发/多发几条」优于「全员静默不发」。
                    log.error("处理家庭空间到期提醒失败，跳过该空间：spaceId={}", space.getId(), e);
                }
            }
        }

        log.info("到期记录扫描完成。空间数={}", spaces.size());
    }

    /**
     * 处理单个家庭空间的到期记录。
     */
    private void processSpace(Long spaceId, int leadDays, LocalDate targetDate) {
        List<Record> dueRecords = recordMapper.selectDueRecords(spaceId, targetDate.toString());

        if (dueRecords.isEmpty()) {
            log.debug("未发现符合条件的到期记录：spaceId={}, 提前天数={}, 日期={}", spaceId, leadDays, targetDate);
            return;
        }

        log.info("发现到期记录：spaceId={}, 数量={}, 提前天数={}, 日期={}",
                spaceId, dueRecords.size(), leadDays, targetDate);

        // 获取该空间内开启邮件提醒的在籍成员
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        List<SpaceMember> mailMembers = members.stream()
                .filter(SpaceMembershipReader::isActive)
                .filter(m -> m.getReceiveMail() != null && m.getReceiveMail() == 1)
                .collect(Collectors.toList());

        // 用户邮箱映射
        Map<Long, User> userMap = mailMembers.stream()
                .map(m -> userMapper.selectById(m.getUserId()))
                .filter(u -> u != null && u.getEmail() != null && !u.getEmail().isEmpty())
                .collect(Collectors.toMap(User::getId, u -> u));

        // 为每条记录创建消息并发送邮件
        for (Record record : dueRecords) {
            processRecord(spaceId, record, leadDays, targetDate, members, mailMembers, userMap);
        }
    }

    /**
     * 处理单条到期记录
     */
    private void processRecord(Long spaceId, Record record, int leadDays, LocalDate targetDate,
                               List<SpaceMember> members, List<SpaceMember> mailMembers, Map<Long, User> userMap) {

        // 模块只查一次：显示名用于文案/邮件，module_key 用于拼跳转链接
        Module module = record.getModuleId() != null ? moduleMapper.selectById(record.getModuleId()) : null;
        String typeLabel = module != null && module.getName() != null ? module.getName() : "记录";
        String moduleKey = module != null && module.getModuleKey() != null
                ? module.getModuleKey() : MODULE_KEY_UNKNOWN;

        String title = record.getTitle() != null ? record.getTitle() : "未命名记录";
        log.info("正在处理到期记录：记录ID={}，标题={}，提前天数={}", record.getId(), title, leadDays);

        String recordPath = buildRecordPath(moduleKey, record);
        String emailJumpUrl = toAbsoluteUrl(recordPath);

        // 构建消息内容
        String content;
        if (leadDays == 0) {
            content = String.format("【%s】%s 今天到期！", typeLabel, title);
        } else if (leadDays == 1) {
            content = String.format("【%s】%s 明天到期，请注意！", typeLabel, title);
        } else {
            content = String.format("【%s】%s 将在 %d 天后到期（%s）", typeLabel, title, leadDays, targetDate);
        }

        // 为每个开启邮件提醒的成员创建消息
        // 私密记录仅管理档（owner/admin）可见（与 RecordService.canViewRecord 一致），避免标题/内容泄露给非管理档
        boolean isPrivate = record.getIsPrivate() != null && record.getIsPrivate() == 1;
        for (SpaceMember member : mailMembers) {
            if (isPrivate && !Role.canSeePrivate(member.getRole())) {
                continue;
            }

            // 写入消息中心
            Message message = new Message()
                    .setSpaceId(spaceId)
                    .setUserId(member.getUserId())
                    .setRecordId(record.getId())
                    .setType(MessageType.REMIND)
                    .setContent(content)
                    .setIsRead(0)
                    .setCreatedAt(LocalDateTime.now());
            messageMapper.insert(message);

            // 发送邮件（异步非阻塞）
            User user = userMap.get(member.getUserId());
            if (user != null) {
                sendRemindEmail(user, record, leadDays, targetDate, content, typeLabel, emailJumpUrl);
            }
        }

        // Web Push 推送：独立通道，不依赖邮件开关（用户是否订阅即其自身开关）。
        // 遍历空间内全部在籍成员并复用上面的私密过滤规则；每用户独立 try-catch，
        // pushToUser 自身已按订阅吞异常，这里的兜底只为防 DB 查询等其它异常冒泡。
        // 载荷里给<b>站内路径</b>而非绝对 URL：SW 有已打开窗口时走 postMessage → router.push(url)，
        // Vue Router 需要的是路由路径；只有无窗口时才 clients.openWindow()，相对路径按 SW 作用域解析，同样可用。
        for (SpaceMember member : members) {
            if (!SpaceMembershipReader.isActive(member)) {
                continue;
            }
            if (isPrivate && !Role.canSeePrivate(member.getRole())) {
                continue;
            }
            try {
                pushService.pushToUser(member.getUserId(), "家记提醒", content, recordPath);
            } catch (Exception e) {
                log.warn("到期提醒 Web Push 发送失败（不影响邮件主流程）：userId={}, recordId={}",
                        member.getUserId(), record.getId(), e);
            }
        }
    }

    /**
     * 发送提醒邮件。
     *
     * <p>typeLabel 与 jumpUrl 由调用方算好传入 —— 本方法会被每个收件人各调一次，
     * 内部再查库就是 成员数×记录数 次重复查询。
     */
    private void sendRemindEmail(User user, Record record, int leadDays, LocalDate targetDate, String content,
                                 String typeLabel, String jumpUrl) {
        String title = record.getTitle() != null ? record.getTitle() : "未命名记录";

        String subject = String.format("【家记提醒】%s", content);
        String htmlBody = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "        .container { background: #fafafa; border-radius: 8px; padding: 24px; }\n" +
                "        .header { color: #e6a23c; font-size: 18px; font-weight: 600; margin-bottom: 16px; }\n" +
                "        .content { background: white; padding: 16px; border-radius: 6px; margin-bottom: 16px; border-left: 4px solid #e6a23c; }\n" +
                "        .detail { margin: 8px 0; }\n" +
                "        .label { color: #999; font-size: 14px; }\n" +
                "        .value { color: #333; font-weight: 500; }\n" +
                "        .btn { display: inline-block; background: #409eff; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; margin-top: 16px; }\n" +
                "        .footer { color: #999; font-size: 12px; margin-top: 24px; text-align: center; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">📅 家记到期提醒</div>\n" +
                "        <div class=\"content\">\n" +
                "            <div class=\"detail\"><span class=\"label\">类型：</span><span class=\"value\">" + typeLabel + "</span></div>\n" +
                "            <div class=\"detail\"><span class=\"label\">标题：</span><span class=\"value\">" + title + "</span></div>\n" +
                "            <div class=\"detail\"><span class=\"label\">提醒时间：</span><span class=\"value\">" +
                (leadDays == 0 ? "今天" : (leadDays == 1 ? "明天" : leadDays + "天后")) + " " +
                targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "</span></div>\n" +
                "        </div>\n" +
                "        <a href=\"" + jumpUrl + "\" class=\"btn\">查看详情</a>\n" +
                "        <div class=\"footer\">\n" +
                "            这是由家记自动发送的提醒邮件，请勿直接回复。<br>\n" +
                "            如不想接收提醒，请在设置中关闭邮件通知。\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        mailService.sendEmail(user.getEmail(), subject, htmlBody);
    }

    /**
     * 记录详情页在 SPA 内的<b>路径</b>，形如 {@code /module/drug/12}。
     *
     * <p><b>不能再拼 hash</b>：前端路由是 history 模式（{@code createWebHistory} +
     * nginx {@code try_files $uri $uri/ /index.html} 回退），而 {@code createWebHistory}
     * 会整段忽略 URL 的 hash 部分。早先写死的 {@code /#/module/xxx/12} 点开只会停在首页，
     * 看不到那条到期记录 —— 邮件与 Web Push 两条通道此前都中招，故一并修正。
     *
     * <p>模块 key 缺失时退回首页 {@code "/"}：与其打开一个必然加载失败的详情页，
     * 不如让用户至少落在首页。
     */
    private String buildRecordPath(String moduleKey, Record record) {
        if (MODULE_KEY_UNKNOWN.equals(moduleKey)) {
            return "/";
        }
        return String.format("/module/%s/%d", moduleKey, record.getId());
    }

    /**
     * 站内路径 → 绝对 URL，仅邮件等<b>站外入口</b>需要（Web Push 用路径，见 {@link #buildRecordPath}）。
     * <p>容忍 {@code homelog.frontend.base-url} 末尾多余的 {@code /}，避免出现双斜杠。
     */
    private String toAbsoluteUrl(String path) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }
}