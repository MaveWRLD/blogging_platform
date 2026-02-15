package org.amalitech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AppLauncher{

    public static void main(String[] args) {
        SpringApplication.run(AppLauncher.class, args);
    }

}

