package cache.controller;

import cache.entity.User;
import cache.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("/v1/users")
public class UserController {

    UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        User saved = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id){
        // localhost:8080/v1/users/7181
        User user = userService.getUserById(id);
        if(user == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user not found", "id", id));
        }
        return ResponseEntity.ok(user);
    }

}
