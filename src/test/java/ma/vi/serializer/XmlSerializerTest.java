/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import ma.vi.collections.Maps;
import ma.vi.tuple.T2;
import ma.vi.tuple.T3;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static ma.vi.lang.Literal.NULL_LITERAL;
import static ma.vi.serializer.Mapper.OBJ_NAME_PREFIX;
import static ma.vi.serializer.MapperTest.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class XmlSerializerTest {
  private A a;
  private B b;
  private C c;
  private D d;
  private E e;
  private F f;
  private G g;
  private int[] h;
  private String[][] i;
  private G[][] j;
  private K k;

  private final String ls = "\n";

  @Before
  public void init() {
    a = new A();
    a.a = "Test";
    a.b = 10;

    b = new B();
    b.a = -10;
    b.b = "Another test";
    b.c = a;

    c = new C();
    c.c = c;

    d = new D();
    E e = new E();
    d.e = e;
    e.d = d;
    e.e = e;

    f = F.a;

    g = new G();
    g.a = F.b;
    g.b = new F[]{F.a, F.a, F.b, F.c};

    h = new int[]{1, 2, 3};
    i = new String[][]{{"a"}, {"c", "d"}};
    j = new G[][]{new G[]{g}, new G[]{g, g}};

    k = new K();
    k.a = h;
    k.b = new String[][]{{}, {"", null, "["}, {"]", ",", "[,]"}};
    k.c = new G[]{g, g};
  }

  /**
   * Checks toMap(a) equals:
   * <pre>
   *  class A {
   *    String a;
   *    int b;
   *  }
   *  </pre>
   */
  @Test
  public void mapA() throws Exception {
    Mapped map = Mapper.toMap(a);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().build();
    String serialized1 = ser.toText(map);
    assertEquals(serialized1,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "a type='" + A.class.getName() + "'>" + ls +
                     "    <a>Test</a>" + ls +
                     "    <b>10</b>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "a>" + ls +
                     "</root>"
    );

    ser = XmlSerializerBuilder.newBuilder()
                              .indentSpaces(0)
                              .rootElement("r")
                              .lineSeparator("")
                              .build();
    String serialized2 = ser.toText(map);
    assertEquals(serialized2,
                 "<?xml version='1.0' encoding='UTF-8'?>" +
                     "<r>" +
                     "<" + OBJ_NAME_PREFIX + "a type='" + A.class.getName() + "'>" +
                     "<a>Test</a>" +
                     "<b>10</b>" +
                     "</" + OBJ_NAME_PREFIX + "a>" +
                     "</r>"
    );

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      String objectName = map.types.keySet().iterator().next();
      assertEquals(map.types.size(), 1);
      assertEquals(map.types.get(objectName), A.class.getName());
      assertEquals(map.objects.size(), 1);
      assertEquals(map.objects,
                   Map.of(objectName, Map.of("a", "Test", "b", "10")));
      assertEquals(map.singleRefObjects, Set.of(objectName));
    }
  }

  /**
   * Checks that toMap(b) produces:
   * <pre>
   *  b[B]: {
   *    a: -10,
   *    b: "Another test",
   *    c: a
   *  },
   *  a[A]: {
   *    a: "Test",
   *    b: 10
   *  }
   */
  @Test
  public void mapB() throws Exception {
    Mapped map = Mapper.toMap(b);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    assertEquals(serialized1,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "b type='" + B.class.getName() + "'>" + ls +
                     "    <a>-10</a>" + ls +
                     "    <b>Another test</b>" + ls +
                     "    <c>" + OBJ_NAME_PREFIX + "a</c>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "b>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "a type='" + A.class.getName() + "'>" + ls +
                     "    <a>Test</a>" + ls +
                     "    <b>10</b>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "a>" + ls +
                     "</root>"
    );

    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);
    assertEquals(serialized2,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "b type='" + B.class.getName() + "'>" + ls +
                     "    <a>-10</a>" + ls +
                     "    <b>Another test</b>" + ls +
                     "    <c type='" + A.class.getName() + "'>" + ls +
                     "      <a>Test</a>" + ls +
                     "      <b>10</b>" + ls +
                     "    </c>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "b>" + ls +
                     "</root>"
    );

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(map.types.size(), 2);

      Map<String, String> typeToName = map.types.entrySet()
                                                .stream()
                                                .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
      assertEquals(typeToName.keySet(), Set.of(A.class.getName(), B.class.getName()));

      String[] objectName = new String[] {
          typeToName.get(B.class.getName()),
          typeToName.get(A.class.getName())
      };

      assertEquals(map.objects.size(), 2);
      assertEquals(map.objects, Map.of(
          objectName[0],
          Map.of("a", "-10", "b", "Another test", "c", objectName[1]),
          objectName[1],
          Map.of("a", "Test", "b", "10")));
      assertEquals(map.singleRefObjects, Set.of(objectName));
    }
  }

  /**
   * Checks that toMap(c) equals:
   * <pre>
   *  c[C]: {
   *    c: c
   *  }
   * </pre>
   */
  @Test
  public void mapC() throws Exception {
    Mapped map = Mapper.toMap(c);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    assertEquals(serialized1,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "c type='" + C.class.getName() + "'>" + ls +
                     "    <c>" + OBJ_NAME_PREFIX + "c</c>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "c>" + ls +
                     "</root>"
    );

    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);
    assertEquals(serialized2, serialized1);

    ser = new XmlSerializer();
    map = ser.toMap(serialized1);
    String objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), C.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("c", objectName)));
    assertEquals(map.singleRefObjects, emptySet());
  }

  /**
   * Checks that toMap(d) produces:
   * <pre>
   *  d[D]: {
   *    e: e
   *  },
   *  e[E]: {
   *    d: d
   *    e: e
   *  }
   *  </pre>
   */
  @Test
  public void mapD() throws Exception {
    Mapped map = Mapper.toMap(d);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    assertEquals(serialized1,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "d type='" + D.class.getName() + "'>" + ls +
                     "    <e>" + OBJ_NAME_PREFIX + "e</e>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "d>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "e type='" + E.class.getName() + "'>" + ls +
                     "    <d>" + OBJ_NAME_PREFIX + "d</d>" + ls +
                     "    <e>" + OBJ_NAME_PREFIX + "e</e>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "e>" + ls +
                     "</root>"
    );

    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);
    assertEquals(serialized2, serialized1);

    ser = new XmlSerializer();
    map = ser.toMap(serialized1);
    assertEquals(map.types.size(), 2);

    Map<String, String> typeToName = map.types.entrySet()
                                              .stream()
                                              .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
    assertEquals(typeToName.keySet(), Set.of(D.class.getName(), E.class.getName()));

    String[] objectName = new String[]{
        typeToName.get(D.class.getName()),
        typeToName.get(E.class.getName())
    };

    assertEquals(map.objects.size(), 2);
    assertEquals(map.objects, Map.of(
        objectName[0],
        Map.of("e", objectName[1]),
        objectName[1],
        Map.of("d", objectName[0], "e", objectName[1])));
    assertEquals(map.singleRefObjects, emptySet());
  }

  /**
   * Checks special object mapping. Specifically that:
   * <ul>
   * <li><b>Mapper.toMap(null)</b> will produce an empty map.</li>
   * <li><b>Mapper.toMap(f)</b> (an enum) will produce: 'f[F]: {f: a}'</li>
   * <li><b>Mapper.toMap(42)</b> will produce: 'a[int]: {int: 42}'</li>
   * <li><b>Mapper.toMap(h)</b> will produce: 'a[int[3]]: {int: [1,2,3]}'</li>
   * <li><b>Mapper.toMap(i)</b> will produce: 'a[java.lang.String[2][]]: {string: [[a],[c,d]]}'</li>
   * </ul>
   */
  @Test
  public void mapSpecial() throws Exception {
    // nulls map to the empty map
    Mapped map = Mapper.toMap(null);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized = ser.toText(map);
    assertEquals(serialized,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls + "</root>"
    );
    map = ser.toMap(serialized);
    assertEquals(map.types, emptyMap());
    assertEquals(map.objects, emptyMap());
    assertEquals(map.singleRefObjects, emptySet());

    // enum maps to their names
    map = Mapper.toMap(f);
    ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    serialized = ser.toText(map);
    assertEquals(serialized,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "f type='" + F.class.getName() + "'>" + ls +
                     "    <f>a</f>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "f>" + ls +
                     "</root>"
    );
    map = ser.toMap(serialized);
    String objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), F.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName, Map.of("f", "a")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    // And so does other literals
    map = Mapper.toMap(42);
    ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    serialized = ser.toText(map);
    assertEquals(serialized,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "integer type='" + Integer.class.getName() + "'>" + ls +
                     "    <integer>42</integer>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "integer>" + ls +
                     "</root>"
    );
    map = ser.toMap(serialized);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), Integer.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("integer", "42")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    // Arrays are also literals
    map = Mapper.toMap(h);
    ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    serialized = ser.toText(map);
    assertEquals(serialized,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "int type='" + int.class.getName() + "[3]'>" + ls +
                     "    <int>[1,2,3]</int>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "int>" + ls +
                     "</root>"
    );
    map = ser.toMap(serialized);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), int.class.getName() + "[3]");
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("int", "[1,2,3]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    map = Mapper.toMap(i);
    ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    serialized = ser.toText(map);
    assertEquals(serialized,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "string type='" + String.class.getName() + "[2][]'>" + ls +
                     "    <string>[[a],[c,d]]</string>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "string>" + ls +
                     "</root>"
    );
    map = ser.toMap(serialized);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), String.class.getName() + "[2][]");
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName, Map.of("string", "[[a],[c,d]]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));
  }

  /**
   * Checks that <b>Mapper.toMap(g)</b> produces:
   * <pre>
   *    g[G]: {a: b, b: [a,a,b,c]}
   * </pre>
   */
  @Test
  public void mapG() throws Exception {
    Mapped map = Mapper.toMap(g);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    assertEquals(serialized1,
                 "<?xml version='1.0' encoding='UTF-8'?>" + ls +
                     "<root>" + ls +
                     "  <" + OBJ_NAME_PREFIX + "g type='" + G.class.getName() + "'>" + ls +
                     "    <a>b</a>" + ls +
                     "    <b>[a,a,b,c]</b>" + ls +
                     "  </" + OBJ_NAME_PREFIX + "g>" + ls +
                     "</root>"
    );

    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);
    assertEquals(serialized1, serialized2);

    map = ser.toMap(serialized1);
    String objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), G.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName, Map.of("a", "b", "b", "[a,a,b,c]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));
  }

  @Test
  public void mapJ() throws Exception {
    Mapped map = Mapper.toMap(j);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);

    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(map.types.size(), 4);
      Map<String, String> typeToName = map.types.entrySet()
                                                .stream()
                                                .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
      assertEquals(typeToName.keySet(), Set.of(
          G.class.getName() + "[2][]",
          G.class.getName() + "[1]",
          G.class.getName() + "[2]",
          G.class.getName()));

      String[] objectName = new String[]{
          typeToName.get(G.class.getName() + "[2][]"),
          typeToName.get(G.class.getName() + "[1]"),
          typeToName.get(G.class.getName() + "[2]"),
          typeToName.get(G.class.getName())
      };

      assertEquals(map.objects.size(), 4);
      assertEquals(map.objects, Map.of(
          objectName[0],
          Map.of("i0", objectName[1], "i1", objectName[2]),
          objectName[1],
          Map.of("i0", objectName[3]),
          objectName[2],
          Map.of("i0", objectName[3], "i1", objectName[3]),
          objectName[3],
          Map.of("a", "b", "b", "[a,a,b,c]")));
      assertEquals(map.singleRefObjects, Set.of(objectName[0], objectName[1], objectName[2]));
    }
  }

  @Test
  public void mapK() throws Exception {
    Mapped map = Mapper.toMap(k);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(map.types.size(), 3);

      Map<String, String> typeToName = map.types.entrySet()
                                                .stream()
                                                .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
      assertEquals(typeToName.keySet(), Set.of(
          K.class.getName(),
          G.class.getName() + "[2]",
          G.class.getName()
      ));

      String[] objectName = new String[]{
          typeToName.get(K.class.getName()),
          typeToName.get(G.class.getName() + "[2]"),
          typeToName.get(G.class.getName()),
      };

      assertEquals(map.objects.size(), 3);
      assertEquals(map.objects, Map.of(
          objectName[0],
          Map.of("a", "[1,2,3]",
                 "b", "[[],[," + NULL_LITERAL + ",\\[],[\\],\\,,\\[\\,\\]]]",
                 "c", objectName[1]),
          objectName[1],
          Map.of("i0", objectName[2], "i1", objectName[2]),
          objectName[2],
          Map.of("a", "b", "b", "[a,a,b,c]")));
      assertEquals(map.singleRefObjects, Set.of(objectName[0], objectName[1]));
    }
  }

  @Test
  public void mapList() throws Exception {
    List<String> obj = Arrays.asList("a", "b", "c", "d");
    Mapped map = Mapper.toMap(obj);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(Mapper.fromMap(map), obj);
    }
  }

  @Test
  public void mapListOfObjects() throws Exception {
    List<T2<String, Integer>> listOfObjects = Arrays.asList(
        T2.of("a", 1), T2.of("b", 2),
        T2.of("c", 3), T2.of("d", 4));
    Mapped map = Mapper.toMap(listOfObjects);

    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(Mapper.fromMap(map), listOfObjects);
    }
  }

  @Test
  public void mapMap() throws Exception {
    Map<String, Integer> obj1 = Maps.of(
        T2.of("a", 1), T2.of("b", 2),
        T2.of("c", 3), T2.of("d", 4));

    Mapped map = Mapper.toMap(obj1);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(Mapper.fromMap(map), obj1);
    }

  }

  @Test
  public void mapMap2() throws Exception {
    Date now = new Date(System.currentTimeMillis());
    Map<T2<String, Integer>, Date> obj2 = Maps.of(
        T2.of(T2.of("a", 1), now), T2.of(T2.of("b", 2), now),
        T2.of(T2.of("c", 3), now), T2.of(T2.of("d", 4), now));

    Mapped map = Mapper.toMap(obj2);
    XmlSerializer ser = XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build();
    String serialized1 = ser.toText(map);
    ser = XmlSerializerBuilder.newBuilder().build();
    String serialized2 = ser.toText(map);

    ser = new XmlSerializer();
    for (String serialized: new String[]{serialized1, serialized2}) {
      map = ser.toMap(serialized);
      assertEquals(Mapper.fromMap(map), obj2);
    }
  }

  @Test
  public void mapPerf() throws Exception {
    Date now = new Date(System.currentTimeMillis());
    Map<T3<String, Integer, Date>, K> obj = Maps.of(
        T2.of(T3.of("a", 1, now), k), T2.of(T3.of("b", 2, now), k),
        T2.of(T3.of("c", 3, now), k), T2.of(T3.of("d", 4, now), k));
    for (int i = 0; i < 10000; i++) {
      obj.put(T3.of("x", i, now), k);
    }

    Mapped map = Mapper.toMap(obj);
    XmlSerializer xs = XmlSerializerBuilder.newBuilder().build();
    String objText = xs.toText(map);
    Mapped fromText = xs.toMap(objText);
    Object reconstructed = Mapper.fromMap(fromText);
    assertEquals(reconstructed, obj);

    for (XmlSerializer ser: new XmlSerializer[]{XmlSerializerBuilder.newBuilder().build(),
        XmlSerializerBuilder.newBuilder().inlineSingleRefObjects(false).build()}) {
      // heat up
      int reps = 3;
      System.out.println("Heating up...");
      String text = ser.toText(map);

      for (int i = 0; i < reps; i++) {
        String s = ser.toText(map);
        ser.toMap(s);
      }

      // assess performance
      int a = 0;
      System.out.println("Computing performance to serialize...");
      long start = System.currentTimeMillis();
      for (int i = 0; i < reps; i++) {
        String s = ser.toText(map);
        a |= System.identityHashCode(s);
      }
      System.out.println(a);
      System.out.println("Time taken to serialize: " + ((System.currentTimeMillis() - start) / reps));


      System.out.println("Computing performance to deserialize...");
      start = System.currentTimeMillis();
      for (int i = 0; i < reps; i++) {
        Mapped map2 = ser.toMap(text);
        a |= System.identityHashCode(map2);
      }
      System.out.println(a);
      System.out.println("Time taken to reconstruct: " + ((System.currentTimeMillis() - start) / reps));
    }
  }
}
