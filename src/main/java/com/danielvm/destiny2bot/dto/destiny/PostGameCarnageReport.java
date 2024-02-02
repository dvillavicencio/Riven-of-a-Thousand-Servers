package com.danielvm.destiny2bot.dto.destiny;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostGameCarnageReport implements Serializable {

  private Instant period;

  private Boolean activityWasStartedFromBeginning;

  private List<PGCREntry> entries;
}
