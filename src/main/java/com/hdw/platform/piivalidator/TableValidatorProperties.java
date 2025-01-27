package com.hdw.platform.piivalidator;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableValidatorProperties {

  public String tableName;
  public String streamArn;
  public String applicationName;
  public String serviceUrl;
  public String stage;
  public String clientId;
  public String clientSecret;
  public InitialPositionInStream positionToStartReadingInStream;

}
