package com.invoice.generator.config;

import com.invoice.generator.service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupTaskRunner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("Application started. Running startup task to check for due recurring invoices.");
        scheduledTaskService.processRecurringInvoices();
    }
}