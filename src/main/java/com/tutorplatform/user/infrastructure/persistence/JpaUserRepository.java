package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {
    private final UserDatabaseRepository databaseRepository;

    JpaUserRepository(UserDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public UserEntity saveAndFlush(UserEntity user) {
        UserDatabaseModel model = databaseRepository.findById(user.getId())
                .orElseGet(() -> new UserDatabaseModel(user));
        model.updateFrom(user);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override public Optional<UserEntity> findById(UUID id) {
        return databaseRepository.findById(id).map(UserDatabaseModel::toEntity);
    }
    @Override public Optional<UserEntity> findByEmail(String email) {
        return databaseRepository.findByEmail(email).map(UserDatabaseModel::toEntity);
    }
    @Override public boolean existsByEmail(String email) { return databaseRepository.existsByEmail(email); }
    @Override public long count() { return databaseRepository.count(); }
    @Override public void deleteAll() { databaseRepository.deleteAll(); }
}
