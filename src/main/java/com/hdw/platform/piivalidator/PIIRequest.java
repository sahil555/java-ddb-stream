package com.hdw.platform.piivalidator;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Builder
@Component
@NoArgsConstructor
@AllArgsConstructor
public class PIIRequest {
  @JsonProperty("service_calling_pii")
  private String serviceCallingAPI;

  @JsonProperty("api_name")
  private String apiName;

  @JsonProperty("data")
  private Map<String, Object> data;
}
