# Long Term Readable Serialization
A library for serializing (and deserializing) Java object graphs to various text formats 
such as XML, JSON and YAML, useful for long-term storage with a focus on human readability
and external modification.

Serialization goes through 2 distinct steps with the object-graph first transformed into a 
representation-independent structure (Mapped) which is then transformed to any target representation.
Currently  XML, JSON and YAML are supported but serialization to any format can be
added with little effort.

## Usage

`Mapper` is a class with two primary methods: `toMap` takes the root of an object graph and
and produces a `Mapped` object which is a simplified representation-independent structure of
the object-graph. `Mapped` can then be transformed into a target representation using an
appropriate implementation of `Serializer` such as `JsonSerializer` or `YamlSerializer`:

               Mapper.toMap                 Serializer.toText
    Object  ----------------->  Mapped    --------------------->  Serialized
    graph   <----------------- structure  <--------------------- representation
              Mapper.fromMap                Serializer.toMap
    
Simply call `Mapper.toMap` on an object to get the `Mapped` instance representation of the object
graph of which the object is part of. Then use one of the serializer builders such as the `JsonSerializerBuilder`
to configure and get an instance of a serializer. E.g:

     JsonSerializer json =
        JsonSerializerBuilder.newBuilder()
              .indentSpaces(4)                // instead of 2
              .lineSeparator("\n")            // instead of the system's default
              .inlineSingleRefObjects(false)  // do not inline singly-referenced objects
              .build();
 
 Or simply: `JsonSerializer json = JsonSerializerBuilder.newBuilder().build()` to use defaults.
 
 Call the `toText` method of the serializer passing the `Mapped` instance to get the serialized
 representation of the object graph: `String ser = json.toText(mapped)`
 
 Call the `toMap` method of the serializer passing in the serialized text to get back the `Mapped`
 instance which can then be passed to the `fromMap` method of `Mapper` to get back the object
 graph.