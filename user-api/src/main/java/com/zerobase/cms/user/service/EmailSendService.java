package com.zerobase.cms.user.service;

import com.zerobase.cms.user.client.MailgunClient;
import com.zerobase.cms.user.client.mailgun.SendMailForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSendService {
    private final MailgunClient mailgunClient;

    public String sendEmail() {
        SendMailForm form = SendMailForm.builder()
                .from("csh980116@gmail.com")
                .to("csh980116@gmail.com")
                .subject("new Test email from zero base")
                .text("new cms module mailgun test")
                .build();
        return mailgunClient.sendEmail(form).getBody();
    }
}
