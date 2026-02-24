package com.deepak.Attendance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MonitoringStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringStartupLogger.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("==========================================================================");
        logger.info("🚀 Monitoring Stack is available!");
        logger.info("📊 Prometheus: http://localhost:9090");
        logger.info("📈 Grafana:    http://localhost:3000 (Default user: admin / admin)");
        logger.info("📱 App Metrics: http://localhost:{}/actuator/prometheus", 
                    event.getApplicationContext().getEnvironment().getProperty("local.server.port", "8081"));
        logger.info("==========================================================================");
    }
}
