package org.einsof.jabba.controllers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Optional;

import org.einsof.jabba.services.ScrapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import com.google.common.net.HttpHeaders;

@RestController
@RequestMapping("/api")
public class Api {
  @Autowired
  public ScrapeService scrapeService;

  // TODO: fix reflection
  @PostMapping("/scrape")
  public RedirectView scrape(@RequestParam("query") String query,
      @RequestParam("tlds[]") Optional<HashSet<String>> tlds) {
    final var t = new HashSet<String>();
    final var first10 = this.scrapeService.fetchFirst10Tlds();
    for (final var tld : first10) {
      t.add(tld.getTld());
    }

    final var scrape = scrapeService.spawnScrape(query, tlds.orElse(t));

    return new RedirectView("/scrape/" + scrape.getId());
  }

  @GetMapping("/scrape/{id}")
  public ResponseEntity<ByteArrayResource> scrape(@PathVariable("id") Long id) throws IOException {
    final var scrape = this.scrapeService.fetchScrapeById(id);
    if (scrape.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    final var s = scrape.get();

    final byte[] excel = ScrapeService.excel(s.getQuery(), s.getDomains());
    final var resource = new ByteArrayResource(excel);

    final var dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    final var filename = s.getQuery() + "-" + dateFormat.format(s.getStarted()) + ".xlsx";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .contentLength(excel.length)
        .body(resource);
  }
}
