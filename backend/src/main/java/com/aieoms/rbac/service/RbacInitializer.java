package com.aieoms.rbac.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RbacInitializer implements ApplicationRunner {

    private final RbacService rbacService;

    public RbacInitializer(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public void run(ApplicationArguments args) {
        rbacService.initializeRoles();
    }
}
