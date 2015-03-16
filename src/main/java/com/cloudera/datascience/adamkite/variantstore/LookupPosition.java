package com.cloudera.datascience.adamkite.variantstore;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellScanner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.bdgenomics.formats.avro.Genotype;
import org.kitesdk.data.hbase.avro.AvroUtils;

public class LookupPosition extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: " + getClass().getSimpleName() + " chrom position");
      System.exit(1);
    }

    HTable table = new HTable(HBaseConfiguration.create(getConf()), "genotypes");

    String chromosome = args[0];
    long position = Long.parseLong(args[1]);
    Get get = new Get(RowKeyBuilder.makeRowKey(chromosome, position));
    get.addFamily(HBaseImporter.GENOTYPE_COLUMN_FAMILY);
    Result res = table.get(get);
    if (res == null) {
      System.out.println("No rows found");
      return 0;
    }
    CellScanner cellScanner = res.cellScanner();
    while (cellScanner.advance()) {
      Cell cell = cellScanner.current();
      byte[] value = cell.getValueArray();
      System.out.println(deserialize(value, cell.getValueOffset(), cell.getValueLength()));
    }
    return 0;
  }

  static Genotype deserialize(byte[] bytes, int offset, int length) {
    SpecificDatumReader<Genotype> datumReader = new
        SpecificDatumReader<>(Genotype.class);
    BinaryDecoder decoder = new DecoderFactory().binaryDecoder(bytes, offset, length, null);
    return AvroUtils.readAvroEntity(decoder, datumReader);
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new LookupPosition(), args);
    System.exit(rc);
  }

}
