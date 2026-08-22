package com.pankaj.mvm.entity;

import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import com.pankaj.mvm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the database has no users yet
        if (userRepository.count() == 0) {
            log.info("Database is empty. Seeding requested demo accounts...");

            // 1. Create 1 Super Admin
            User superAdmin = new User();
            superAdmin.setFullName("Super Demo Admin");
            superAdmin.setEmail("superadmin@mvm.com");
            superAdmin.setPhone("9999999991");
            superAdmin.setPassword(passwordEncoder.encode("Password@123"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setStatus(AccountStatus.ACTIVE);
            userRepository.save(superAdmin);

            // 2. Create 2 Admins
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

            // 3. Create 4 Vendors
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

            log.info("Successfully seeded: 1 Super Admin, 2 Admins, and 4 Vendors!");
            log.info("All demo accounts use password: Password@123");
        } else {
            log.info("Users already exist in database. Skipping data seeding.");
        }
    }
}
