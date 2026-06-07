package org.personal.library.dao;

import org.personal.library.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    java.util.List<Role> findByNameIn(java.util.Collection<String> names);
}
