package com.hdw.platform.piivalidator;

/**
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>This file is licensed under the Apache License, Version 2.0 (the "License"). You may not use
 * this file except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0/
 *
 * <p>This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class StreamsRecordProcessor implements IRecordProcessor {

  private final PIIService piiService;

  private Integer checkpointCounter;

  private final AmazonDynamoDB dynamoDBClient;
  private final String tableName;

  private PIIConfig piiConfig;

  private DatabaseOperations databaseOperations;

  private final HashSet<String> stringsToIgnore = new HashSet<String>(
      List.of("NA", "NULL", "EMPTY", "na", "null", "empty", "undefined", "UNDEFINED"));

  public StreamsRecordProcessor(AmazonDynamoDB dynamoDBClient2, String tableName,
      PIIService piiService) {
    this.dynamoDBClient = dynamoDBClient2;
    this.tableName = tableName;
    this.piiService = piiService;
    this.databaseOperations = new DatabaseOperations(piiService);
  }

  @Override
  public void initialize(InitializationInput initializationInput) {

    setConfigFromDatabase();

    checkpointCounter = 0;
  }

  private Map<String, Object> convertDDBItemToHumanReadableItem(
      Map<String, AttributeValue> record) {
    HashMap<String, Object> readValue;
    try {
      String shit = ItemUtils.toItem(record).toJSON();

//      shit = shit.replaceAll("NULL", "S");

      ObjectMapper mapper = new ObjectMapper();
      readValue = mapper.readValue(shit, HashMap.class);
      log.debug("final object : {}", readValue);
      return readValue;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void processRecords(ProcessRecordsInput processRecordsInput) {

    for (Record record : processRecordsInput.getRecords()) {
      String data = new String(record.getData().array(), StandardCharsets.UTF_8);
      if (record instanceof RecordAdapter) {
        com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) record).getInternalObject();

        switch (streamRecord.getEventName()) {
          case "INSERT", "MODIFY" -> {
            try {

              final Map<String, Object> unmarshalledDDBItem = convertDDBItemToHumanReadableItem(
                  streamRecord.getDynamodb().getNewImage());

              log.info("Got new record for processing : {} going for validation",
                  streamRecord.getDynamodb().getKeys());

              validateRecordForPII(unmarshalledDDBItem, streamRecord.getDynamodb().getKeys());

            } catch (Exception e) {
              log.error("Failed to validated record: {}", streamRecord.getDynamodb().getKeys(), e);
            }
          }
          default -> log.debug("Un Handled type of event : {} pk-sk : {}, skipping ...",
              streamRecord.getEventName(), streamRecord.getDynamodb().getKeys());
        }

      } else {
        log.debug("Unknown Record type in ddb stream : {}", data);
      }

      checkpointCounter += 1;
      if (checkpointCounter % 10 == 0) {
        try {
          processRecordsInput.getCheckpointer().checkpoint();
        } catch (Exception error) {
          log.error("failed to checkpoint", error);
        }
      }
    }
  }

  private void setConfigFromDatabase() {
    HashMap<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

    keyToGet.put("_id", new AttributeValue("pf-pii-keys"));
    keyToGet.put("type", new AttributeValue("pii"));

    GetItemRequest request = new GetItemRequest();
    request.setTableName("settings");
    request.setKey(keyToGet);
    Map<String, AttributeValue> ding = this.dynamoDBClient.getItem(request).getItem();

    PIIConfig readValue;

    try {
      String shit = ItemUtils.toItem(ding).toJSON();

      ObjectMapper mapper = new ObjectMapper();
      readValue = mapper.readValue(shit, PIIConfig.class);
      log.info("PII config : {}", readValue);
      this.piiConfig = readValue;

      // merging pii keys and indexed keys as in the end they are both considered  PII
      this.piiConfig.getPiiKeys().addAll(this.piiConfig.getIndexedKeys());
    } catch (JsonProcessingException e) {
      log.error("Unable to fetch pii config from database setting default values", e);
      this.piiConfig = PIIConfig.builder().enabled(true)
          .piiKeys(Set.of("mobile", "email", "accountNumber")).build();
    }
  }

  private void validateRecordForPII(Map<String, Object> record, Map<String, AttributeValue> keys)
      throws Exception {
    final PIIResponse response = piiService.runTheMagic("/decrypt",
        PIIRequest.builder().serviceCallingAPI("platform-user").apiName("pii_validator")
            .data(record).build());

    final ConsolidatedErrorForLogging consolidatedErrorForLogging = new ConsolidatedErrorForLogging();

    validateDDBDataWithEncryptedData(record, response.getData(), keys, consolidatedErrorForLogging,
        "");

    if (consolidatedErrorForLogging.isError) {
      log.error("validation failed for pk-sk : {} log : {}", keys, consolidatedErrorForLogging);

      // encrypt the item if keys are missing
      if (!consolidatedErrorForLogging.missingKeys.isEmpty()) {
        // TODO: A Big No !!
        // Note: Updating same table from which stream is running never a good idea specially users
        // Loop Incident with preferences stored as Bool in mobile :(

        // databaseOperations.updateItemWithEncryptedValues(this.tableName, record, keys);
      }
    }
  }


  public void validateDDBDataWithEncryptedData(
      Map<String, Object> ddbRecord,
      Map<String, Object> decryptedDDBRecord,
      Map<String, AttributeValue> keys,
      ConsolidatedErrorForLogging consolidatedErrorForLogging,
      String keyPath) {

    log.debug("keys for which validation has to be done : {} {}", ddbRecord.keySet(), ddbRecord);

    for (String key : ddbRecord.keySet()) {

      log.debug("Starting validation for key : {} and type of data : {}", key, ddbRecord.get(key));

      String keyPathIncludingCurrentKey =
          keyPath.isEmpty() ? key : String.join(".", keyPath, key);

      if (ddbRecord.get(key) != null
          && ddbRecord.get(key) instanceof Map
          && !this.piiConfig.getNottoloop().contains(key)) {

        validateDDBDataWithEncryptedData(
            (Map<String, Object>) ddbRecord.get(key),
            (Map<String, Object>) decryptedDDBRecord.get(key),
            keys,
            consolidatedErrorForLogging,
            keyPathIncludingCurrentKey
        );

      } else {

//        if (!this.piiConfig.getPiiKeys().contains(key)) {
//          log.debug("Skipping validation for key : {}", key);
//          continue;
//        }

        if (!this.piiConfig.getKycFieldsToCheck().contains(key)) {
          log.debug("Skipping validation for key : {}", key);
          continue;
        }

        if (ddbRecord.containsKey(key) && ddbRecord.containsKey(key + "_encrypted")) {

          if (ddbRecord.get(key).toString().equals(ddbRecord.get(key + "_encrypted").toString())) {
            log.debug(
                "encrypted_key data invalid for pk-sk : {} key : {} plaintext : {} encrypted : {}",
                keys, key, ddbRecord.get(key), ddbRecord.get(key + "_encrypted"));

            consolidatedErrorForLogging.isError = true;
            consolidatedErrorForLogging.invalidKeysAndTheirValue.put(keyPath,
                MismatchValue.builder()
                    .plaintext(ddbRecord.get(key).toString())
                    .encryptedText(ddbRecord.get(key + "_encrypted").toString())
                    .build());

          } else if (!ddbRecord.get(key).equals(decryptedDDBRecord.get(key + "_encrypted"))) {
            log.debug(
                "encrypted data does not match for pk-sk : {} key : {} plaintext : {} encrypted : {}",
                keys, key, ddbRecord.get(key), decryptedDDBRecord.get(key + "_encrypted"));

            consolidatedErrorForLogging.isError = true;

            consolidatedErrorForLogging.mismatchKey.put(keyPath, MismatchValue.builder()
                .plaintext(ddbRecord.get(key).toString())
                .encryptedText(decryptedDDBRecord.get(key + "_encrypted").toString())
                .build());
          }
        }
        if (ddbRecord.containsKey(key)
            && Objects.nonNull(ddbRecord.get(key))
            && !Strings.isEmpty(ddbRecord.get(key).toString())
            && !stringsToIgnore.contains(ddbRecord.get(key).toString())
            && !ddbRecord.containsKey(key + "_encrypted")) {
          consolidatedErrorForLogging.isError = true;
          log.debug(
              "Encrypted key not found for pk-sk : {} key : {} plaintext : {} encryptedd : {}",
              keys, key, ddbRecord.get(key), decryptedDDBRecord.get(key + "_encrypted"));

          consolidatedErrorForLogging.missingKeys.add(keyPathIncludingCurrentKey);
        }
      }
    }
  }

  @Override
  public void shutdown(ShutdownInput shutdownInput) {
    if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
      log.warn("Received Termination, gracefully closing checkpointing ddb streams");
      try {
        shutdownInput.getCheckpointer().checkpoint();
      } catch (Exception e) {
        log.error("failed to gracefully shutdown checkpointing", e);
      }
    }
  }
}
