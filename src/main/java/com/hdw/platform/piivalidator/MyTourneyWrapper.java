package com.hdw.platform.piivalidator;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

@Slf4j
public class MyTourneyWrapper {

  private HttpClient httpClient;

  public MyTourneyWrapper() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(10);
    connectionManager.setValidateAfterInactivity(600000);
//    connectionManager.closeExpiredConnections();
//    connectionManager.closeIdleConnections(0, TimeUnit.SECONDS);

    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000)
        .setSocketTimeout(10 * 1000).setConnectionRequestTimeout(10 * 1000).build();

    HttpClient httpClient = HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .build();
    this.httpClient = httpClient;
  }

  public void makeDummyCall(String url, String traceId) {
    try {
      HttpPost httpPost = new HttpPost(url);

      httpPost.setEntity(new StringEntity(
          "{\"appId\":135,\"user_id\":\"08piczeyp1f3r0n\",\"tourneyId\":\"jWTh0\",\"referenceId\":\"12rsgfc53fgv\",\"rank\":1,\"winAmt\":10,\"channel\":\"IPAPS\",\"entryType\":14,\"parentTournament\":{\"parentTournamentId\":\"154523hsv\",\"parentTournamentName\":\"gdns\",\"tourneyStartTime\":5274,\"parentReferenceNumber\":\"ref432\"},\"transactionId\":\"132543\"}"));

      httpPost.addHeader("content-type", "application/json");
      httpPost.addHeader("x-amzn-trace-id", traceId);

      HttpResponse httpResponse = httpClient.execute(httpPost);

      log.info("response : {} {}", httpResponse.getStatusLine(),
          EntityUtils.toString(httpResponse.getEntity()));
    } catch (Exception error) {
      log.error("Error while making dummy call", error);
    }

  }
}
