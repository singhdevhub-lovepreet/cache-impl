package cache.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User implements Serializable { // sql

    private static final long serialVersionUID = 1L;

    // id, email, name, createdAt

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // camelCase, created_at, createdAt

}
