package com.mountblue.stackoverflowclone.workers;

import com.mountblue.stackoverflowclone.services.EmailService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EmailWorkerStarter implements CommandLineRunner {
    private final EmailQueue queue;
    private final EmailService emailService;

    public EmailWorkerStarter(EmailQueue queue, EmailService emailService){
        this.queue = queue;
        this.emailService = emailService;
    }

    @Override
    public void run(String... args) throws Exception {
        int numberOfThreads = 3;
        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new EmailWorker(queue, emailService));
            t.setDaemon(true);
            t.start();
        }
    }
}
