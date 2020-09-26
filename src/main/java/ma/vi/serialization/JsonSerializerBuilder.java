/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serialization;

/**
 * A builder for {@link JsonSerializer} providing a simple method for
 * overriding the latter's defaults. Usage scenario:
 *
 * <pre>
 *  JsonSerializer xml =
 *      JsonSerializerBuilder.newBuilder()
 *          .indentSpaces(4)                // instead of 2
 *          .lineSeparator("\n")            // instead of the system's default one
 *          .inlineSingleRefObjects(false)  // do not inline singly-referenced objects
 *          .build();
 *  </pre>
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class JsonSerializerBuilder extends AbstractSerializerBuilder<JsonSerializer, JsonSerializerBuilder> {
  /**
   * Creates a new builder.
   */
  public static JsonSerializerBuilder newBuilder() {
    return new JsonSerializerBuilder();
  }

  @Override
  public JsonSerializer build() {
    JsonSerializer json = new JsonSerializer();
    json.indentSpaces = indentSpaces;
    json.lineSeparator = lineSeparator;
    json.encoding = encoding;
    json.inlineSingleRefObjects = inlineSingleRefObjects;
    return json;
  }
0
  private JsonSerializerBuilder() {
  }
}