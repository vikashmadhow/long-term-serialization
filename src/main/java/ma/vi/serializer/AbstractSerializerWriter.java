/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.repeat;
import static java.util.Map.Entry;

/**
 * Partial implementation the writing part of a Serializer as a set of
 * template methods whose implementation in subclasses direct how the objects
 * are written to an output stream.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public abstract class AbstractSerializerWriter implements Serializer {
  @Override
  public void toText(Mapped map, Writer out) throws IOException {
    writeHeader(map, out);
    String indent = repeat(" ", indentSpaces);
    Set<String> written = new HashSet<>();
    boolean first = true;
    for (Entry<String, Map<String, String>> entry : map.objects.entrySet()) {
      String name = entry.getKey();
      if (!written.contains(name)) {
        Map<String, String> object = entry.getValue();
        String type = map.types.get(name);

        writeObjectStart(map, out, name, type, indent, object, first);
        writeObject(map, out, name, type, indent, object, first, written, 2);
        writeObjectEnd(map, out, name, type, indent, object, first);

        if (first) {
          first = false;
        }
      }
    }
    writeFooter(map, out);
  }

  protected abstract void writeHeader(Mapped map, Writer out) throws IOException;

  protected abstract void writeFooter(Mapped map, Writer out) throws IOException;

  protected abstract void writeObjectStart(Mapped map, Writer out,
                                           String name, String type,
                                           String indent, Map<String, String> object,
                                           boolean first) throws IOException;

  protected abstract void writeObjectEnd(Mapped map, Writer out,
                                         String name, String type,
                                         String indent, Map<String, String> object,
                                         boolean first) throws IOException;

  protected abstract void writeObject(Mapped map, Writer out, String name, String type,
                                      String indent, Map<String, String> object,
                                      boolean first, Set<String> written, int level) throws IOException;

  /**
   * The number of spaces for indentation: default 2.
   */
  int indentSpaces = 2;

  /**
   * The line separator character sequence to use; defaults to '\n'.
   */
  String lineSeparator = "\n"; // System.lineSeparator();

  /**
   * Character encoding to use: default UTF-8.
   */
  Charset encoding = StandardCharsets.UTF_8;

  /**
   * Whether to inline single-reference objects. Defaults to true as this produces
   * more readable content without any impact on the size of the text produced.
   */
  boolean inlineSingleRefObjects = true;
}