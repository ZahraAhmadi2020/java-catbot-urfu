package com.urfu.catbot.cat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(CatServiceApplication.class, args);
    System.out.println("  Cat Service started successfully! ");
    System.out.println(" Cat management microservice   ");
    System.out.println("  H2 Console: http://localhost:8081/h2-console");
  }
}
