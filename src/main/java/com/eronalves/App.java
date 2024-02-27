package com.eronalves;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Hello world!
 *
 */
public class App {
  public static CompletableFuture<String> crawl(URL url) {
    return CompletableFuture.completedFuture("");
  }

  public static void main(String[] args) {
    System.out.println("Hello World!");
  }
}
