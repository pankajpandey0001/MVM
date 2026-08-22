package com.pankaj.mvm.service;

import com.pankaj.mvm.dto.StatusChangeRequest;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.UserAuditLog;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import com.pankaj.mvm.exceptions.ApiException;
import com.pankaj.mvm.repository.UserAuditLogRepository;
import com.pankaj.mvm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserGovernanceService {

    private final UserRepository userRepository;
    private final UserAuditLogRepository auditLogRepository;

    private static final Map<AccountStatus, Set<AccountStatus>> PERMITTED_TRANSITIONS = new EnumMap<>(AccountStatus.class);

    static {
        PERMITTED_TRANSITIONS.put(AccountStatus.PENDING,
                EnumSet.of(AccountStatus.ACTIVE, AccountStatus.BLACKLISTED));
        PERMITTED_TRANSITIONS.put(AccountStatus.ACTIVE,
                EnumSet.of(AccountStatus.SUSPENDED, AccountStatus.DEACTIVATED, AccountStatus.TERMINATED));
        PERMITTED_TRANSITIONS.put(AccountStatus.SUSPENDED,
                EnumSet.of(AccountStatus.ACTIVE, AccountStatus.TERMINATED));
        PERMITTED_TRANSITIONS.put(AccountStatus.DEACTIVATED,
                EnumSet.of(AccountStatus.ACTIVE, AccountStatus.TERMINATED));
        PERMITTED_TRANSITIONS.put(AccountStatus.BLACKLISTED,
                EnumSet.noneOf(AccountStatus.class));

        // Allow TERMINATED accounts to be approved/reactivated back to ACTIVE
        PERMITTED_TRANSITIONS.put(AccountStatus.TERMINATED,
                EnumSet.of(AccountStatus.ACTIVE));
    }

    public Page<User> getUsersByStatusAndRole(AccountStatus status, Role role, Pageable pageable) {
        if (role != null) {
            return userRepository.findByStatusAndRole(status, role, pageable);
        }
        return userRepository.findByStatus(status, pageable);
    }

    public Map<String, Long> getStatusTabCountsByRole(Role role) {
        Map<String, Long> counts = new HashMap<>();
        for (AccountStatus status : AccountStatus.values()) {
            long count = userRepository.countByStatusAndRole(status, role);
            counts.put(status.name(), count);
        }
        return counts;
    }

    public Map<String, Long> getStatusTabCounts() {
        Map<String, Long> stringCounts = new HashMap<>();
        for (AccountStatus status : AccountStatus.values()) {
            if (status != AccountStatus.REJECTED) {
                stringCounts.put(status.name(), userRepository.countByStatus(status));
            }
        }
        return stringCounts;
    }

    public List<UserAuditLog> getUserAuditTrail(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException("User not found with ID: " + userId, HttpStatus.NOT_FOUND);
        }
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional
    public void rejectAndPurgeUser(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId, HttpStatus.NOT_FOUND));

        if (targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new ApiException("Super Administrator accounts cannot be deleted.", HttpStatus.FORBIDDEN);
        }

        // Only allow PENDING and BLACKLISTED accounts to be purged; block TERMINATED accounts
        if (targetUser.getStatus() != AccountStatus.PENDING && targetUser.getStatus() != AccountStatus.BLACKLISTED) {
            throw new ApiException("Only PENDING or BLACKLISTED accounts can be purged. Terminated accounts must be preserved for historical records.", HttpStatus.BAD_REQUEST);
        }

        // Clean dependent audit trail prior to permanent user purge
        auditLogRepository.deleteByUserId(userId);
        userRepository.delete(targetUser);
    }

    @Transactional
    public User transitionUserStatus(Long userId, StatusChangeRequest request) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId, HttpStatus.NOT_FOUND));

        if (targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new ApiException("Super Administrator status is immutable and protected from modification.", HttpStatus.FORBIDDEN);
        }

        AccountStatus currentStatus = targetUser.getStatus();
        AccountStatus targetStatus = request.getTargetStatus();

        if (currentStatus == targetStatus) {
            throw new ApiException("User is already in " + currentStatus + " status.", HttpStatus.BAD_REQUEST);
        }

        Set<AccountStatus> allowedNextStates = PERMITTED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(AccountStatus.class));
        if (!allowedNextStates.contains(targetStatus)) {
            throw new ApiException(
                    "Invalid transition: Cannot move account from " + currentStatus + " to " + targetStatus + ".",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Extract the exact email address of the acting administrator
        String loggedInAdminEmail = resolveCurrentAdminEmail();

        UserAuditLog auditLog = UserAuditLog.builder()
                .user(targetUser)
                .previousStatus(currentStatus)
                .newStatus(targetStatus)
                .actionReason(request.getReason().trim())
                .performedByEmail(loggedInAdminEmail)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);

        targetUser.setStatus(targetStatus);
        return userRepository.save(targetUser);
    }

    /**
     * Resolves the logged-in administrator's email safely from SecurityContext.
     */
    private String resolveCurrentAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system@domain.local";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getEmail();
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String emailStr && emailStr.contains("@")) {
            return emailStr;
        }

        return auth.getName();
    }
}