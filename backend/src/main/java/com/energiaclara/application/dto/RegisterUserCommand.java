package com.energiaclara.application.dto;

import com.energiaclara.domain.model.Role;
import com.energiaclara.domain.model.vo.Email;
import com.energiaclara.domain.model.vo.TenantId;

import java.util.Set;

public record RegisterUserCommand(TenantId tenantId, Email email, String rawPassword, Set<Role> roles) {}
