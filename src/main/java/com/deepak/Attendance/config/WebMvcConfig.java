package com.deepak.Attendance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin/login").setViewName("forward:/login.html");
        registry.addViewController("/admin/dashboard").setViewName("forward:/dashboard.html");
        registry.addViewController("/admin/").setViewName("forward:/dashboard.html");
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path for captcha images - must end with /
        Path captchaDir = Paths.get("uploads/captcha").toAbsolutePath();
        String captchaPath = "file:///" + captchaDir.toString().replace("\\", "/") + "/";
        
        log.info("Configuring static resource handler for CAPTCHA images: {}", captchaPath);
        
        // Serve CAPTCHA images from uploads/captcha folder
        registry.addResourceHandler("/captcha-images/**")
                .addResourceLocations(captchaPath)
                .setCachePeriod(0);
    }
}
