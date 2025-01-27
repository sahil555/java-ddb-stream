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
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import java.time.Duration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {

  private final String tableName;
  private final AmazonDynamoDB dynamoDBClient;

  private final TableValidatorProperties properties;

  public StreamsRecordProcessorFactory(
      AmazonDynamoDB dynamoDBClient, String tableName, TableValidatorProperties properties) {
    this.tableName = tableName;
    this.dynamoDBClient = dynamoDBClient;
    this.properties = properties;
  }

  @Override
  public IRecordProcessor createProcessor() {
    ConnectionProvider provider = ConnectionProvider.builder("pii_service_conn_provider")
        .maxConnections(50)
        .maxIdleTime(Duration.ofSeconds(60))
        .maxLifeTime(Duration.ofSeconds(60))
        .pendingAcquireTimeout(Duration.ofSeconds(60))
        .evictInBackground(Duration.ofSeconds(30))
        .build();

    HttpClient httpClient = HttpClient.create(provider);

    ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

    WebClient webClient = WebClient.builder()
        .baseUrl(properties.serviceUrl)
        .clientConnector(connector)
        .build();

    return new StreamsRecordProcessor(dynamoDBClient, tableName,
        new PIIService(properties, webClient));
  }
}
