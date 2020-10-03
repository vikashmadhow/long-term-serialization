/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ma.vi.reflect.Dissector;
import ma.vi.tuple.T2;

import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.Integer.parseInt;
import static java.lang.reflect.Modifier.*;
import static java.util.Collections.singletonMap;
import static ma.vi.lang.Errors.unchecked;
import static ma.vi.lang.Literal.*;
import static ma.vi.lang.Names.toIdentifier;
import static ma.vi.lang.Names.uniqueRandomName;
import static ma.vi.lang.UnsafeLoader.unsafe;
import static ma.vi.reflect.Classes.*;
import static ma.vi.string.Strings.uncapFirst;
import static org.apache.commons.lang3.StringUtils.countMatches;

/**
 * <p>
 * Produces a representation of an object graph as a flat map of string to primitives. This
 * map is a more malleable intermediate form which can be then be more easily serialized to
 * various forms, textual (XML, JSON, YAML, etc.) or binary.
 *
 * <p>
 * For example, given:
 *
 * <pre>
 *  class A {
 *    String a;
 *    int b;
 *  }
 *
 *  class B {
 *      int a;
 *      String b;
 *      A c;
 *  }
 *
 *  class C {
 *      C c;
 *  }
 *
 *  class D {
 *      E e;
 *  }
 *
 *  class E {
 *      D d;
 *      E e;
 *  }
 *
 *  enum F {
 *      a, b, c
 *  }
 *
 *  class G {
 *      F a;
 *      F[] b;
 *  }
 *
 *  Class K {
 *      int [] a;
 *      String[][] b;
 *      G[] c;
 *  }
 * </pre>
 * and
 * <pre>
 *  A a = new A();
 *  a.a = "Test";
 *  a.b = 10;
 *
 *  B b = new B();
 *  b.a = -10;
 *  b.b = "Another test";
 *  b.c = a;
 *
 *  C c = new C();
 *  c.c = c;
 *
 *  D d = new D();
 *  E e = new E();
 *  d.e = e;
 *  e.d = d;
 *  e.e = e;
 *
 *  F f = F.a;
 *
 *  G g = new G();
 *  g.a = F.b;
 *  g.b = new F[] {a, a, b, c};
 *
 *  h = new int[] {1, 2, 3};
 *  i = new String[][] {{"a"}, {"c", "d"}};
 *  j = new G[][] {new G[]{g}, new G[]{g, g}};
 *
 *  K k = new K();
 *  k.a = h;
 *  k.b = new String[][]{{}, {"", null, "["}, {"]", ",", "[,]"}};
 *  k.c = new G[]{g, g}
 *  </pre>
 * <p>
 * In the following, {x:y,....} represents a map with x as a a and y a b
 * with the surrounding parenthesis of the top-level map not shown. a[A] represents
 * a a with name a and type A; only top-level names need explicit types as internal
 * types can be determined from the target field type during unmapping.
 *
 * <ul>
 * <li>
 * <b>Mapper.toMap(a)</b> will produce:
 * <pre>
 *    a[A]: {
 *      a: "Test",
 *      b: 10
 *    }
 *    </pre>
 * </li>
 *
 * <li>
 * <b>Mapper.toMap(b)</b> will produce:
 * <pre>
 *    b[B]: {
 *      a: -10,
 *      b: "Another test",
 *      c: a
 *    },
 *    a[A]: {
 *      a: "Test",
 *      b: 10
 *    }
 *    </pre>
 * </li>
 *
 * <li>
 * <b>Mapper.toMap(c)</b> will produce:
 * <pre>
 *    c[C]: {
 *      c: c
 *    }
 *    </pre>
 * </li>
 *
 * <li>
 * <b>Mapper.toMap(d)</b> will produce:
 * <pre>
 *    d[D]: {
 *      e: e
 *    },
 *    e[E]: {
 *      d: d
 *      e: e
 *    }
 *    </pre>
 * </li>
 * </ul>
 * <p>
 * The Mapper can also map primitive and special objects although this is
 * not its primary purpose. Literal types ({@link ma.vi.lang.Literal}) which
 * includes primitives and arrays of literals are mapped to their literal
 * (textual) form resulting in a more concise representation while remaining
 * easily human-readable:
 *
 * <ul>
 * <li><b>Mapper.toMap(null)</b> will produce an empty map.</li>
 * <li><b>Mapper.toMap(f)</b> (an enum) will produce: 'f[F]: {f: a}'</li>
 * <li><b>Mapper.toMap(42)</b> will produce: 'a[int]: {int: 42}'</li>
 * <li><b>Mapper.toMap(h)</b> will produce: 'a[int[3]]: {int: [1, 2, 3]}'</li>
 * <li><b>Mapper.toMap(i)</b> will produce: 'a[java.lang.String[2][]]: {string: [[a], [c, d]]}'</li>
 * </ul>
 * <p>
 * The output produced by the mapper, if serialised as shown here, remains
 * reasonably human-readable, but still can get quite complex, especially
 * in relation to the number of links in th object graph:
 *
 * <ul>
 * <li>
 * <b>Mapper.toMap(g)</b> will produce:
 * <pre>
 *    g[G]: {a: b, b: [a, a, b, c]}
 * </pre>
 * </li>
 *
 * <li>
 * <b>Mapper.toMap(j)</b> will produce:
 * <pre>
 *    a[G[2][]]: {0: b, 1: c}
 *    b[G[1]]: {0: d}
 *    c[G[2]]: {0: d, 1: d}
 *    d[G]: {a: b, b: [a, a, b, c]}
 * </pre>
 * </li>
 *
 * <li>
 * <b>Mapper.toMap(k)</b> will produce:
 * <pre>
 *    k[K]: {a: [1, 2, 3], b: [[], [,\N,\[],[\],\,,\[\,\]]], c: c}
 *    c[G[2]]: {0: d, 1: d}
 *    d[G]: {a: b, b: [a, a, b, c]}
 * </pre>
 * </li>
 * </ul>
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class Mapper {
  /**
   * Creates the map for the object graph anchored at the object supplied returning
   * a pair of maps. The first one maps the name of each object in the graph to a
   * type-string describing its type. The second map contains the actual flattened
   * map for each object mapped to its name.
   */
  public static Mapped toMap(Object object) {
    Mapped map = new Mapped();
    _toMap(object, new HashSet<>(), map, new IdentityHashMap<>());
    return map;
  }

  /**
   * Reconstruct an object graph from the types and flattened map produced by the
   * {@link #toMap(Object)} methods.
   */
  public static Object fromMap(Mapped map) {
    return fromMap(null, map);
  }

  /**
   * Same as the {@link #fromMap(Mapped)} but using an explicit for the first object
   * of the object graph to reconstruct. If this is null, the first object in the object
   * map is reconstructed.
   */
  public static Object fromMap(String objectName, Mapped mapped) {
    if (objectName == null) {
      objectName = mapped.objects.isEmpty() ? "unknown" : mapped.objects.keySet().iterator().next();
    }
    return _fromMap(objectName, mapped, new IdentityHashMap<>());
  }

  /**
   * Generates an object name for the object, using the proposed name. If latter is null,
   * the object name is based solely on the class of the object. The {@link #OBJ_NAME_PREFIX}
   * is added to the object name if it is not already present.
   */
  public static String objectName(Object object, String proposedName) {
    if (proposedName == null) {
      if (object == null) {
        proposedName = OBJ_NAME_PREFIX + "null";
      } else {
        Class<?> cls = classOf(object);
        if (cls.isArray()) {
          proposedName = OBJ_NAME_PREFIX + uncapFirst(toIdentifier(componentType(cls).getSimpleName())) + "_array";
        } else {
          proposedName = OBJ_NAME_PREFIX + uncapFirst(toIdentifier(cls.getSimpleName()));
        }
      }
    }
    if (!proposedName.startsWith(OBJ_NAME_PREFIX)) {
      proposedName = OBJ_NAME_PREFIX + proposedName;
    }
    return proposedName;
  }

  /**
   * Returns a valid object name which is built from the potential name and unique within the
   * of names supplied.
   */
  public static String uniqueObjectName(Object object, String potentialName, Set<String> existingNames) {
    if (potentialName == null) {
      Class<?> cls = object.getClass();
      potentialName = uncapFirst(toIdentifier(cls.getSimpleName()));
    }
    return objectName(
        object,
        uniqueRandomName(
            objectName(object, potentialName),
            existingNames
        ));
  }

  /**
   * The prefix that all object name should have.
   */
  public static final String OBJ_NAME_PREFIX = "obj_ref_";

  /**
   * Internal method which does the mapping
   */
  private static String _toMap(Object object,
                               Set<String> names,
                               Mapped map,
                               IdentityHashMap<Object, String> mapped) {
    if (object == null) {
      return null;
    } else {
      String name = mapped.get(object);
      if (name == null) {
        Class cls = object.getClass();
        String potentialName = uncapFirst(toIdentifier(cls.getSimpleName()));
        name = uniqueObjectName(object, potentialName, names);
        names.add(name);
        map.singleRefObjects.add(name);
        map.types.put(name, typeOf(object));
        mapped.put(object, name);

        if (isLiteral(cls)) {
          // a literal is mapped to a single-entry map
          map.objects.put(name, singletonMap(potentialName, toText(cls, object)));

        } else if (cls.isArray()) {
          // map array
          Object[] array = (Object[]) object;
          int length = array.length;
          Map<String, String> objMap = new LinkedHashMap<>();
          map.objects.put(name, objMap);
          for (int i = 0; i < length; i++) {
            Object v = array[i];
            objMap.put("i" + i, v == null ? null : _toMap(v, names, map, mapped));
          }
        } else {
          // map object
          Map<String, String> objMap = new LinkedHashMap<>();
          map.objects.put(name, objMap);

          // map fields
          Structure structure = classStructure.getUnchecked(cls);
          for (Field field : structure.mappedFields)
            try {
              Object value = field.get(object);
              String fName = field.getName();
              Class fType = field.getType();

              if (value == null) {
                objMap.put(fName, null);
              } else if (isLiteral(fType)) {
                objMap.put(fName, toText(fType, value));
              } else {
                objMap.put(fName, _toMap(value, names, map, mapped));
              }

            } catch (IllegalAccessException iae) {
              throw new RuntimeException("Could not access field " + field, iae);
            }
        }
      } else {
        map.singleRefObjects.remove(name);
      }
      return name;
    }
  }

  /**
   * Internal map method which reconstructs object from its map and types information.
   */
  private static Object _fromMap(String objectName, Mapped mapped, Map<String, Object> unmapped) {
    Object instance = unmapped.get(objectName);
    if (instance == null) {
      Map<String, String> map = mapped.objects.get(objectName);
      if (map != null) try {
        String type = mapped.types.get(objectName);
        int pos = type.indexOf('[');
        if (pos != -1) {
          // for array types, the type will be in the form type[x][][].... where type is
          // component type and x is the length of the first dimension. The length of the
          // other dimensions are not specified.
          Class<?> componentType = classOf(type.substring(0, pos));
          if (isLiteral(componentType)) {
            String value = map.values().iterator().next();
            instance = toValue(classOf(type), value);
            unmapped.put(objectName, instance);

          } else {
            int dimensions = countMatches(type, '[');
            int length = parseInt(type.substring(pos + 1, type.indexOf(']', pos + 1)));
            int[] lengths = new int[dimensions];
            lengths[0] = length;
            instance = Array.newInstance(componentType, lengths);
            unmapped.put(objectName, instance);

            // map array
            boolean literalComponentType = isLiteral(componentType);
            for (int i = 0; i < length; i++) {
              String value = map.get("i" + i);
              if (value == null) {
                Array.set(instance, i, null);
              } else if (dimensions > 1 || !literalComponentType) {
                Array.set(instance, i, _fromMap(value, mapped, unmapped));
              } else {
                Array.set(instance, i, instanceOf(componentType, T2.of(value, String.class)));
              }
            }
          }
        } else {
          Class<?> cls = classOf(type);
          if (isLiteral(cls)) {
            // literal are constructed using their default constructor taking a string value
            instance = toValue(cls, map.values().iterator().next());
            unmapped.put(objectName, instance);

          } else {
            instance = unsafe().allocateInstance(cls);
            unmapped.put(objectName, instance);
            for (Map.Entry<String, String> entry : map.entrySet()) {
              String fieldName = entry.getKey();
              String value = entry.getValue();

              Optional<Field> f = Dissector.field(cls, fieldName);
              if (f.isPresent()) {
                Field field = f.get();
                field.setAccessible(true);
                Class fieldType = field.getType();

                if (value == null) {
                  field.set(instance, null);
                } else if (isLiteral(fieldType)) {
                  field.set(instance, toValue(fieldType, value));
                } else {
                  field.set(instance, _fromMap(value, mapped, unmapped));
                }
              }
            }
          }
        }
      } catch (Exception e) {
        throw unchecked(e);
      }
    }
    return instance;
  }

  /**
   * Structure holding the fields to map for every class.
   */
  private static class Structure {
    Structure(Class<?> cls, Field[] mappedFields) {
      this.cls = cls;
      this.mappedFields = mappedFields;
    }

    final Class<?> cls;
    final Field[] mappedFields;
  }

  /**
   * Cache of class mapping structures.
   */
  private static final LoadingCache<Class<?>, Structure> classStructure =
      CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Structure load(Class<?> cls) {
          // check if object implements its own Java serialization protocol. For those
          // objects which do, we need to serialize transients as not serializing them
          // makes it impossible to reconstruct certain objects such as HashMaps. HashMaps,
          // for example, uses a transient array which it serializes outside of the
          // serialization protocol using the writeObject method. Since we are aiming for
          // independence from the serialization format, we cannot support such a custom
          // method at this level (at least not in its current form). Instead, we just
          // serialize everything and hope for the best.
          boolean bypassProtocol =
              Dissector.method(cls, "writeObject", ObjectOutputStream.class).isPresent() ||
                  Dissector.method(cls, "readObject", ObjectInputStream.class).isPresent() ||
                  Externalizable.class.isAssignableFrom(cls);

          // get all fields to mapped
          List<Field> fields = new ArrayList<>();
          for (Field field : Dissector.fields(cls).values()) {
            int fieldModifiers = field.getModifiers();
            if ((bypassProtocol && !(isFinal(fieldModifiers) && isStatic(fieldModifiers))) ||
                (!isTransient(fieldModifiers) &&
                    !(isFinal(fieldModifiers) && isStatic(fieldModifiers)))) {
              field.setAccessible(true);
              fields.add(field);
            }
          }
          return new Structure(cls, fields.toArray(new Field[0]));
        }
      });
}