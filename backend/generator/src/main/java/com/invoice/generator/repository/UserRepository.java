package com.invoice.generator.repository;

import com.invoice.generator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Add this line. Spring Data JPA will automatically understand
    // how to implement it because of the special naming convention.
    Optional<User> findByUsername(String username);

}