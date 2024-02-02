package com.danielvm.destiny2bot.repository;

import com.danielvm.destiny2bot.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

}
