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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
    System.out.println("Received " + url);
    CompletableFuture<String> cf = new CompletableFuture<>();
    this.executor.execute(() -> {
      System.out.println("Fetching " + url);
      HttpURLConnection conn = null;
      try {
        conn = (HttpURLConnection) url.openConnection();
      } catch (IOException e) {
        System.out.println(e);
        cf.completeExceptionally(e);
        return;
      }
      conn.setConnectTimeout(10000);
      conn.setRequestProperty("Accept-Charset", "UTF-8");
      try {
        conn.setRequestMethod("GET");
      } catch (ProtocolException e) {
        cf.completeExceptionally(e);
        System.out.println(e);
        return;
      }
      try {
        conn.connect();
      } catch (IOException e) {
        cf.completeExceptionally(e);
        System.out.println(e);
        return;
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
        System.out.println(e);
        return;
      }
      cf.complete(sb.toString());
      conn.disconnect();
      System.out.println("Completed");
      return;
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

  public void shutdown() throws InterruptedException {
    this.executor.shutdown();
  }

  public CompletableFuture<String> fetchAll(List<String> links) {
    List<URL> urls = new ArrayList<>();

    for (String link : links) {
      System.out.println(link);
      try {
        urls.add(new URL(link));
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    System.out.println(urls);
    CompletableFuture[] array = urls.stream().map(this::crawl)
        .toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(array)
        .handle((t, r) -> {
          if (r != null) {

            System.out.println(r);
            return "";
          } else {
            return Arrays.stream(array)
                .map(CompletableFuture::join)
                .map(String.class::cast)
                .collect(Collectors.joining("\n"));
          }
        });
  }

  public static void main(String[] args)
      throws MalformedURLException, InterruptedException {
    App app = new App();
    CompletableFuture<String> crawl = app
        .crawl(new URL("https://g1.com.br"));
    CompletableFuture<List<String>> links = app.extractLinks(crawl);
    CompletableFuture<String> l = links
        .thenCompose(list -> app.fetchAll(list));
    app.extractLinks(l).thenAccept(s -> System.out.println(s.size()))
        .thenRun(() -> {
          try {
            app.shutdown();
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        });
  }

}
