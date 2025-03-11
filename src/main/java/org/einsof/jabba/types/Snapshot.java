package org.einsof.jabba.types;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Snapshot {
  public boolean available;
  public String url;
  @JsonDeserialize(using = WaybackDateDeserializer.class)
  public Date timestamp;
  public String status;
}
