/*
 * Copyright (c) 2020 2017 Vikash Madhow
 */

package ma.vi.serialization;

import ma.vi.xml.Fragment;
import ma.vi.xml.XmlReader;
import ma.vi.xml.XmlReaderBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.repeat;
import static java.util.Map.Entry;
import static ma.vi.lang.Literal.NULL_LITERAL;
import static ma.vi.serialization.Mapper.OBJ_NAME_PREFIX;
import static ma.vi.serialization.Mapper.uniqueObjectName;
import static ma.vi.xml.Fragment.Type.T_START_ELEMENT;

/**
 * The XML serializer.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class XmlSerializer extends AbstractSerializerWriter {
  @Override
  public Mapped toMap(Reader in) {
    try (XmlReader xml = XmlReaderBuilder.newBuilder(in)
        .rewindCapacity(2)
        .discardInterElementSpaces(true)
        .build()) {
      Mapped map = new Mapped();

      // read root element
      xml.next();

      // read each object which appear as a top-element inside the root element
      Fragment de;
      Set<String> multiRefObjects = null;
      while (xml.hasNext() && (de = xml.next()).type == T_START_ELEMENT) {
        String objectName = de.text;
        String type = de.attributes.get("type");

        if (multiRefObjects == null) {
          multiRefObjects = new HashSet<>();
          map.singleRefObjects.add(objectName);
        }

        readObject(map, objectName, type, multiRefObjects, xml);
      }
      return map;
    }
  }

  protected void readObject(Mapped map, String objectName, String type, Set<String> multiRefObjects, XmlReader xml) {
    Fragment de;
    map.types.put(objectName, type);
    Map<String, String> object = new LinkedHashMap<>();
    map.objects.put(objectName, object);

    while ((de = xml.next()).type == T_START_ELEMENT) {
      String fieldName = de.text;
      String valueType = de.attributes.get("type");
      if (valueType != null) {
        String valueObjectName = uniqueObjectName(object, fieldName, map.objects.keySet());
        map.singleRefObjects.add(valueObjectName);
        readObject(map, valueObjectName, valueType, multiRefObjects, xml);
        object.put(fieldName, valueObjectName);

      } else {
        // read the element contents
        de = xml.next();
        if (de.text.equals(NULL_LITERAL)) {
          object.put(fieldName, null);
        } else {
          object.put(fieldName, de.text);

          // if refers to another object, update referencing sets.
          if (de.text.startsWith(OBJ_NAME_PREFIX)) {
            if (map.singleRefObjects.contains(de.text)) {
              multiRefObjects.add(de.text);
              map.singleRefObjects.remove(de.text);

            } else if (!multiRefObjects.contains(de.text)) {
              map.singleRefObjects.add(de.text);
            }
          }
        }

        // read the end element
        xml.next();
      }
    }
  }

  @Override
  protected void writeHeader(Mapped map, Writer out) throws IOException {
    out.write("<?xml version='1.0' encoding='" + encoding.name() + "'?>" + lineSeparator);
    out.write("<" + rootElement + ">" + lineSeparator);
  }

  @Override
  protected void writeFooter(Mapped map, Writer out) throws IOException {
    out.write("</" + rootElement + ">");
  }

  @Override
  protected void writeObjectStart(Mapped map, Writer out,
                                  String name, String type,
                                  String indent, Map<String, String> object,
                                  boolean first) throws IOException {
    out.write(indent + "<" + name + " type='" + type + "'>" + lineSeparator);
  }

  @Override
  protected void writeObjectEnd(Mapped map, Writer out,
                                String name, String type,
                                String indent, Map<String, String> object,
                                boolean first) throws IOException {
    out.write(indent + "</" + name + '>' + lineSeparator);
  }

  @Override
  protected void writeObject(Mapped map,
                             Writer out,
                             String name,
                             String type,
                             String indent,
                             Map<String, String> object,
                             boolean first,
                             Set<String> written,
                             int level) throws IOException {
    written.add(name);
    String indentation = repeat(indent, level);
    for (Entry<String, String> field : object.entrySet()) {
      String fieldName = field.getKey();
      String value = field.getValue();
      if (value == null) {
        value = NULL_LITERAL;
      }
      if (inlineSingleRefObjects &&
          value.startsWith(OBJ_NAME_PREFIX) &&
          map.singleRefObjects.contains(value) &&
          map.objects.containsKey(value)) {

        out.write(indentation + '<' + fieldName + " type='" + map.types.get(value) + "'>" + lineSeparator);
        writeObject(map, out, value, map.types.get(value), indent, map.objects.get(value), false, written, level + 1);
        out.write(indentation + "</" + fieldName + '>' + lineSeparator);

      } else {
        if (value.contains("<") || value.contains(">") || value.contains("&")) {
          value = "<![CDATA[" + value + "]]>";
        }
        out.write(indentation + '<' + fieldName + '>' + value + "</" + fieldName + '>' + lineSeparator);
      }
    }
  }

  /**
   * The root element to use: defaults to 'root'.
   */
  String rootElement = "root";
}