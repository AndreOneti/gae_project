package br.com.andre.carvalho.gae_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@EnableResourceServer
@SpringBootApplication
public class GaeProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaeProjectApplication.class, args);
    }

}
