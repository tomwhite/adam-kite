package com.cloudera.datascience.adamkite.variantstore;

import org.apache.hadoop.hbase.util.Bytes;
import org.bdgenomics.formats.avro.Genotype;

public class RowKeyBuilder {
  private static final int CHROMOSOME_LENGTH = Bytes.SIZEOF_BYTE;
  private static final int POSITION_LENGTH = Bytes.SIZEOF_LONG;

  static byte[] makeRowKey(Genotype genotype) {
    return makeRowKey(getChromosome(genotype), genotype.getVariant().getStart());
  }

  static byte[] makeRowKey(String chromosome, long position) {
    return makeRowKey(getChromosome(chromosome), position);
  }

  static byte[] makeRowKey(byte chromosome, long position) {
    byte[] row = new byte[CHROMOSOME_LENGTH + POSITION_LENGTH];
    Bytes.putByte(row, 0, chromosome);
    Bytes.putLong(row, CHROMOSOME_LENGTH, position);
    return row;
  }

  static byte getChromosome(Genotype genotype) {
    String contigName = genotype.getVariant().getContig().getContigName();
    return getChromosome(contigName);
  }

  static byte getChromosome(String chromosome) {
    // TODO: remove any "chromosome" prefix
    return Byte.parseByte(chromosome);
  }

}
