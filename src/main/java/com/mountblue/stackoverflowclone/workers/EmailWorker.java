package com.mountblue.stackoverflowclone.workers;

import com.mountblue.stackoverflowclone.dtos.EmailTaskDto;
import com.mountblue.stackoverflowclone.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailWorker implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(EmailWorker.class);

    private final EmailQueue queue;
    private final EmailService emailService;
    private final int maxAttempts;

    public EmailWorker(EmailQueue queue, EmailService emailService){
        this.queue = queue;
        this.emailService = emailService;
        this.maxAttempts = 3;
    }

    @Override
    public void run(){
        while(true){
            try {
                EmailTaskDto emailTaskDto = queue.take();
                boolean sent = attemptSendWithRetries(emailTaskDto);
                if(!sent){
                    logger.error("Exhausted retries for email to {}", emailTaskDto.getEmail());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public boolean attemptSendWithRetries(EmailTaskDto emailTaskDto){
        long backoffMs = 1000L;
        while(emailTaskDto.getAttempts() < maxAttempts){
            emailTaskDto.incrementAttempts();
            boolean sent = emailService.sendEmail(emailTaskDto);
            if(sent){
                return true;
            }
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            backoffMs = Math.min(backoffMs * 2, 8000L);
        }
        return false;
    }
}
