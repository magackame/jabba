package org.einsof.jabba;

import java.util.Optional;

import org.einsof.jabba.entities.DomainState;
import org.einsof.jabba.services.ScrapeService;
import org.junit.Assert;
import org.junit.Test;

public class AppTest {
  @Test
  public void extractPriceEmpty() {
    final var price = ScrapeService.extractPrice("");
    Assert.assertEquals(Optional.empty(), price);
  }

  @Test
  public void extractPriceRegistered() {
    final var price = ScrapeService.extractPrice("\nregistered");
    Assert.assertEquals(Optional.empty(), price);
  }

  @Test
  public void extractPriceError() {
    final var price = ScrapeService.extractPrice("\nerror");
    Assert.assertEquals(Optional.empty(), price);
  }

  @Test
  public void extractPrice() {
    final var price = ScrapeService.extractPrice("$20.06 $3.07 / year\nrenews at $20.06");
    Assert.assertEquals(Optional.of("3.07"), price);
  }

  @Test
  public void extractPriceFirstYearSale() {
    final var price = ScrapeService.extractPrice("1st Year Sale!\n$20.06 $3.07 / year\nrenews at $20.06");
    Assert.assertEquals(Optional.of("3.07"), price);
  }

  @Test
  public void extractPriceRenewalSale() {
    final var renewalPrice = ScrapeService.extractPrice("Sale!\n$46.65 $31.20 / year\nrenews at $46.65 SALE! $31.20");
    Assert.assertEquals(Optional.of("31.20"), renewalPrice);
  }

  @Test
  public void extractRenewalPriceEmpty() {
    final var renewalPrice = ScrapeService.extractRenewalPrice("");
    Assert.assertEquals(Optional.empty(), renewalPrice);
  }

  @Test
  public void extractRenewalPriceRegistered() {
    final var renewalPrice = ScrapeService.extractRenewalPrice("\nregistered");
    Assert.assertEquals(Optional.empty(), renewalPrice);
  }

  @Test
  public void extractRenewalPriceError() {
    final var renewalPrice = ScrapeService.extractRenewalPrice("\nerror");
    Assert.assertEquals(Optional.empty(), renewalPrice);
  }

  @Test
  public void extractRenewalPrice() {
    final var renewalPrice = ScrapeService.extractRenewalPrice("$3.07 / year\nrenews at $20.06");
    Assert.assertEquals(Optional.of("20.06"), renewalPrice);
  }

  @Test
  public void extractRenewalPriceFirstYearSale() {
    final var renewalPrice = ScrapeService.extractRenewalPrice("1st Year Sale!\n$20.06 $3.07 / year\nrenews at $20.06");
    Assert.assertEquals(Optional.of("20.06"), renewalPrice);
  }

  @Test
  public void extractRenewalPriceRenewalSale() {
    final var renewalPrice = ScrapeService
        .extractRenewalPrice("Sale!\n$46.65 $31.20 / year\nrenews at $46.65 SALE! $31.20");
    Assert.assertEquals(Optional.of("46.65"), renewalPrice);
  }

  @Test
  public void extractDomainStateEmpty() {
    final var state = ScrapeService.extractState("");
    Assert.assertEquals(DomainState.Available, state);
  }

  @Test
  public void extractDomainStateRegistered() {
    final var state = ScrapeService.extractState("\nregistered");
    Assert.assertEquals(DomainState.Registered, state);
  }

  @Test
  public void extractDomainStateError() {
    final var state = ScrapeService.extractState("\nerror");
    Assert.assertEquals(DomainState.Error, state);
  }

  @Test
  public void extractDomainState() {
    final var state = ScrapeService.extractState("$3.07 / year\nrenews at $20.06");
    Assert.assertEquals(DomainState.Available, state);
  }

  @Test
  public void extractDomainFirstYearSale() {
    final var state = ScrapeService.extractState("1st Year Sale!\n$20.06 $3.07 / year\nrenews at $20.06");
    Assert.assertEquals(DomainState.Available, state);
  }

  @Test
  public void extractDomainSale() {
    final var state = ScrapeService.extractState("Sale!\n$46.65 $31.20 / year\nrenews at $46.65 SALE! $31.20");
    Assert.assertEquals(DomainState.Available, state);
  }

  @Test
  public void extractTld() {
    final var tld = ScrapeService.extractTld("einsof.org");
    Assert.assertEquals("org", tld);
  }

  @Test
  public void extractTldMulti() {
    final var tld = ScrapeService.extractTld("einsof.co.uk");
    Assert.assertEquals("co.uk", tld);
  }
}
