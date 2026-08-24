package com.aieoms.rbac.service;

import com.aieoms.rbac.entity.Role;
import com.aieoms.rbac.entity.UserRole;
import com.aieoms.rbac.repository.RoleRepository;
import com.aieoms.rbac.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacService {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public RbacService(
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public void initializeRoles() {
        createRoleIfMissing(
                ROLE_USER,
                "Standard authenticated user"
        );

        createRoleIfMissing(
                ROLE_OPERATOR,
                "Incident operations operator"
        );

        createRoleIfMissing(
                ROLE_ADMIN,
                "System administrator"
        );
    }

    @Transactional
    public void assignDefaultUserRole(Long userId) {

        Role role = roleRepository.findByName(ROLE_USER)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_USER not initialized"
                        ));

        if (!userRoleRepository.existsByUserIdAndRoleId(
                userId,
                role.getId()
        )) {
            userRoleRepository.save(
                    new UserRole(userId, role.getId())
            );
        }
    }

    private void createRoleIfMissing(
            String name,
            String description
    ) {

        if (!roleRepository.existsByName(name)) {

            Role role = new Role();
            role.setName(name);
            role.setDescription(description);

            roleRepository.save(role);
        }
    }
}
