package com.homelog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 邮件发送服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${homelog.remind.from}")
    private String fromEmail;

    /**
     * 发送邮件
     */
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            doSend(toEmail, subject, htmlBody);
            log.info("邮件发送成功：收件人={}", toEmail);
        } catch (Exception e) {
            // 定时任务等主流程不能因单封邮件失败而中断，这里只记录不外抛
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * 同步发送邮件并向上抛出真实异常 —— 供测试接口定位 SMTP 连通性/认证/网络根因。
     * <p>与 {@link #sendEmail} 的区别：后者吞异常保证提醒任务不中断，前者绝不吞，便于排查。
     */
    public void sendEmailSync(String toEmail, String subject, String htmlBody) throws MessagingException {
        doSend(toEmail, subject, htmlBody);
        log.info("邮件发送成功（同步）：收件人={}", toEmail);
    }

    private void doSend(String toEmail, String subject, String htmlBody) throws MessagingException {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(mime);
    }
}