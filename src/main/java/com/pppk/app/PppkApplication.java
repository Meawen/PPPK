package com.pppk.app;

import com.pppk.app.util.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PppkApplication {


    static {
        DotenvInitializer.init();
    }

    public static void main(String[] args) {
        SpringApplication.run(PppkApplication.class, args);
    }

}
