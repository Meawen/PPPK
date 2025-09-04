package com;

import com.pppk.app.util.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.pppk")
@EnableJpaRepositories(basePackages = "com.pppk.patients.patients_infra.Repos")
@EntityScan(basePackages = "com.pppk.patients.patients_infra.Entities")
public class PppkApplication {



    static {
        DotenvInitializer.init();
    }

    public static void main(String[] args) {
        SpringApplication.run(PppkApplication.class, args);
    }

}
