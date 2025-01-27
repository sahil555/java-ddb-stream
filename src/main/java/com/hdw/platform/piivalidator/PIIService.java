package com.hdw.platform.piivalidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@SuppressWarnings("unchecked")
public class PIIService {

  private final TableValidatorProperties properties;

  private final WebClient webClient;

  private String jwtToken;

  public PIIService(
      @Autowired TableValidatorProperties properties,
      @Autowired WebClient webClient) {
    this.properties = properties;
    this.webClient = webClient;

    setJwtToken();
  }

  public PIIResponse runTheMagic(String action, PIIRequest request) throws Exception {

    String uuid = UUID.randomUUID().toString();

    try {

      log.debug("Calling PII service with action : {} ", action);
      PIIResponse response = PIIResponse.builder().build();

      HashMap<String, Object> nothing =
          (HashMap<String, Object>)
              this.webClient
                  .post()
                  .uri(action)
                  .bodyValue(request)
                  .header("Content-Type", "application/json")
                  .header("Authorization", this.jwtToken)
                  .header("x-amzn-trace-id", uuid)
                  .exchangeToMono(
                      clientResponse -> {
                        log.info(
                            "Received Status Code From PII Service : {} for requestId : {}",
                            clientResponse.statusCode(),
                            uuid);

                        response.setHttpStatusCode(clientResponse.statusCode().value());

                        if (clientResponse.statusCode().equals(HttpStatus.OK)) {
                          return clientResponse.bodyToMono(HashMap.class);
                        } else {
                          return clientResponse.createException().flatMap(Mono::error);
                        }
                      })
                  .block();

      response.setData(nothing);

      log.debug("response for action {} : {} requestId : {}", action, response, uuid);

      return response;
    } catch (WebClientResponseException error) {
      log.error(
          "PII request failed for action : {} response : {}",
          action,
          error.getResponseBodyAsString());

      // create the jwt token if expired and trying again :)
      if (error.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
          && Objects.requireNonNull(error.getResponseBodyAs(HashMap.class))
          .get("errorCode")
          .equals(4011)) {
        log.warn("JWT token expired getting new one and retying the request :)");
        this.setJwtToken();
        return this.runTheMagic(action, request);
      }

      throw new Exception(
          "WebClientResponseException API Request Failed for PII Service for action", error);
    } catch (WebClientRequestException error) {
      log.error(
          "WebClientRequestException exception while calling PII service uuid : {} error : {}",
          uuid, error.getMessage(), error);
      if (error.getMessage().equals("Connection prematurely closed BEFORE response")) {
        log.warn("Retrying failed PII request ...");
        return this.runTheMagic(action, request);
      }

      throw new Exception("Unknown WebClientRequestException Error While PII Service", error);
    }
  }


  public void setJwtToken() {
    try {
      WebClient client = WebClient.builder().baseUrl(this.properties.serviceUrl + "login").build();

      Map<String, String> request =
          Map.of(
              "clientId", this.properties.clientId,
              "clientSecret", this.properties.clientSecret);

      HashMap<String, String> response =
          (HashMap<String, String>)
              client
                  .post()
                  .bodyValue(request)
                  .exchangeToMono(
                      clientResponse -> {
                        if (clientResponse.statusCode().equals(HttpStatus.OK)) {
                          return clientResponse.bodyToMono(HashMap.class);
                        } else {
                          return clientResponse.createException().flatMap(Mono::error);
                        }
                      })
                  .block();

      assert response != null;
      this.jwtToken = response.getOrDefault("token", null);

    } catch (WebClientResponseException error) {
      log.error(
          "Token API Request Failed for PII Service for action {}",
          error.getResponseBodyAsString());

    } catch (Exception error) {
      log.error("Failed to get token", error);
      throw error;
    }
  }
}
