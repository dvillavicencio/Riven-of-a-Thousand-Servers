package com.danielvm.destiny2bot.dto.destiny;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponse<T> {

  /**
   * Most of the responses from Bungie.net have a Json element named 'Response' with arbitrary info
   * depending on the endpoint. This field is just a generic-wrapper for it.
   */
  @JsonAlias("Response")
  @Nullable
  private T response;
}
