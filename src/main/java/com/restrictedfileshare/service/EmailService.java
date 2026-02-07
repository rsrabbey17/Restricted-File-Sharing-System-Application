package com.restrictedfileshare.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Async
    public void sendVerificationEmail(String to, String verificationLink) {
        // FOR TESTING - NO EMAIL SENDING
        // Just print to console so you can copy the verification link

        log.info("\n");
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║                  VERIFICATION LINK (TEST MODE)                  ║");
        log.info("╠══════════════════════════════════════════════════════════════════╣");
        log.info("║ Email: {}", to);
        log.info("║ Verification Link: {}", verificationLink);
        log.info("║ Expires in: 10 minutes");
        log.info("║ Copy this link and use it in Postman or browser to verify");
        log.info("╚══════════════════════════════════════════════════════════════════╝");
        log.info("\n");

        //  IMPORTANT: This DOES NOT send real email!
        // It just prints to console for testing
    }
}