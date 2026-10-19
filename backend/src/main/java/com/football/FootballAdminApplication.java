package com.football;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.football.mapper")
public class FootballAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballAdminApplication.class, args);
    }
}
