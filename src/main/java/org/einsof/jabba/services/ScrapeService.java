package org.einsof.jabba.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.einsof.jabba.entities.Domain;
import org.einsof.jabba.entities.DomainState;
import org.einsof.jabba.entities.Scrape;
import org.einsof.jabba.entities.ScrapeStatus;
import org.einsof.jabba.entities.Tld;
import org.einsof.jabba.repositories.ScrapeRepository;
import org.einsof.jabba.repositories.TldRepository;
import org.einsof.jabba.types.WaybackResponse;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Iterables;

@Service
public class ScrapeService {
  @Autowired
  private TldRepository tldRepository;
  @Autowired
  private ScrapeRepository scrapeRepository;
  @Autowired
  private TaskExecutor taskExecutor;

  static ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
  static HttpClient httpClient = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();
  static Semaphore rateLimiter = new Semaphore(10);

  static Pattern renewalPricePattern = Pattern.compile("renews at \\$([0-9]+\\.[0-9]+)", Pattern.CASE_INSENSITIVE);
  static Pattern pricePattern = Pattern.compile("\\$([0-9]+\\.[0-9]+) / year", Pattern.CASE_INSENSITIVE);

  public Iterable<Tld> fetchAllTlds() {
    return this.tldRepository.findAll();
  }

  public Iterable<Tld> fetchFirst10Tlds() {
    return Iterables.limit(this.tldRepository.findAll(), 10);
  }

  public Iterable<Scrape> fetchAllScrapes() {
    return this.scrapeRepository.findAllByOrderByStartedDesc();
  }

  public Optional<Scrape> fetchScrapeById(Long id) {
    return this.scrapeRepository.findById(id);
  }

  public Scrape doScrape(String query, HashSet<String> tlds) {
    final var scrape = new Scrape(query, ScrapeStatus.InProgress, new Date());
    this.scrapeRepository.save(scrape);

    // TODO: handle exc
    // TODO: spawn multiple
    taskExecutor.execute(() -> {
      final var options = new ChromeOptions();

      final var googleChromeBinary = System.getenv("GOOGLE_CHROME_BINARY");
      if (!googleChromeBinary.isBlank()) {
        options
            .setBinary(googleChromeBinary);
        System.err.println("using Google Chrome binary from env: " + googleChromeBinary);
      }

      options.addArguments("--remote-allow-origins=*");
      // TODO: headless detection
      options.addArguments("--headless");
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--disable-blink-features=AutomationControlled");
      options.addArguments(
          "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

      final var driver = new ChromeDriver(options);

      List<Domain> records;
      try {
        // TODO: prase sidebar
        // TODO: extract SALE
        records = scrape(driver, query, tlds);
      } catch (Exception e) {
        System.err.println("failed to scrape: " + e);
        scrape.setStatus(ScrapeStatus.Failed);
        this.scrapeRepository.save(scrape);
        return;
      }

      final var futures = records
          .stream()
          .map(record -> fetchLatestHistoryAsync(record.getDomain()))
          .collect(Collectors.toList());
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      final var latestHistories = futures
          .stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      for (var i = 0; i < records.size(); ++i) {
        records.get(i).setLatestHistory(latestHistories.get(i));
      }

      // TODO: reuse driver?
      driver.quit();

      scrape.setStatus(ScrapeStatus.Finished);
      scrape.setFinished(Optional.of(new Date()));
      scrape.setDomains(records);
      this.scrapeRepository.save(scrape);
    });

    return scrape;
  }

  public static List<Domain> scrape(WebDriver driver, String query, HashSet<String> tlds) {
    driver.get("https://porkbun.com");

    final var input = driver.findElement(By.id("domainSearchInput"));
    input.sendKeys(query);

    final var searchButton = driver.findElement(By.id("domainSearchButton"));
    searchButton.click();

    final var showAllExtensionsButton = driver.findElement(By.xpath("//*[contains(text(), 'Show All Extensions')]"));
    showAllExtensionsButton.click();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
    }

    final var records = new ArrayList<Domain>();
    final var rows = driver
        .findElements(By.cssSelector(".searchResultsSectionContainer .searchResultRow"));
    for (final var element : rows) {
      WebElement domainElement;
      try {
        domainElement = element.findElement(By.className("searchResultRowDomain"));
      } catch (NoSuchElementException e) {
        try {
          domainElement = element.findElement(By.className("searchResultRowDomainAftermarket"));
        } catch (java.util.NoSuchElementException ee) {
          System.err.println(element.getTagName() + ": " + element.getText());
          System.err.println("no such element domain");
          continue;
        }
      }

      final var domain = domainElement.getText();
      if (domain.isBlank()) {
        continue;
      }

      final var tld = extractTld(domain);
      if (!tlds.contains(tld)) {
        continue;
      }
      tlds.remove(tld);

      WebElement priceElement;
      try {
        priceElement = element.findElement(By.className("searchResultRowPrice"));
      } catch (NoSuchElementException e) {
        System.err.println("no such element price");
        continue;
      }

      Optional<String> price = Optional.empty();
      Optional<String> renewalPrice = Optional.empty();

      final var state = extractState(priceElement.getText());
      if (state == DomainState.Available) {
        price = extractPrice(priceElement.getText());
        renewalPrice = extractRenewalPrice(priceElement.getText());
      }

      Optional<Date> latestHistory = Optional.empty();

      final var record = new Domain(domain, state, price, renewalPrice, latestHistory);
      records.add(record);

      if (tlds.isEmpty()) {
        break;
      }
    }

    return records;
  }

