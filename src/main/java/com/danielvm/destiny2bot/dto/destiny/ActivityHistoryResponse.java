package com.danielvm.destiny2bot.dto.destiny;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityHistoryResponse {

  private List<Activity> activities;

}
