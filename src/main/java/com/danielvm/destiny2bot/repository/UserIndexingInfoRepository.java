package com.danielvm.destiny2bot.repository;

import com.danielvm.destiny2bot.entity.UserIndexingInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIndexingInfoRepository extends JpaRepository<UserIndexingInformation, Long> {

}
