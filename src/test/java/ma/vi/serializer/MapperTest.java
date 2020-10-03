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

import static java.util.Collections.*;
import static java.util.Map.Entry;
import static java.util.stream.Collectors.toMap;
import static ma.vi.lang.Literal.NULL_LITERAL;
import static org.junit.Assert.*;

/**
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class MapperTest {
  public static class A {
    String a;
    int b;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      A a1 = (A)o;
      return b == a1.b && a.equals(a1.a);
    }

    @Override
    public int hashCode() {
      int result = a.hashCode();
      result = 31 * result + b;
      return result;
    }

    @Override
    public String toString() {
      return "A{" +
          "a='" + a + '\'' +
          ", b=" + b +
          '}';
    }
  }

  public static class B extends A {
    int a;
    String b;
    A c;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      B b1 = (B)o;

      if (a != b1.a) return false;
      if (!Objects.equals(b, b1.b)) return false;
      return Objects.equals(c, b1.c);
    }

    @Override
    public int hashCode() {
      int result = a;
      result = 31 * result + (b != null ? b.hashCode() : 0);
      result = 31 * result + (c != null ? c.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "B{" +
          "a=" + a +
          ", b='" + b + '\'' +
          ", c=" + c +
          '}';
    }
  }

  public static class C {
    C c;
  }

  public static class D {
    E e;
  }

  public static class E {
    D d;
    E e;
  }

  public enum F {a, b, c}

  public static class G {
    F a;
    F[] b;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      G g = (G)o;
      return a == g.a && Arrays.equals(b, g.b);
    }

    @Override
    public int hashCode() {
      int result = a != null ? a.hashCode() : 0;
      result = 31 * result + Arrays.hashCode(b);
      return result;
    }

    @Override
    public String toString() {
      return "G{" +
          "a=" + a +
          ", b=" + Arrays.toString(b) +
          '}';
    }
  }

  public static class K {
    int[] a;
    String[][] b;
    G[] c;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      K k = (K)o;

      if (!Arrays.equals(a, k.a)) return false;
      if (!Arrays.deepEquals(b, k.b)) return false;
      return Arrays.equals(c, k.c);
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(a);
      result = 31 * result + Arrays.deepHashCode(b);
      result = 31 * result + Arrays.hashCode(c);
      return result;
    }

    @Override
    public String toString() {
      return "K{" +
          "a=" + Arrays.toString(a) +
          ", b=" + Arrays.toString(b) +
          ", c=" + Arrays.toString(c) +
          '}';
    }
  }

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
    String objectName = map.types.keySet().iterator().next();

    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), A.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName, Map.of("a", "Test", "b", "10")));
    assertEquals(map.singleRefObjects, singleton(objectName));
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
    assertEquals(map.types.size(), 2);

    Map<String, String> typeToName = map.types.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));
    assertEquals(typeToName.keySet(), Set.of(A.class.getName(), B.class.getName()));

    String[] objectName = new String[]{
        typeToName.get(B.class.getName()),
        typeToName.get(A.class.getName())
    };

    assertEquals(map.objects.size(), 2);
    assertEquals(map.objects, Map.of(
        objectName[0],
        Map.of("a", "-10", "b", "Another test", "c", objectName[1]),
        objectName[1],
        Map.of("a", "Test", "b", "10")
    ));
    assertEquals(map.singleRefObjects, Set.of(objectName));
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
    String objectName = map.types.keySet().iterator().next();

    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), C.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName, Map.of("c", objectName)));
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
    assertEquals(map.types.size(), 2);

    Map<String, String> typeToName = map.types.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));
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
        Map.of("d", objectName[0], "e", objectName[1])
                 )
    );
    assertEquals(map.singleRefObjects, emptySet());
  }

  /**
   * Checks special object mapping. Specifically that:
   * <ul>
   * <li><b>Mapper.toMap(null)</b> will produce an empty map.</li>
   * <li><b>Mapper.toMap(f)</b> (an enum) will produce: 'f[F]: {f: a}'</li>
   * <li><b>Mapper.toMap(42)</b> will produce: 'a[int]: {int: 42}'</li>
   * <li><b>Mapper.toMap(h)</b> will produce: 'a[int[3]]: {int: [1, 2, 3]}'</li>
   * <li><b>Mapper.toMap(i)</b> will produce: 'a[java.lang.String[2][]]: {string: [[a], [c, d]]}'</li>
   * </ul>
   */
  @Test
  public void mapSpecial() throws Exception {
    // nulls map to the empty map
    Mapped map = Mapper.toMap(null);
    assertEquals(map.types, emptyMap());
    assertEquals(map.objects, emptyMap());
    assertEquals(map.singleRefObjects, emptySet());

    // enum maps to their names
    map = Mapper.toMap(f);
    String objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), F.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("f", "a")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    // And so does other literals
    map = Mapper.toMap(42);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), Integer.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("integer", "42")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    // Arrays are also literals
    map = Mapper.toMap(h);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), int.class.getName() + "[3]");
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("int", "[1,2,3]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));

    map = Mapper.toMap(i);
    objectName = map.types.keySet().iterator().next();
    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), String.class.getName() + "[2][]");
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects, Map.of(objectName, Map.of("string", "[[a],[c,d]]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));
  }

  /**
   * Checks that <b>Mapper.toMap(g)</b> produces:
   * <pre>
   *    g[G]: {a: b, b: [a, a, b, c]}
   * </pre>
   */
  @Test
  public void mapG() throws Exception {
    Mapped map = Mapper.toMap(g);
    String objectName = map.types.keySet().iterator().next();

    assertEquals(map.types.size(), 1);
    assertEquals(map.types.get(objectName), G.class.getName());
    assertEquals(map.objects.size(), 1);
    assertEquals(map.objects,
                 Map.of(objectName,
                        Map.of("a", "b", "b", "[a,a,b,c]")));
    assertEquals(map.singleRefObjects, Set.of(objectName));
  }

  @Test
  public void mapJ() throws Exception {
    Mapped map = Mapper.toMap(j);
    assertEquals(map.types.size(), 4);

    Map<String, String> typeToName = map.types.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));
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
    assertEquals(map.objects,
                 Map.of(
                     objectName[0],
                     Map.of("i0", objectName[1], "i1", objectName[2]),
                     objectName[1],
                     Map.of("i0", objectName[3]),
                     objectName[2],
                     Map.of("i0", objectName[3], "i1", objectName[3]),
                     objectName[3],
                     Map.of("a", "b", "b", "[a,a,b,c]")
                 )
    );
    assertEquals(map.singleRefObjects, Set.of(objectName[0], objectName[1], objectName[2]));
  }

  @Test
  public void mapK() throws Exception {
    Mapped map = Mapper.toMap(k);
    assertEquals(map.types.size(), 3);

    Map<String, String> typeToName = map.types.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));
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

  @Test
  public void mapDemapA() throws Exception {
    Mapped map = Mapper.toMap(a);
    assertEquals(Mapper.fromMap(map), a);
  }

  @Test
  public void mapDemapB() throws Exception {
    Mapped map = Mapper.toMap(b);
    assertEquals(Mapper.fromMap(map), b);
  }

  @Test
  public void mapDemapC() throws Exception {
    Mapped map = Mapper.toMap(c);
    C c_ = (C)Mapper.fromMap(map);
    assertSame(c_, c_.c);
  }

  @Test
  public void mapDemapD() throws Exception {
    Mapped map = Mapper.toMap(d);
    D d_ = (D)Mapper.fromMap(map);
    assertSame(d_, d_.e.d);
    assertSame(d_.e, d_.e.e);
  }

  @Test
  public void mapDemapF() throws Exception {
    Mapped map = Mapper.toMap(f);
    assertEquals(Mapper.fromMap(map), f);
  }

  @Test
  public void mapDemapG() throws Exception {
    Mapped map = Mapper.toMap(g);
    assertEquals(Mapper.fromMap(map), g);
  }

  @Test
  public void mapDemapK() throws Exception {
    Mapped map = Mapper.toMap(k);
    assertEquals(Mapper.fromMap(map), k);
  }

  @Test
  public void mapDemapSpecial() throws Exception {
    // nulls
    Mapped map = Mapper.toMap(null);
    assertSame(Mapper.fromMap(map), null);

    // other literals
    map = Mapper.toMap(42);
    assertEquals(Mapper.fromMap(map), 42);

    // Arrays are also literals
    map = Mapper.toMap(h);
    assertArrayEquals((int[])Mapper.fromMap(map), h);

    map = Mapper.toMap(i);
    assertArrayEquals((String[][])Mapper.fromMap(map), i);
  }


  @Test
  public void mapDemapList() throws Exception {
    List<String> obj = Arrays.asList("a", "b", "c", "d");
    Mapped map = Mapper.toMap(obj);
    assertEquals(Mapper.fromMap(map), obj);

    List<T2<String, Integer>> listOfObjects = Arrays.asList(
        T2.of("a", 1), T2.of("b", 2),
        T2.of("c", 3), T2.of("d", 4));
    map = Mapper.toMap(listOfObjects);
    Object reconstructed = Mapper.fromMap(map);
    assertEquals(reconstructed, listOfObjects);
  }

  @Test
  public void mapDemapMap() throws Exception {
    Map<String, Integer> obj1 = Map.of(
        "a", 1, "b", 2,
        "c", 3, "d", 4);

    Mapped map = Mapper.toMap(obj1);
    Object reconstructed = Mapper.fromMap(map);
    assertEquals(reconstructed, obj1);

    Date now = new Date(System.currentTimeMillis());
    Map<T2<String, Integer>, Date> obj2 = Map.of(
        T2.of("a", 1), now, T2.of("b", 2), now,
        T2.of("c", 3), now, T2.of("d", 4), now);
    map = Mapper.toMap(obj2);
    reconstructed = Mapper.fromMap(map);
    assertEquals(reconstructed, obj2);
  }

  @Test
  public void mapPerf() throws Exception {
    Date now = new Date(System.currentTimeMillis());
    Map<T3<String, Integer, Date>, K> obj = Maps.of(
        T2.of(T3.of("a", 1, now), k), T2.of(T3.of("b", 2, now), k),
        T2.of(T3.of("c", 3, now), k), T2.of(T3.of("d", 4, now), k));

    Mapped map = Mapper.toMap(obj);
    Object reconstructed = Mapper.fromMap(map);
    assertEquals(reconstructed, obj);

    for (int i = 0; i < 10000; i++) {
      obj.put(T3.of("x", i, now), k);
    }

    // heat up
    int reps = 3;
    System.out.println("Heating up...");
    for (int i = 0; i < reps; i++) {
      map = Mapper.toMap(obj);
      Mapper.fromMap(map);
    }

    int a = 0;
    System.out.println("Computing performance to map...");
    long start = System.currentTimeMillis();
    for (int i = 0; i < reps; i++) {
      map = Mapper.toMap(obj);
      a |= System.identityHashCode(map.objects);
    }
    System.out.println(a);
    System.out.println("Time taken to map: " + ((System.currentTimeMillis() - start) / reps));

    map = Mapper.toMap(obj);
    System.out.println("Number of mapped objects: " + map.types.size());

    System.out.println("Computing performance to reconstruct...");
    start = System.currentTimeMillis();
    for (int i = 0; i < reps; i++) {
      reconstructed = Mapper.fromMap(map);
      a |= System.identityHashCode(reconstructed);
    }
    System.out.println(a);
    System.out.println("Time taken to reconstruct: " + ((System.currentTimeMillis() - start) / reps));
  }
}