/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

/**
 * A builder for {@link YamlSerializer} providing a simple method for
 * overriding the latter's defaults. Usage scenario:
 *
 * <pre>
 *  YamlSerializer xml =
 *      YamlSerializerBuilder.newBuilder()
 *          .indentSpaces(4)                // instead of 2
 *          .lineSeparator("\n")            // instead of the system's default one
 *          .inlineSingleRefObjects(false)  // do not inline singly-referenced objects
 *          .build();
 *  </pre>
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class YamlSerializerBuilder extends AbstractSerializerBuilder<YamlSerializer, YamlSerializerBuilder> {
  /**
   * Creates a new builder.
   */
  public static YamlSerializerBuilder newBuilder() {
    return new YamlSerializerBuilder();
  }

  @Override
  public YamlSerializer build() {
    YamlSerializer yaml = new YamlSerializer();
    yaml.indentSpaces = indentSpaces;
    yaml.lineSeparator = lineSeparator;
    yaml.encoding = encoding;
    yaml.inlineSingleRefObjects = inlineSingleRefObjects;
    return yaml;
  }

  private YamlSerializerBuilder() {
  }
}