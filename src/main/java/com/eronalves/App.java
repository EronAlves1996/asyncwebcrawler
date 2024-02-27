package com.eronalves;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.omg.CORBA.CompletionStatus;

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
    CompletableFuture<String> cf = new CompletableFuture<>();
    this.executor.execute(() -> {
      URLConnection conn = null;
      try {
        conn = url.openConnection();
      } catch (IOException e) {
        cf.completeExceptionally(e);
      }
      conn.setConnectTimeout(10);
      conn.setRequestProperty("Accept-Charset", "UTF-8");
      try {
        conn.connect();
      } catch (IOException e) {
        cf.completeExceptionally(e);
      }
      StringBuilder sb = new StringBuilder();
      try (InputStream response = conn.getInputStream();
          InputStreamReader isr = new InputStreamReader(response,
              StandardCharsets.UTF_8);
          BufferedReader br = new BufferedReader(isr)) {
        while (true) {
          String readLine = br.readLine();
          if (readLine == null) break;
          sb.append(readLine + "\n");
        }
      } catch (IOException e) {
        cf.completeExceptionally(e);
      }
      cf.complete(sb.toString());
    });

    return cf;
  }

  public static void main(String[] args) throws MalformedURLException {
    App app = new App();
    CompletableFuture<String> crawl = app.crawl(new URL("https://g1.com.br"));
  }

}
