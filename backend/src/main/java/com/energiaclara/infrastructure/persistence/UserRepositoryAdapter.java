package com.energiaclara.infrastructure.persistence;

import com.energiaclara.domain.model.Role;
import com.energiaclara.domain.model.User;
import com.energiaclara.domain.model.vo.Email;
import com.energiaclara.domain.model.vo.TenantId;
import com.energiaclara.domain.model.vo.UserId;
import com.energiaclara.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        jpaRepository.save(toEntity(user));
        return user;
    }

    @Override
    public Optional<User> findByEmailAndTenantId(Email email, TenantId tenantId) {
        return jpaRepository.findByEmailAndTenantId(email.value(), tenantId.value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmailAndTenantId(Email email, TenantId tenantId) {
        return jpaRepository.existsByEmailAndTenantId(email.value(), tenantId.value());
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setTenantId(user.getTenantId().value());
        entity.setEmail(user.getEmail().value());
        entity.setHashedPassword(user.getHashedPassword());
        entity.setRoles(user.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        return new User(
                UserId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                Email.of(entity.getEmail()),
                entity.getHashedPassword(),
                roles,
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
