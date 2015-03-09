package com.cloudera.datascience.adamkite;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.bdgenomics.formats.avro.KeyedGenotype;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.RefinableView;

/**
 * Reads records whose variant position is in a specified range.
 */
public class ReadKeyedGenotypeDataset extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: " + getClass().getSimpleName() +
          " dataset_uri [chromosome] [start] [end]");
      System.exit(1);
    }
    String uri = args[0];
    String chromosome = null;
    Long start = null;
    Long end = null;
    if (args.length > 1) {
      chromosome = args[1];
    }
    if (args.length > 2) {
      start = Long.parseLong(args[2]);
    }
    if (args.length > 3) {
      end = Long.parseLong(args[3]);
    }
    RefinableView<KeyedGenotype> view = Datasets.load(uri, KeyedGenotype.class);
    if (chromosome != null) {
      view = view.with("chromosome", chromosome);
    }
    if (start != null) {
      view = view.from("position", start);
    }
    if (end != null) {
      view = view.to("position", end);
    }
    DatasetReader<KeyedGenotype> reader = null;
    try {
      reader = view.newReader();
      for (KeyedGenotype genotype : reader) {
        System.out.println(genotype);
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return 0;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new ReadKeyedGenotypeDataset(), args);
    System.exit(rc);
  }
}
