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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.StreamsWorkerFactory;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@SpringBootApplication
public class PIIValidatorApplication implements CommandLineRunner {

  private static Worker worker;
  private static KinesisClientLibConfiguration workerConfig;
  private static IRecordProcessorFactory recordProcessorFactory;

  private static AmazonDynamoDB dynamoDBClient;
  private static AmazonCloudWatch cloudWatchClient;
  private static AmazonDynamoDBStreams dynamoDBStreamsClient;
  private static AmazonDynamoDBStreamsAdapterClient adapterClient;

  private static Regions awsRegion = Regions.AP_SOUTH_1;

  private static AWSCredentialsProvider awsCredentialsProvider =
      DefaultAWSCredentialsProviderChain.getInstance();

  public static void main(String[] args) {
    log.info("STARTING THE APPLICATION");
    SpringApplication.run(PIIValidatorApplication.class, args);
    log.info("APPLICATION FINISHED");
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Starting Demo ... with args : {}", Arrays.toString(args));
    TableValidatorProperties properties = this.getTableValidatorProperties();
    log.info("System Properties from ENV : {}", properties);
    dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).build();

    cloudWatchClient = AmazonCloudWatchClientBuilder.standard().withRegion(awsRegion).build();

    dynamoDBStreamsClient =
        AmazonDynamoDBStreamsClientBuilder.standard().withRegion(awsRegion).build();

    adapterClient = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient);

    recordProcessorFactory =
        new StreamsRecordProcessorFactory(dynamoDBClient, properties.tableName, properties);

    String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
    workerConfig =
        new KinesisClientLibConfiguration(
            properties.applicationName, properties.streamArn, awsCredentialsProvider, workerId)
            .withMaxRecords(1000)
            .withIdleTimeBetweenReadsInMillis(500)
            .withInitialLeaseTableWriteCapacity(15)
            .withInitialPositionInStream(properties.positionToStartReadingInStream);

    log.info("Creating worker for stream : {}", properties.streamArn);
    worker =
        StreamsWorkerFactory.createDynamoDbStreamsWorker(
            recordProcessorFactory, workerConfig, adapterClient, dynamoDBClient, cloudWatchClient);

    log.info("Starting worker with id {}...", workerId);

    worker.run();

    log.info("Done.");
  }

  @Bean
  public TableValidatorProperties getTableValidatorProperties() {
    Map<String, String> env = System.getenv();
    return TableValidatorProperties.builder()
        .serviceUrl(env.getOrDefault("SERVICE_URL", "https://pii.qapfgames.com/"))
        .tableName(env.getOrDefault("TABLE_NAME", "users"))
        .streamArn(env.getOrDefault("STREAM_ARN", "unknown"))
        .applicationName(env.getOrDefault("APPLICATION_NAME", "pii.validator"))
        .stage(env.getOrDefault("ENV", "local"))
        .clientId(env.getOrDefault("CLIENT_ID", "76e2b7e9ca18314b1063ebb8308598ad"))
        .clientSecret(
            env.getOrDefault(
                "CLIENT_SECRET",
                "434c06cb41f32f27e6b8d8e572e2cb7e007053e088325e26964fa7222b136a94"))
        .positionToStartReadingInStream(getPositionInStreamToReadFrom())
        .build();
  }

  private InitialPositionInStream getPositionInStreamToReadFrom() {
    final String position = System.getenv()
        .getOrDefault("STREAM_POSITION", "LATEST");

    if (Objects.equals(position, "TRIM_HORIZON")) {
      return InitialPositionInStream.TRIM_HORIZON;
    } else {
      return InitialPositionInStream.LATEST;
    }
  }

  @Bean
  public WebClient getWebClient() {
    return WebClient.create(this.getTableValidatorProperties().serviceUrl);
  }
}
