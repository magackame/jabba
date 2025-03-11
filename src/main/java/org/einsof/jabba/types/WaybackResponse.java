package org.einsof.jabba.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaybackResponse {
  public String url;
  @JsonProperty("archived_snapshots")
  public ArchivedSnapshots archivedSnapshots;
}
