# Long Term Readable Serialization
A library for serializing (and deserializing) Java object graphs to various text formats 
such as XML, JSON and YAML useful for long-term storage with focus on human readability.

Serialization goes through 2 distinct steps with the object-graph first transformed into a 
representation-independent structure (Mapped) which is then transformed to any target representation.
Currently  XML, JSON and YAML are supported but serialization to any format can be
easily added to the library.

## Usage

              Mapper.toMap                  Serializer.toText
    Object  ----------------->  Mapped    --------------------->  Serialized
    graph   <----------------- structure  <--------------------- representation
              Mapper.fromMap                Serializer.toMap
    

