package com.yourname.moviematcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MoviematcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoviematcherApplication.class, args);
    }
}
