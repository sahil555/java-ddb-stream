package com.hdw.platform.piivalidator;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class DatabaseOperations {

  private final AmazonDynamoDB ddbClient;
  private final PIIService piiService;

  public DatabaseOperations(PIIService piiService) {
    this.ddbClient = AmazonDynamoDBClientBuilder.defaultClient();
    this.piiService = piiService;
  }

  public void updateItemWithEncryptedValues(
      String tableName,
      Map<String, Object> record,
      Map<String, AttributeValue> keys) throws Exception {
    try {

      log.info("Encrypting Item for pk-sk : {}", keys);

      PIIRequest piiRequest = PIIRequest.builder()
          .data(record)
          .serviceCallingAPI("platform-user")
          .apiName("validator_updating_item_with_encrypted_values")
          .build();
      PIIResponse piiResponse = this.piiService.runTheMagic("encrypt", piiRequest);

      log.debug("Encrypted Record for pk-sk : {} record : {}", keys, piiResponse);

      Map<String, AttributeValue> item = ItemUtils.fromSimpleMap(piiResponse.getData());

      // Adding identifier to know its inserted from validator
      item.put("inserted_from_validator", new AttributeValue().withBOOL(true));
      item.put("updated_at_from_validator", new AttributeValue().withS(
          String.valueOf(System.currentTimeMillis())));

      log.debug("Encrypted Item for pk-sk : {} {}", keys, item);

      PutItemRequest putItemRequest = new PutItemRequest();
      putItemRequest.setTableName(tableName);
      putItemRequest.setReturnConsumedCapacity("TOTAL");
      putItemRequest.setReturnValuesOnConditionCheckFailure("ALL_OLD");
      putItemRequest.setItem(item);

      log.info("Updating Item with encrypted values for pk-sk : {}", keys);

      PutItemResult putItemResult = ddbClient.putItem(putItemRequest);

      log.info("Updated Item with encrypted for pk-sk : {} putItemResult : {}", keys,
          putItemResult);

    } catch (Exception exception) {
      // Not retrying the update as it could overwrite actual data
      // please make manual update mostly in case of Conditional Checks :)
      log.error(
          "Failed to update item with encrypted values with pk-sk : {} please update manually",
          keys, exception);
    }
  }
}
