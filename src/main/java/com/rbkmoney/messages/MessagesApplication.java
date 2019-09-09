package com.rbkmoney.messages;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@EnableJdbcRepositories
@ServletComponentScan
@SpringBootApplication
public class MessagesApplication extends SpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagesApplication.class, args);
    }

}
