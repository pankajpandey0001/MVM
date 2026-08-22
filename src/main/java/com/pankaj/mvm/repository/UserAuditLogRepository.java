package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    List<UserAuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    void deleteByUserId(Long userId);
}