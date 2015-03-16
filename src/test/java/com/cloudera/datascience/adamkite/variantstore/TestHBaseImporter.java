package com.cloudera.datascience.adamkite.variantstore;

import java.io.IOException;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kitesdk.data.spi.DefaultConfiguration;
import org.kitesdk.minicluster.HBaseService;
import org.kitesdk.minicluster.HdfsService;
import org.kitesdk.minicluster.MiniCluster;
import org.kitesdk.minicluster.ZookeeperService;

import static org.junit.Assert.assertEquals;

public class TestHBaseImporter {

  private MiniCluster miniCluster;

  @Before
  public void setUp() throws Exception {
    String workDir = "target/kite-minicluster-workdir-" + UUID.randomUUID();
    miniCluster = new MiniCluster.Builder().workDir(workDir)
        .bindIP("127.0.0.1")
        .addService(HdfsService.class)
        .addService(ZookeeperService.class)
        .addService(HBaseService.class)
        .clean(true).build();
    miniCluster.start();
  }

  @After
  public void tearDown() throws Exception {
    miniCluster.stop();
  }

  public void createTable(String table, String columnFamily) throws Exception {
    Configuration config = HBaseConfiguration.create(DefaultConfiguration.get());
    // Create table
    HBaseAdmin admin = new HBaseAdmin(config);
    try {
      TableName tableName = TableName.valueOf(table);
      HTableDescriptor htd = new HTableDescriptor(tableName);
      HColumnDescriptor hcd = new HColumnDescriptor(columnFamily);
      htd.addFamily(hcd);
      admin.createTable(htd);
      HTableDescriptor[] tables = admin.listTables();
      if (tables.length != 1 &&
          Bytes.equals(tableName.getName(), tables[0].getTableName().getName())) {
        throw new IOException("Failed create of table");
      }
    } finally {
      admin.close();
    }
  }

  @Test
  public void testImport() throws Exception {
    createTable("genotypes", "g");

    HBaseImporter importer = new HBaseImporter();
    importer.setConf(DefaultConfiguration.get());
    int rc = importer.run(new String[]{"dataset:file:data/genotypes"});
    assertEquals(0, rc);

    LookupPosition lookupPosition = new LookupPosition();
    lookupPosition.setConf(DefaultConfiguration.get());
    int rc2 = lookupPosition.run(new String[]{"1", "14396"});
    assertEquals(0, rc2);

  }
}
