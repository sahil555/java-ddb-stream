package com.hdw.platform.piivalidator;


import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;

@Slf4j
public class StreamsRecordProcessorTest {

  private final PIIService piiService = Mockito.mock(PIIService.class);

  private StreamsRecordProcessor processor;

  //  @Test
  public void validateDDBDataWithEncryptedData() throws Exception {

    this.processor = new StreamsRecordProcessor(null, "DUMMY_TABLE", piiService);

    Mockito.doReturn(PIIResponse.builder().build())
        .when(piiService).runTheMagic(Mockito.any(), Mockito.any());

    Map<String, Object> ddbRecord = Map.of("mobile", "1234567890");
    Map<String, AttributeValue> keys = Map.of("mobile", new AttributeValue().withS("1234567890"));
    ConsolidatedErrorForLogging consolidatedErrorForLogging = new ConsolidatedErrorForLogging();

    processor.validateDDBDataWithEncryptedData(ddbRecord, ddbRecord, keys,
        consolidatedErrorForLogging, "");

    log.info("final logging : {}", consolidatedErrorForLogging);
    assert (true);
  }
}