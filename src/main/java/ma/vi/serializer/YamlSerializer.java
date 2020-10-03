/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.repeat;
import static java.util.Map.Entry;
import static ma.vi.serializer.Mapper.OBJ_NAME_PREFIX;
import static ma.vi.serializer.Mapper.uniqueObjectName;

/**
 * The YAML serializer.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class YamlSerializer extends AbstractSerializerWriter {

  @Override
  public Mapped toMap(Reader in) throws IOException {
    Yaml yaml = new Yaml();
    Mapped map = new Mapped();
    Set<String> multiRefObjects = null;

    String s;
    Iterator<Event> parser = yaml.parse(in).iterator();
    while (parser.hasNext() && !(parser.next() instanceof MappingStartEvent)) ;  // read up to first mapping

    Event e;
    while (parser.hasNext() && !((e = parser.next()) instanceof MappingEndEvent)) {
      String objectName = ((ScalarEvent) e).getValue();

      // get type in first element of mapping
      parser.next();
      e = parser.next();
      checkState((s = ((ScalarEvent) e).getValue()).equals("class"), "Expecting 'class', found '" + s + '\'');

      e = parser.next();
      String type = ((ScalarEvent) e).getValue();

      if (multiRefObjects == null) {
        multiRefObjects = new HashSet<>();
        map.singleRefObjects.add(objectName);
      }
      readObject(map, objectName, type, multiRefObjects, parser);
    }
    return map;
  }

  private void readObject(Mapped map, String objectName, String type, Set<String> multiRefObjects, Iterator<Event> parser) throws IOException {
    map.types.put(objectName, type);
    Map<String, String> object = new LinkedHashMap<>();
    map.objects.put(objectName, object);

    Event e;
    while (!((e = parser.next()) instanceof MappingEndEvent)) {
      String fieldName = ((ScalarEvent) e).getValue();
      if ((e = parser.next()) instanceof MappingStartEvent) {
        // first field name must be class and contains the type of the object
        parser.next();
        e = parser.next();

        String valueType = ((ScalarEvent) e).getValue();
        String valueObjectName = uniqueObjectName(object, fieldName, map.objects.keySet());

        map.singleRefObjects.add(valueObjectName);
        readObject(map, valueObjectName, valueType, multiRefObjects, parser);
        object.put(fieldName, valueObjectName);

      } else {
        // read the element contents
        String text = ((ScalarEvent) e).getValue();
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
  }

  @Override
  protected void writeFooter(Mapped map, Writer out) throws IOException {
  }

  @Override
  protected void writeObjectStart(Mapped map, Writer out,
                                  String name, String type,
                                  String indent, Map<String, String> object,
                                  boolean first) throws IOException {
    out.write(name + ':' + lineSeparator);
    out.write(indent + " class: " + type + lineSeparator);
  }

  @Override
  protected void writeObjectEnd(Mapped map, Writer out,
                                String name, String type,
                                String indent, Map<String, String> object,
                                boolean first) throws IOException {
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
    String indentation = repeat(indent, level - 1);
    for (Entry<String, String> field : object.entrySet()) {
      String fieldName = field.getKey();
      String value = field.getValue();
      if (value == null) {
        out.write(indentation + ' ' + fieldName + ": null" + lineSeparator);

      } else if (inlineSingleRefObjects &&
          value.startsWith(OBJ_NAME_PREFIX) &&
          map.singleRefObjects.contains(value) &&
          map.objects.containsKey(value)) {

        out.write(indentation + ' ' + fieldName + ':' + lineSeparator);
        out.write(indentation + indent + " class: " + map.types.get(value) + lineSeparator);
        writeObject(map, out, value, map.types.get(value), indent, map.objects.get(value), false, written, level + 1);

      } else {
        out.write(indentation + ' ' + fieldName + ": \"" + JsonSerializer.escapeText(value) + '"' + lineSeparator);
      }
    }
  }
}