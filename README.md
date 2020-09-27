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
    
