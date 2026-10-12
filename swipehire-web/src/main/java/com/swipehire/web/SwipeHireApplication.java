package com.swipehire.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Boot entry point. Extending SpringBootServletInitializer lets the same WAR run on an external
 * Tomcat; @ServletComponentScan registers our raw @WebServlet classes alongside Spring MVC.
 */
@SpringBootApplication
@ServletComponentScan
public class SwipeHireApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SwipeHireApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SwipeHireApplication.class);
    }
}
