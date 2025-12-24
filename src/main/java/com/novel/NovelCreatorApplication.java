package com.novel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI网文创作系统主启动类
 * 
 * @author NovelCreator
 * @version 1.0.0
 */
@SpringBootApplication
public class NovelCreatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelCreatorApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  AI Novel Creator Started Successfully!   ");
        System.out.println("  Access: http://localhost:8080            ");
        System.out.println("  H2 Console: http://localhost:8080/h2-console");
        System.out.println("===========================================");
    }
}
