package com.cloudera.datascience.adamkite.variantstore;

import java.io.IOException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.bdgenomics.formats.avro.Genotype;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.hbase.avro.AvroUtils;
import org.kitesdk.data.mapreduce.DatasetKeyInputFormat;

/**
 * Imports genotype data into HBase from a Kite dataset.
 */
public class HBaseImporter extends Configured implements Tool {

  public static final byte[] GENOTYPE_COLUMN_FAMILY = Bytes.toBytes("g");

  static class HBaseGenotypeMapper<K> extends Mapper<Genotype, Void, K, Put> {
    @Override
    protected void map(Genotype genotype, Void v,
        Context context)
        throws IOException, InterruptedException {
      Put put = new Put(RowKeyBuilder.makeRowKey(genotype));
      byte[] columnQualifier = Bytes.toBytes(genotype.getSampleId());
      byte[] value = serialize(genotype);
      put.add(GENOTYPE_COLUMN_FAMILY, columnQualifier, value);
      context.write(null, put);
    }
  }

  static byte[] serialize(Genotype genotype) {
    DatumWriter<Genotype> datumWriter = new
        SpecificDatumWriter<Genotype>(Genotype.class);
    return AvroUtils.writeAvroEntity(genotype, datumWriter);
  }

  @Override
  public int run(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: " + getClass().getSimpleName() + " dataset_uri");
      System.exit(1);
    }
    String uri = args[0];
    View<Genotype> inputDataset = Datasets.load(uri, Genotype.class);
    Job job = new Job(getConf(), getClass().getSimpleName());
    job.setJarByClass(getClass());
    DatasetKeyInputFormat.configure(job).readFrom(inputDataset);
    job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, "genotypes");
    job.setMapperClass(HBaseGenotypeMapper.class);
    job.setNumReduceTasks(0);
    job.setOutputFormatClass(TableOutputFormat.class);
    return job.waitForCompletion(true) ? 0 : 1;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new HBaseImporter(), args);
    System.exit(rc);
  }
}
