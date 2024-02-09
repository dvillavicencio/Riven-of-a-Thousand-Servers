package com.danielvm.destiny2bot.repository;

import com.danielvm.destiny2bot.entity.UserCharacterIndexing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCharacterIndexingRepository extends
    JpaRepository<UserCharacterIndexing, Long> {

}
