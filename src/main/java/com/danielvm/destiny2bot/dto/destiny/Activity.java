package com.danielvm.destiny2bot.dto.destiny;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Activity {

  private Instant period;

  private ActivityDetails activityDetails;

  private Map<String, ActivityValue> values;
}
