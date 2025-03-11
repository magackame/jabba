package org.einsof.jabba.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WaybackDateDeserializer extends JsonDeserializer<Date> {
  private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

  @Override
  public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    String date = jsonParser.getText().substring(0, 8);

    try {
      return format.parse(date);
    } catch (ParseException e) {
      throw new IOException("failed to parse date: " + date, e);
    }
  }
}
