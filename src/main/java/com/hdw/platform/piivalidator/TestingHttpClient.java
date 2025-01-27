package com.hdw.platform.piivalidator;


import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@SpringBootApplication
public class TestingHttpClient {

  public static void main(String[] args) throws IOException, InterruptedException {

    MyTourneyWrapper myTourneyWrapper = new MyTourneyWrapper();
    myTourneyWrapper.makeDummyCall("https://tourney.qapfgames.com/credit", "ayush.ravi");

    log.info("sleeping ...");
    Thread.sleep(60000);
    log.info("Woke up !!");

    myTourneyWrapper.makeDummyCall("https://tourney.qapfgames.com/credit", "ayush.ravi");
    myTourneyWrapper.makeDummyCall("https://tourney.qapfgames.com/credit", "ayush.ravi");


  }

}
