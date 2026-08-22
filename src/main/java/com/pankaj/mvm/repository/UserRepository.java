package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Page<User> findByStatusAndRole(AccountStatus status, Role role, Pageable pageable);
    Page<User> findByStatus(AccountStatus status, Pageable pageable);
    long countByStatus(AccountStatus status);
    long countByStatusAndRole(AccountStatus status, Role role);
}