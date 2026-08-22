package com.pankaj.mvm.config;

import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import com.pankaj.mvm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. If database is completely empty, create the accounts
        if (userRepository.count() == 0) {
            log.info("Database is empty. Seeding demo accounts...");

            // Super Admin
            User superAdmin = new User();
            superAdmin.setFullName("Super Demo Admin");
            superAdmin.setEmail("superadmin@mvm.com");
            superAdmin.setPhone("9999999991");
            superAdmin.setPassword(passwordEncoder.encode("Password@123"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setStatus(AccountStatus.ACTIVE);
            userRepository.save(superAdmin);

            // Admins
            for (int i = 1; i <= 2; i++) {
                User admin = new User();
                admin.setFullName("Demo Admin " + i);
                admin.setEmail("admin" + i + "@mvm.com");
                admin.setPhone("988888888" + i);
                admin.setPassword(passwordEncoder.encode("Password@123"));
                admin.setRole(Role.ADMIN);
                admin.setStatus(AccountStatus.ACTIVE);
                userRepository.save(admin);
            }

            // Vendors
            for (int i = 1; i <= 4; i++) {
                User vendor = new User();
                vendor.setFullName("Demo Vendor " + i);
                vendor.setEmail("vendor" + i + "@mvm.com");
                vendor.setPhone("877777777" + i);
                vendor.setPassword(passwordEncoder.encode("Password@123"));
                vendor.setRole(Role.VENDOR);
                vendor.setStatus(AccountStatus.ACTIVE);
                userRepository.save(vendor);
            }

            log.info("Successfully seeded demo accounts!");
        }

        // 2. SAFETY CHECK: Force ALL users in the database to be ACTIVE so nobody gets blocked by approval walls during your demo
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getStatus() != AccountStatus.ACTIVE) {
                user.setStatus(AccountStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        log.info("Checked and verified that all database users are ACTIVE.");
    }
}
