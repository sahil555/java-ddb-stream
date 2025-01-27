package com.hdw.platform.piivalidator;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Component
public class PIIResponse {

  private int httpStatusCode;

  @JsonProperty("message")
  private String message;

  @JsonProperty("data")
  private Map<String, Object> data;
}
