package com.danielvm.destiny2bot.repository;

import com.danielvm.destiny2bot.entity.CharacterRaid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterRaidRepository extends JpaRepository<CharacterRaid, Long> {

}
