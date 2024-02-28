package com.eronalves;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.processing.Completions;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

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
      System.out.println("Open connection");
      HttpURLConnection conn = null;
      try {
        conn = (HttpURLConnection) url.openConnection();
      } catch (IOException e) {
        cf.completeExceptionally(e);
        return;
      }
      conn.setConnectTimeout(10000);
      conn.setRequestProperty("Accept-Charset", "UTF-8");
      try {
        conn.setRequestMethod("GET");
      } catch (ProtocolException e) {
        cf.completeExceptionally(e);
        return;
      }
      try {
        System.out.println("Connect");
        conn.connect();
      } catch (IOException e) {
        cf.completeExceptionally(e);
        return;
      }
      try {
        System.out.println(((HttpURLConnection) conn).getResponseCode());
      } catch (IOException e) {
        cf.completeExceptionally(e);
        return;
      }
      StringBuilder sb = new StringBuilder();
      System.out.println("Connected");
      try (InputStream response = conn.getInputStream();
          InputStreamReader isr = new InputStreamReader(response,
              StandardCharsets.UTF_8);
          BufferedReader br = new BufferedReader(isr)) {
        System.out.println("Extracting body");
        while (true) {
          String readLine = br.readLine();
          System.out.println(readLine);
          if (readLine == null) break;
          sb.append(readLine + "\n");
        }
      } catch (IOException e) {
        cf.completeExceptionally(e);
        return;
      }
      cf.complete(sb.toString());
    });

    return cf;
  }

  public CompletableFuture<List<String>> extractLinks(
      CompletableFuture<String> pageContent) {
    return pageContent
        .thenApply(Jsoup::parse)
        .thenApply(d -> d.select("a[href]"))
        .thenApply(Elements::spliterator)
        .thenApply(s -> StreamSupport.stream(s, false))
        .thenApply(s -> s.map(e -> e.attr("abs:href"))
            .collect(Collectors.toList()));
  }

  public void shutdown() {
    this.executor.shutdown();
  }

  public CompletableFuture<String> fetchAll(List<String> links) {
    CompletableFuture[] array = links.stream().map(t -> {
      try {
        return new URL(t);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e.getCause());
      }
    })
        .map(this::crawl)
        .toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(array)
        .thenApply(v -> Arrays.stream(array)
            .map(CompletableFuture::join)
            .map(String.class::cast)
            .collect(Collectors.joining(",")));
  }

  public static void main(String[] args) throws MalformedURLException {
    App app = new App();
    CompletableFuture<String> crawl = app
        .crawl(new URL("https://www.g1.com.br"));
    CompletableFuture<List<String>> links = app.extractLinks(crawl);
    CompletableFuture<String> content = links
        .thenCompose(list -> app.fetchAll(list));
    app.shutdown();
  }

}
