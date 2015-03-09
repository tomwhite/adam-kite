# Using Kite with ADAM

The idea here is to use the ADAM schemas to define Kite datasets so we can use Kite tools
and APIs to work with ADAM data.

## Prereqs

Kite has a [limitation](https://issues.cloudera.org/browse/CDK-956) that means we can't
have nullable top-level fields in schemas
when we're using HBase storage. As a workaround, build a local version of Kite from this
branch: https://github.com/tomwhite/kite/tree/CDK-956-nullable-fields-in-hbase

You'll also need a local HBase install. The QuickStart VM is the simplest way to achieve
that. See these set up instructions:
https://github.com/kite-sdk/kite-examples#getting-started

## Building

```bash
# Set up the environment for tools
export ADAM_HOME=~/sw/adam-distribution-0.16.0
export SPARK_HOME=~/sw/spark-1.2.0-bin-hadoop2.3/
export PATH=$PATH:$ADAM_HOME/bin 
export PATH=$PATH:~/sw/kite-tools-cdh5-1.0.1-SNAPSHOT/bin

# Convert the ADAM Avro IDL type to individual JSON schema files
avro idl2schemata bdg.avdl src/main/avro

# Build
mvn package

# Remove old datasets
kite-dataset delete dataset:file:data/genotypes
kite-dataset delete dataset:hbase:localhost/genotype.KeyedGenotype
```

## Create a filesystem-backed Kite dataset

This will create a dataset in the local directory, _data/genotypes_. Note that this uses
ADAM and Kite tools; no custom code is needed.

```bash
# Create the genotypes (VCF) dataset in Parquet format
kite-dataset create dataset:file:data/genotypes --schema src/main/avro/Genotype.avsc \
  --format parquet

# Convert a VCF file to ADAM
adam-submit vcf2adam data-raw/small.vcf small.adam -parquet_compression_codec UNCOMPRESSED
cp small.adam/*.parquet data/genotypes
rm -rf small.adam

# Read the dataset
kite-dataset show dataset:file:data/genotypes
```

## Create an HBase-backed Kite dataset

To store genotype objects in HBase we need to choose the row key carefully. We'll use a
key composed of the chromosome and position (this is the simplest key, more fields may
be added later).

Key fields must be the first fields in the schema, and may not be nested
(i.e. they may not be nested in a record). To meet this requirement we create a
slightly modified Genotype schema, called KeyedGenotype. (See
src/main/avro/KeyedGenotype.avsc.) This schema specifies the HBase column mappings.

To populate the dataset we transform the file-based dataset using a simple class that
extracts the key from a Genotype and creates an equivalent KeyedGenotype. (See
src/main/java/com/cloudera/datascience/adamkite/AddKeyToGenotype.java.)

To demonstrate reading the dataset from HBase, we use the Kite Dataset API to read a
portion of the dataset between a range of chromosome positions. (See
src/main/java/com/cloudera/datascience/adamkite/ReadKeyedGenotypeDataset.java.)

```bash
# Create an HBase-backed genotypes dataset using a schema tuned for HBase storage
kite-dataset create dataset:hbase:localhost/genotype.KeyedGenotype \
  --schema src/main/avro/KeyedGenotype.avsc

# Transform the HDFS-backed genotypes dataset to the HBase-backed one
kite-dataset transform dataset:file:data/genotypes \
  dataset:hbase:localhost/genotype.KeyedGenotype --jar target/adam-kite-*.jar \
  --transform com.cloudera.datascience.adamkite.AddKeyToGenotype \
  --no-compaction

# Read the whole dataset
mvn exec:java -Dexec.mainClass="com.cloudera.datascience.adamkite.ReadKeyedGenotypeDataset" \
  -Dexec.args="dataset:hbase:quickstart.cloudera/genotype.KeyedGenotype"

# .. or just a range of chromosome 1
mvn exec:java -Dexec.mainClass="com.cloudera.datascience.adamkite.ReadKeyedGenotypeDataset" \
  -Dexec.args="dataset:hbase:quickstart.cloudera/genotype.KeyedGenotype 1 15000 65000"
```