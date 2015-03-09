package com.cloudera.datascience.adamkite;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.bdgenomics.formats.avro.Genotype;
import org.bdgenomics.formats.avro.KeyedGenotype;

public class AddKeyToGenotype extends DoFn<GenericRecord, GenericRecord> {
  @Override
  public void process(GenericRecord genotype, Emitter<GenericRecord> emitter) {
    Object variant = genotype.get("variant");
    if (variant == null) {
      return;
    }
    Object contig = ((GenericRecord) variant).get("contig");
    if (contig == null) {
      return;
    }
    Object contigName = ((GenericRecord) contig).get("contigName");
    if (contigName == null) {
      return;
    }
    Object start = ((GenericRecord) variant).get("start");
    if (start == null) {
      return;
    }
    GenericRecord keyedGenotype = new GenericData.Record(KeyedGenotype.SCHEMA$);
    keyedGenotype.put("chromosome", contigName);
    keyedGenotype.put("position", start);
    for (Schema.Field field : Genotype.SCHEMA$.getFields()) {
      keyedGenotype.put(field.name(), genotype.get(field.name()));
    }
    emitter.emit(keyedGenotype);
  }
}
