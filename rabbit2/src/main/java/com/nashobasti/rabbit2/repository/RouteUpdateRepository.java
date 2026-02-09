package com.nashobasti.rabbit2.repository;

import com.nashobasti.rabbit2.entity.RouteUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteUpdateRepository extends JpaRepository<RouteUpdate, Long> {
}
