package cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 */
@SpringBootApplication
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
// controller, repository, service (DTOs, DAOs), Repository

// API <-> Usercontroller [UserRequest] <-> Userservice <-> UserRepository

// how to user docker compose,