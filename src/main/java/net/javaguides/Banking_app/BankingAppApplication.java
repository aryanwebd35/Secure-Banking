package net.javaguides.Banking_app;
// ↑ "package" is like a folder name for this class. It tells Java where this file lives in the project.
// Think of it like a file path: net/javaguides/Banking_app/

import org.springframework.boot.SpringApplication;
// ↑ We import SpringApplication — a helper class that knows how to START a Spring Boot project.

import org.springframework.boot.autoconfigure.SpringBootApplication;
// ↑ We import @SpringBootApplication annotation — one annotation that does THREE things at once (see below).

// ============================================================
// @SpringBootApplication is a SUPER ANNOTATION. It combines:
//   1. @Configuration      → This class can define Spring "beans" (objects Spring manages)
//   2. @EnableAutoConfiguration → Spring Boot auto-configures database, security etc. for us
//   3. @ComponentScan      → Spring Boot scans this package and all sub-packages to find
//                            controllers, services, repositories, etc.
// In simple words: "This is the starting point of the whole app. Set everything up automatically."
// ============================================================
@SpringBootApplication
public class BankingAppApplication {

    // This is THE MAIN METHOD — the first thing Java runs when you start the app.
    // Every Java program starts from a method called "main".
    public static void main(String[] args) {

        // SpringApplication.run() does all the heavy lifting:
        //   - Creates the application context (Spring's container that holds all objects)
        //   - Starts an embedded web server (Tomcat by default) on port 8080
        //   - Scans packages for @Controller, @Service, @Repository etc.
        //   - Connects to the MySQL database using settings from application.properties
        SpringApplication.run(BankingAppApplication.class, args);

        // After this line runs, your REST API is LIVE at http://localhost:8080
    }
}
