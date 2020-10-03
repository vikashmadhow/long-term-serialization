/*
 * Copyright (c) 2016 Vikash Madhow
 */

package ma.vi.serializer;

import java.util.*;

/**
 * This is the representation-independent structure of an object-graph
 * produced by the {@link Mapper} and contains the types and objects of
 * the mapped object graph.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class Mapped {
  /**
   * The mapped object-graph consisting of a map between object names
   * and their map representation. The object map representation is another
   * map of string to string mapping each field name of the object to the
   * text representation of its value.
   */
  public final Map<String, Map<String, String>> objects;

  /**
   * The type, in string form, of each object name in the {link #objects} map.
   */
  public final Map<String, String> types;

  /**
   * A set of objects having a single reference to the, in the object map.
   * Such objects are guaranteed to not be part of a cycle in the graph and
   * can be inlined in the serialized representation of the graph. This is
   * not strictly part of the mapped definition of the object graph; it is
   * instead an additional piece of information that could be useful in
   * subsequent processing stages of the object graph.
   */
  public final Set<String> singleRefObjects;

  public Mapped() {
    this(new LinkedHashMap<>(), new HashMap<>(), new HashSet<>());
  }

  /**
   * Internal constructor.
   */
  Mapped(Map<String, Map<String, String>> objects,
         Map<String, String> types,
         Set<String> singleRefObjects) {
    this.types = types;
    this.objects = objects;
    this.singleRefObjects = singleRefObjects;
  }

  /**
   * Internal factory method.
   */
  static Mapped of(Map<String, Map<String, String>> objects,
                   Map<String, String> types,
                   Set<String> singleRefObjects) {
    return new Mapped(objects, types, singleRefObjects);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Mapped mapped = (Mapped) o;

    if (!Objects.equals(objects, mapped.objects)) return false;
    if (!Objects.equals(types, mapped.types)) return false;
    return Objects.equals(singleRefObjects, mapped.singleRefObjects);
  }

  @Override
  public int hashCode() {
    int result = objects != null ? objects.hashCode() : 0;
    result = 31 * result + (types != null ? types.hashCode() : 0);
    result = 31 * result + (singleRefObjects != null ? singleRefObjects.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Mapped{" +
        "objects=" + objects +
        ", types=" + types +
        ", singleRefObjects=" + singleRefObjects +
        '}';
  }
}
