package com;

import com.pppk.app.util.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "com.pppk.patients.patients_infra.Repos",
        "com.pppk.exams.exams_infra.repos"
})
@EntityScan(basePackages = {
        "com.pppk.patients.patients_infra.Entities",
        "com.pppk.exams.exams_infra.entities"
})
@ComponentScan(basePackages = "com.pppk")
public class PppkApplication {



    static {
        DotenvInitializer.init();
    }

    public static void main(String[] args) {
        SpringApplication.run(PppkApplication.class, args);
    }

}
