package com.saadaoui.tunidesk.config;

import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.RoleRepository;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.category.repository.CategoryRepository;
import com.saadaoui.tunidesk.entity.SlaPolicy;
import com.saadaoui.tunidesk.entity.TicketCategory;
import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.sla.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "application.seed-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final SlaPolicyRepository slaPolicyRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        seedRoles();

        seedUsers();

        seedCategories();

        seedSlaPolicies();
    }

    private void seedRoles() {

        if (roleRepository.count() > 0) {
            return;
        }

        roleRepository.saveAll(List.of(
                Role.builder().name("ADMIN").description("Platform administrator").build(),
                Role.builder().name("AGENT").description("Support / helpdesk agent").build(),
                Role.builder().name("USER").description("End user submitting tickets").build()
        ));

        log.info("Seeded default roles");
    }

    private void seedUsers() {

        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = requireRole("ADMIN");
        Role agentRole = requireRole("AGENT");
        Role userRole = requireRole("USER");

        userRepository.saveAll(List.of(
                User.builder()
                        .firstName("Admin")
                        .lastName("TuniDesk")
                        .email("admin@tunidesk.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .roles(Set.of(adminRole))
                        .build(),
                User.builder()
                        .firstName("Support")
                        .lastName("Agent")
                        .email("agent@tunidesk.com")
                        .password(passwordEncoder.encode("Agent@123"))
                        .roles(Set.of(agentRole))
                        .build(),
                User.builder()
                        .firstName("Demo")
                        .lastName("Employee")
                        .email("user@tunidesk.com")
                        .password(passwordEncoder.encode("User@123"))
                        .roles(Set.of(userRole))
                        .build()
        ));

        log.info("Seeded default users: admin@tunidesk.com / agent@tunidesk.com / user@tunidesk.com");
    }

    private void seedCategories() {

        if (categoryRepository.count() > 0) {
            return;
        }

        Map<String, String> categories = new LinkedHashMap<>();

        categories.put("Hardware", "Physical devices, peripherals and equipment issues");
        categories.put("Software", "Applications, operating systems and licenses");
        categories.put("Network", "Connectivity, VPN and network infrastructure");
        categories.put("Access & Accounts", "Login issues, permissions and account provisioning");
        categories.put("Other", "Anything that does not fit another category");

        categories.forEach((name, description) -> categoryRepository.save(
                TicketCategory.builder()
                        .name(name)
                        .description(description)
                        .build()));

        log.info("Seeded default ticket categories");
    }

    private void seedSlaPolicies() {

        if (slaPolicyRepository.count() > 0) {
            return;
        }

        slaPolicyRepository.saveAll(List.of(
                SlaPolicy.builder().name("Critical SLA").priority(TicketPriority.CRITICAL).hours(4).build(),
                SlaPolicy.builder().name("High SLA").priority(TicketPriority.HIGH).hours(8).build(),
                SlaPolicy.builder().name("Medium SLA").priority(TicketPriority.MEDIUM).hours(24).build(),
                SlaPolicy.builder().name("Low SLA").priority(TicketPriority.LOW).hours(48).build()
        ));

        log.info("Seeded default SLA policies");
    }

    private Role requireRole(String name) {

        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + name));
    }

}
