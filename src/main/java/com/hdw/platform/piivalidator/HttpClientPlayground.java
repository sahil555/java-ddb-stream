package com.hdw.platform.piivalidator;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import reactor.netty.http.client.HttpClient;

public class HttpClientPlayground {

  public static void main(String[] args) {
    HttpClient httpClient = HttpClient.create();

//    List<String> response = httpClient.baseUrl(
//            "https://pii.qapfgames.com/encrypt")
//        .post()
//        .responseContent()
//        .asString()
//        .collectList()
//        .block();

    String key = "abc";

    Algorithm algorithm = Algorithm.HMAC512(key);

    String token = JWT.create()
        .withClaim("name", "hello")
        .sign(algorithm);

    System.out.println(token);
  }

}
