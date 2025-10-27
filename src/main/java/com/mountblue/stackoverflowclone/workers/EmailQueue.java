package com.mountblue.stackoverflowclone.workers;

import com.mountblue.stackoverflowclone.dtos.EmailTaskDto;

import org.springframework.stereotype.Component;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
public class EmailQueue {

    private final BlockingQueue<EmailTaskDto> queue = new ArrayBlockingQueue<>(50);

    public void enqueue(EmailTaskDto task) throws InterruptedException {
        queue.put(task);
    }

    public boolean offer(EmailTaskDto task) {
        return queue.offer(task);
    }

    public EmailTaskDto take() throws InterruptedException {
        return queue.take();
    }

    public EmailTaskDto poll() {
        return queue.poll();
    }
}
