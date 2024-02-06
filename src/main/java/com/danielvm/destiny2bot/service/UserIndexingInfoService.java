package com.danielvm.destiny2bot.service;

import com.danielvm.destiny2bot.repository.UserIndexingInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserIndexingInfoService {

  private final UserIndexingInfoRepository userIndexingInfoRepository;

  public UserIndexingInfoService(UserIndexingInfoRepository userIndexingInfoRepository) {
    this.userIndexingInfoRepository = userIndexingInfoRepository;
  }

}
