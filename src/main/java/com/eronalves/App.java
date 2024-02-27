package com.eronalves;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Hello world!
 *
 */
public class App {

  ExecutorService executor;

  public App() {
    this.executor = Executors.newCachedThreadPool(new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
      }

    });
  }

  public CompletableFuture<String> crawl(URL url) {
    return CompletableFuture.supplyAsync(() -> {
      URLConnection conn = null;
      try {
        conn = url.openConnection();
      } catch (IOException e) {
        return "";
      }
      conn.setRequestProperty("Accept-Charset", "UTF-8");
      StringBuilder sb = new StringBuilder();
      try (InputStream response = conn.getInputStream();
          InputStreamReader isr = new InputStreamReader(response,
              StandardCharsets.UTF_8);
          BufferedReader br = new BufferedReader(isr)) {
        while (true) {
          String readLine = br.readLine();
          if (readLine == null) break;
          sb.append(readLine);
        }
      } catch (IOException e) {
      }
      return sb.toString();
    }, this.executor);
  }

  public static void main(String[] args) {
    System.out.println("Hello World!");
  }
}
