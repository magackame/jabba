package org.einsof.jabba.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.einsof.jabba.services.ScrapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class Web {
  @Autowired
  private ScrapeService scrapeService;

  @GetMapping("/")
  public String index(Model model) {
    final var tlds = this.scrapeService.fetchAllTlds();
    model.addAttribute("tlds", tlds);

    return "index";
  }

  @GetMapping("/scrape")
  public String scrape(Model model) {
    final var scrapes = this.scrapeService.fetchAllScrapes();
    model.addAttribute("scrapes", scrapes);

    return "scrape/index";
  }

  @GetMapping("/scrape/{id}")
  public String scrape(@PathVariable("id") Long id, Model model) {
    final var scrape = this.scrapeService.fetchScrapeById(id);
    if (scrape.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    model.addAttribute("scrape", scrape.get());

    return "scrape/id";
  }
}
