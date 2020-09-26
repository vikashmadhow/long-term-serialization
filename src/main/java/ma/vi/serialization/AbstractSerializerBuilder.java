/*
 * Copyright (c) 2020 2017 Vikash Madhow
 */

package ma.vi.serialization;

import ma.vi.lang.Builder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract serializer builder provides control of some widely
 * applicable configuration options.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public abstract class AbstractSerializerBuilder<T extends Serializer, B extends Builder<T>> implements Builder<T> {
  /**
   * Changes the number of spaces for indentation: default 2.
   */
  public B indentSpaces(int indentSpaces) {
    checkArgument(indentSpaces >= 0, "indent spaces must be zero or more");
    this.indentSpaces = indentSpaces;
    return (B) this;
  }

  /**
   * Changes the line separator character sequence to use; defaults to '\n'.
   */
  public B lineSeparator(String lineSeparator) {
    checkNotNull(lineSeparator, "line separator cannot be null. Use empty string instead.");
    this.lineSeparator = lineSeparator;
    return (B) this;
  }

  /**
   * Changes the character encoding to use: default UTF-8.
   */
  public B encoding(Charset encoding) {
    checkNotNull(lineSeparator, "encoding cannot be null. Use a standard charset such as UTF-8 or " +
        "the default charset for the system instead.");
    this.encoding = encoding;
    return (B) this;
  }

  /**
   * Changes whether to inline single-reference objects or not. Defaults is to
   * inline as this produces more readable content without any impact on the size
   * of the text produced.
   */
  public B inlineSingleRefObjects(boolean inline) {
    this.inlineSingleRefObjects = inline;
    return (B) this;
  }

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