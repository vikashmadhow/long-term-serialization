/*
 * Copyright (c) 2018 Vikash Madhow
 */

package ma.vi.serialization;

import java.io.*;

/**
 * Serializers takes mapped value produced by {@link Mapper} and creates
 * string representations, and vice-versa.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public interface Serializer {
  /**
   * Construct the mapped value reading its string representation from the
   * provided reader.
   */
  Mapped toMap(Reader in) throws IOException;

  /**
   * Writes the string representation of the mapped value to the writer.
   */
  void toText(Mapped map, Writer out) throws IOException;

  /**
   * Convenience method to get the mapped value from a string.
   */
  default Mapped toMap(String text) {
    try {
      return toMap(new StringReader(text));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convenience method to get the string representation of the mapped value.
   */
  default String toText(Mapped map) {
    try {
      StringWriter out = new StringWriter();
      toText(map, out);
      return out.toString();
    } catch (IOException e) {
      // should not happen
      throw new RuntimeException(e);
    }
  }
}