  public static byte[] excel(String sheetName, List<Domain> records) throws IOException {
    final var workbook = new XSSFWorkbook();
    final var sheet = workbook.createSheet(sheetName);

    final var rowOffset = 1;
    final var cellOffset = 1;

    final var header = sheet.createRow(0 + rowOffset);

    final var d = header.createCell(0 + cellOffset);
    d.setCellValue("domain");
    d.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    d.getCellStyle().setBorderLeft(BorderStyle.THIN);
    d.getCellStyle().setBorderTop(BorderStyle.THIN);
    d.getCellStyle().setBorderBottom(BorderStyle.THIN);
    d.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var s = header.createCell(1 + cellOffset);
    s.setCellValue("status");
    s.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    s.getCellStyle().setBorderLeft(BorderStyle.THIN);
    s.getCellStyle().setBorderTop(BorderStyle.THIN);
    s.getCellStyle().setBorderBottom(BorderStyle.THIN);
    s.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var p = header.createCell(2 + cellOffset);
    p.setCellValue("price, USD");
    p.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    p.getCellStyle().setBorderLeft(BorderStyle.THIN);
    p.getCellStyle().setBorderTop(BorderStyle.THIN);
    p.getCellStyle().setBorderBottom(BorderStyle.THIN);
    p.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var rp = header.createCell(3 + cellOffset);
    rp.setCellValue("renewal price, USD");
    rp.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    rp.getCellStyle().setBorderLeft(BorderStyle.THIN);
    rp.getCellStyle().setBorderTop(BorderStyle.THIN);
    rp.getCellStyle().setBorderBottom(BorderStyle.THIN);
    rp.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var lh = header.createCell(4 + cellOffset);
    lh.setCellValue("latest history");
    lh.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    lh.getCellStyle().setBorderLeft(BorderStyle.THIN);
    lh.getCellStyle().setBorderTop(BorderStyle.THIN);
    lh.getCellStyle().setBorderBottom(BorderStyle.THIN);
    lh.getCellStyle().setBorderRight(BorderStyle.THIN);

    for (var i = 0; i < records.size(); ++i) {
      final var record = records.get(i);
      final var row = sheet.createRow(i + 1 + rowOffset);

      final var domain = row.createCell(0 + cellOffset);
      domain.setCellValue(record.getDomain());
      domain.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
      domain.getCellStyle().setBorderLeft(BorderStyle.THIN);
      domain.getCellStyle().setBorderTop(BorderStyle.THIN);
      domain.getCellStyle().setBorderBottom(BorderStyle.THIN);
      domain.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var state = row.createCell(1 + cellOffset);
      state.setCellValue(record.getState().toString());
      state.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      state.getCellStyle().setBorderLeft(BorderStyle.THIN);
      state.getCellStyle().setBorderTop(BorderStyle.THIN);
      state.getCellStyle().setBorderBottom(BorderStyle.THIN);
      state.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var price = row.createCell(2 + cellOffset);
      price.setCellValue(record.getPrice().orElse("-"));
      price.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      price.getCellStyle().setBorderLeft(BorderStyle.THIN);
      price.getCellStyle().setBorderTop(BorderStyle.THIN);
      price.getCellStyle().setBorderBottom(BorderStyle.THIN);
      price.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var renewalPrice = row.createCell(3 + cellOffset);
      renewalPrice.setCellValue(record.getRenewalPrice().orElse("-"));
      renewalPrice.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      renewalPrice.getCellStyle().setBorderLeft(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderTop(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderBottom(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      final var latestHistory = row.createCell(4 + cellOffset);
      latestHistory.setCellValue(record.getLatestHistory().map(date -> dateFormat.format(date)).orElse("-"));
      latestHistory.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      latestHistory.getCellStyle().setBorderLeft(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderTop(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderBottom(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderRight(BorderStyle.THIN);
    }

    final var stream = new ByteArrayOutputStream();
    workbook.write(stream);
    workbook.close();

    return stream.toByteArray();
  }

  public static CompletableFuture<WaybackResponse> fetchHistoryAsync(String url, boolean skipLock) {
    if (!skipLock) {
      try {
        rateLimiter.acquire();
      } catch (InterruptedException e) {
        System.err.println("failed to aquire the lock: " + e);
      }
    }

    final var request = HttpRequest
        .newBuilder()
        .timeout(Duration.ofSeconds(15))
        .uri(URI.create("https://archive.org/wayback/available?url=" + url))
        .GET()
        .build();

    return httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenCompose(response -> {
          // System.out.println("status: " + response.statusCode());

          if (response.statusCode() == 429) {
            return CompletableFuture
                .supplyAsync(() -> {
                  try {
                    return fetchHistoryAsync(url, true).get();
                  } catch (Exception e) {
                    throw new CompletionException(e);
                  }
                },
                    CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS));
          } else {
            return CompletableFuture.supplyAsync(() -> response)
                .thenApply(HttpResponse::body)
                .thenApply(json -> {
                  try {
                    return objectMapper.readValue(json, WaybackResponse.class);
                  } catch (Exception e) {
                    throw new CompletionException(e);
                  }
                })
                .thenApply(id -> {
                  rateLimiter.release();
                  return id;
                });
          }
        })
        .exceptionally(e -> {
          rateLimiter.release();
          throw new CompletionException(e);
        });
  }

  public static CompletableFuture<Optional<Date>> fetchLatestHistoryAsync(String domain) {
    return fetchHistoryAsync("https://" + domain, false)
        .thenCompose(httpsResponse -> {
          final var httpsSnapshot = httpsResponse.archivedSnapshots.closest;

          if (httpsSnapshot.isPresent()) {
            final var latestHistory = httpsSnapshot.map(snapshot -> snapshot.timestamp);

            return CompletableFuture.supplyAsync(() -> latestHistory);
          } else {
            return fetchHistoryAsync("http://" + domain, false)
                .thenApply(httpResponse -> {
                  final var httpSnapshot = httpResponse.archivedSnapshots.closest;
                  final var latestHistory = httpSnapshot.map(snapshot -> snapshot.timestamp);

                  return latestHistory;
                });
          }
        })
        .exceptionally(e -> {
          System.err.println("failed to fetch latest history: " + e);
          return Optional.empty();
        });

  }

  public static DomainState extractState(String text) {
    if (text.contains("error")) {
      return DomainState.Error;
    } else if (text.contains("registered")) {
      return DomainState.Registered;
    } else {
      return DomainState.Available;
    }
  }

  public static Optional<String> extractPrice(String text) {
    final var priceMatcher = pricePattern.matcher(text);

    if (priceMatcher.find()) {
      return Optional.of(priceMatcher.group(1));
    } else {
      return Optional.empty();
    }
  }

  public static Optional<String> extractRenewalPrice(String text) {
    final var renewalPriceMatcher = renewalPricePattern.matcher(text);
    if (renewalPriceMatcher.find()) {
      return Optional.of(renewalPriceMatcher.group(1));
    } else {
      return Optional.empty();
    }
  }

  public static String extractTld(String domain) {
    final var index = domain.indexOf(".");

    return domain.substring(index + 1);
  }
}
