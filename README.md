<div align="right">
    <img src="https://raw.githubusercontent.com/GuinsooLab/glab/main/src/images/guinsoolab-badge.png" height="60" alt="badge">
    <br />
</div>
<div align="center">
    <img src="https://raw.githubusercontent.com/GuinsooLab/glab/main/src/images/guinsoolab-hurricanedb.svg" alt="logo" height="100" />
    <br />
    <br />
</div>

# HurricaneDB

HurricaneDB is a real-time distributed OLAP datastore, built to deliver scalable real-time analytics with low latency. It can ingest from batch data sources (such as Hadoop HDFS, Amazon S3, Azure ADLS, Google Cloud Storage) as well as stream data sources (such as Apache Kafka).

HurricaneDB was built by engineers at LinkedIn and Uber and is designed to scale up and out with no upper bound. Performance always remains constant based on the size of your cluster and an expected query per second (QPS) threshold.

For getting started guides, deployment recipes, tutorials, and more, please visit our project documentation at [HurricaneDB](https://ciusji.gitbook.io/guinsoolab/products/query-engine/hurricanedb).

## Features

HurricaneDB was originally built at LinkedIn to power rich interactive real-time analytic applications such as [Who Viewed Profile](https://www.linkedin.com/me/profile-views/urn:li:wvmp:summary/),  [Company Analytics](https://www.linkedin.com/company/linkedin/insights/),  [Talent Insights](https://business.linkedin.com/talent-solutions/talent-insights), and many more. [UberEats Restaurant Manager](https://eng.uber.com/restaurant-manager/) is another example of a customer facing Analytics App. At LinkedIn, HurricaneDB powers 50+ user-facing products, ingesting millions of events per second and serving 100k+ queries per second at millisecond latency.

* **Column-oriented**: a column-oriented database with various compression schemes such as Run Length, Fixed Bit Length.

* [**Pluggable indexing**](https://ciusji.gitbook.io/guinsoolab/products/query-engine/hurricanedb/indexing): pluggable indexing technologies Sorted Index, Bitmap Index, Inverted Index.

* **Query optimization**: ability to optimize query/execution plan based on query and segment metadata.

* **Stream and batch ingest**: near real time ingestion from streams and batch ingestion from Hadoop.

* **Query with SQL:** SQL-like language that supports selection, aggregation, filtering, group by, order by, distinct queries on data.

* **Upsert during real-time ingestion**: update the data at-scale with consistency

* **Multi-valued fields:** support for multi-valued fields, allowing you to query fields as comma separated values.

## When should I use HurricaneDB?

HurricaneDB is designed to execute real-time OLAP queries with low latency on massive amounts of data and events. In addition to real-time stream ingestion, HurricaneDB also supports batch use cases with the same low latency guarantees. It is suited in contexts where fast analytics, such as aggregations, are needed on immutable data, possibly, with real-time data ingestion. HurricaneDB works very well for querying time series data with lots of dimensions and metrics.

Example query:
```SQL
SELECT sum(clicks), sum(impressions) FROM AdAnalyticsTable
  WHERE
       ((daysSinceEpoch >= 17849 AND daysSinceEpoch <= 17856)) AND
       accountId IN (123456789)
  GROUP BY
       daysSinceEpoch TOP 100
```

HurricaneDB is not a replacement for database i.e it cannot be used as source of truth store, cannot mutate data. While HurricaneDB supports text search, it's not a replacement for a search engine. Also, HurricaneDB queries cannot span across multiple tables by default. You can use the Trino-HurricaneDB Connector to achieve table joins and other features.

## Building HurricaneDB

More detailed instructions can be found at [Quick Demo](https://ciusji.gitbook.io/guinsoolab/products/query-engine/hurricanedb/quickstart) section in the documentation.
```
# Clone a repo
$ git clone git@github.com:GuinsooLab/hurricanedb.git
$ cd hurricandb

# Build HurricaneDB
$ mvn clean install -DskipTests -Pbin-dist

# Run the Quick Demo
$ cd build/
$ bin/quick-start-batch.sh
```

## Join the Community

- Ask questions on [GuinsooLab HurricaneDB](https://github.com/GuinsooLab/hurricanedb/issues)
- HurricaneDB Meetup Group: https://ciusji.gitbook.io/guinsoolab/products/query-engine/hurricanedb

## License

HurricaneDB is under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
