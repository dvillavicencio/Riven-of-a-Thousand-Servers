package com.danielvm.destiny2bot.repository;

import com.danielvm.destiny2bot.entity.UserIndexing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIndexingRepository extends
    JpaRepository<UserIndexing, Long> {

}
