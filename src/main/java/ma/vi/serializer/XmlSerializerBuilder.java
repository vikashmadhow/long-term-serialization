/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import ma.vi.lang.Names;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for {@link XmlSerializer} providing a simple method for
 * overriding the latter's defaults. Usage scenario:
 *
 * <pre>
 *  XmlSerializer xml =
 *      XmlSerializerBuilder.newBuilder()
 *          .indentSpaces(4)                // instead of 2
 *          .lineSeparator("\n")            // instead of the system's default one
 *          .rootElement("objects")         // instead of "root"
 *          .inlineSingleRefObjects(false)  // do not inline singly-referenced objects
 *          .build();
 *  </pre>
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class XmlSerializerBuilder extends AbstractSerializerBuilder<XmlSerializer, XmlSerializerBuilder> {
  /**
   * Creates a new builder.
   */
  public static XmlSerializerBuilder newBuilder() {
    return new XmlSerializerBuilder();
  }

  /**
   * Changes the root element to use (default = 'root').
   */
  public XmlSerializerBuilder rootElement(String rootElement) {
    checkNotNull(rootElement, "root element cannot be null.");
    checkArgument(Names.isValidXmlName(rootElement), "root element must be a valid XML element name.");
    this.rootElement = rootElement;
    return this;
  }

  @Override
  public XmlSerializer build() {
    XmlSerializer xml = new XmlSerializer();
    xml.indentSpaces = indentSpaces;
    xml.lineSeparator = lineSeparator;
    xml.encoding = encoding;
    xml.inlineSingleRefObjects = inlineSingleRefObjects;
    xml.rootElement = rootElement;
    return xml;
  }

  private XmlSerializerBuilder() {
  }

  /**
   * An xml element name can contain a (unicode) letter, digits, hyphens, underscores and periods.
   * This pattern matches any character outside these classes and can thus be used to find invalid
   * element names.
   */
  private static final Pattern invalidXmlElementNameChar = Pattern.compile("[^\\p{L}\\d\\-_.]");

  /**
   * The root element to use: defaults to 'root'.
   */
  private String rootElement = "root";
}