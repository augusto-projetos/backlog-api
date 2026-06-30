package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    
}