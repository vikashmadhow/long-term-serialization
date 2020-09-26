/*
 * Copyright (c) 2020 2017 Vikash Madhow
 */

package ma.vi.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.core.JsonToken.*;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.repeat;
import static java.util.Map.Entry;
import static ma.vi.serialization.Mapper.OBJ_NAME_PREFIX;
import static ma.vi.serialization.Mapper.uniqueObjectName;

/**
 * The JSON serializer.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class JsonSerializer extends AbstractSerializerWriter {
  @Override
  public Mapped toMap(Reader in) throws IOException {
    try (JsonParser json = new JsonFactory().createParser(in)) {
      Mapped map = new Mapped();

      // read start of json object
      checkState(json.nextToken() == START_OBJECT,
          "JSON object data must start with a root object");

      // Iterate over object fields:
      Set<String> multiRefObjects = null;
      while (json.nextToken() != END_OBJECT) {
        String objectName = json.getCurrentName();

        checkState(json.nextToken() == START_OBJECT,
            "Object definition could not be found for object " + objectName);

        // first field name must be class and contains the type of the object
        checkState(json.nextToken() == FIELD_NAME,
            "No field found inside object definition; it must have at " +
                "least one containing its type name");

        String fieldName = json.getCurrentName();
        checkState(fieldName.equals("class"),
            "The first expected field must be named 'class' and contains " +
                "the type of the object; instead it is named '" + fieldName + '\'');

        json.nextToken();
        String type = json.getText();

        if (multiRefObjects == null) {
          multiRefObjects = new HashSet<>();
          map.singleRefObjects.add(objectName);
        }
        readObject(map, objectName, type, multiRefObjects, json);
      }
      return map;
    }
  }

  private void readObject(Mapped map, String objectName, String type, Set<String> multiRefObjects, JsonParser json) throws IOException {
    map.types.put(objectName, type);
    Map<String, String> object = new LinkedHashMap<>();
    map.objects.put(objectName, object);

    while (json.nextToken() != END_OBJECT) {
      String fieldName = json.getCurrentName();
      if (json.nextToken() == START_OBJECT) {
        // first field name must be class and contains the type of the object
        checkState(json.nextToken() == FIELD_NAME,
            "No field found inside object definition; it must have at " +
                "least one containing its type name");

        String objFieldName = json.getCurrentName();
        checkState(objFieldName.equals("class"),
            "The first expected field must be named 'class' and contains " +
                "the type of the object; instead it is named '" + objFieldName + '\'');

        json.nextToken();
        String valueType = json.getText();

        String valueObjectName = uniqueObjectName(object, fieldName, map.objects.keySet());
        map.singleRefObjects.add(valueObjectName);
        readObject(map, valueObjectName, valueType, multiRefObjects, json);
        object.put(fieldName, valueObjectName);

      } else {
        // read the element contents
        String text = json.getText();
        if (text == null) {
          object.put(fieldName, null);
        } else {
          object.put(fieldName, text);

          // if refers to another object, update referencing sets.
          if (text.startsWith(OBJ_NAME_PREFIX)) {
            if (map.singleRefObjects.contains(text)) {
              multiRefObjects.add(text);
              map.singleRefObjects.remove(text);

            } else if (!multiRefObjects.contains(text)) {
              map.singleRefObjects.add(text);
            }
          }
        }
      }
    }
  }

  @Override
  protected void writeHeader(Mapped map, Writer out) throws IOException {
    out.write('{');
  }

  @Override
  protected void writeFooter(Mapped map, Writer out) throws IOException {
    out.write(lineSeparator + '}');
  }

  @Override
  protected void writeObjectStart(Mapped map, Writer out,
                                  String name, String type,
                                  String indent, Map<String, String> object,
                                  boolean first) throws IOException {
    if (!first) {
      out.write(',');
    }
    out.write(lineSeparator + indent + '"' + name + "\": {" + lineSeparator);
    out.write(indent + indent + "\"class\": \"" + type + '"');
  }

  @Override
  protected void writeObjectEnd(Mapped map, Writer out,
                                String name, String type,
                                String indent, Map<String, String> object,
                                boolean first) throws IOException {
    out.write(lineSeparator + indent + '}');
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
      out.write(',' + lineSeparator);
      if (value == null) {
        out.write(indentation + '"' + fieldName + "\": null");

      } else if (inlineSingleRefObjects && value.startsWith(OBJ_NAME_PREFIX) &&
          map.singleRefObjects.contains(value) && map.objects.containsKey(value)) {

        out.write(indentation + '"' + fieldName + "\": {" + lineSeparator);
        out.write(indentation + indent + "\"class\": \"" + map.types.get(value) + '"');
        writeObject(map, out, value, map.types.get(value), indent, map.objects.get(value), false, written, level + 1);
        out.write(lineSeparator + indentation + '}');

      } else {
        out.write(indentation + '"' + fieldName + "\": \"" + escapeText(value) + '"');
      }
    }
  }

  static String escapeText(String text) {
    return backSlash.replaceFrom(
        doubleQuote.replaceFrom(text, "\\\""),
        "\\\\");
  }

  private static final CharMatcher doubleQuote = CharMatcher.is('"');
  private static final CharMatcher backSlash = CharMatcher.is('\\');
}