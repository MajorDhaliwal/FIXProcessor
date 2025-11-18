package com.example.fixparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "com.example.fixparser",    
        "com.example.fixreport"     
})
public class FixApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixApiApplication.class, args);
    }
}
