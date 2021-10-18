package nl.schiphol

import org.apache.spark.SparkFiles
import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.functions.{col, window}
import org.apache.spark.sql.{DataFrame, Encoders, SparkSession}

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}


object Main {
  def getRoutesDf(spark: SparkSession): DataFrame = {
    spark.sparkContext.addFile("https://raw.githubusercontent.com/jpatokal/openflights/master/data/routes.dat")
    spark.read.format("csv")
      .schema(Encoders.product[Flight].schema)
      .load("file://" + SparkFiles.get("routes.dat"))
  }

  def getSession(): SparkSession = {
    val spark = SparkSession.builder.master("local[*]").appName("Airport Analyzer").getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    spark
  }

  def task1(outputFilename: String): Unit = {
    val spark = getSession()
    var df = getRoutesDf(spark)

    df = getTask1Query(df)
    df.show()
    df.write
      .option("header", "true")
      .option("overwrite", "true")
      .csv(outputFilename)
  }

  def getTask1Query(df: DataFrame): DataFrame = {
    // similar to `select source_airport, count(*) as total from airports order by total desc limit 10`
    df.groupBy("source_airport")
      .count()
      .withColumnRenamed("count", "total_usage")
      .orderBy(col("total_usage").desc, col("source_airport").asc)
      .repartition(1)
      .limit(10)
  }

  def task2(): Unit = {
    val spark = getSession()
    getRoutesDf(spark)

    import spark.implicits._

    val df = spark.readStream
      .format("csv")
      .schema(Encoders.product[Flight].schema)
      .load("file://" + SparkFiles.getRootDirectory())
      .groupBy("source_airport")
      .count()
      .withColumnRenamed("count", "total_usage")
      .orderBy($"total_usage".desc)
      .repartition(1)
      .limit(10)

    val query = df.writeStream
      .outputMode("complete")
      .format("console")
      .start()

    query.awaitTermination()
  }

  def task3(): Unit = {
    val spark = getSession()
    val inputDf = getRoutesDf(spark)

    import spark.implicits._

    // create a memorystream of data, where we can put Flight objects in
    implicit val ctx = spark.sqlContext
    val stream = MemoryStream[Flight]
    val streamingData = stream.toDS()

    // repartition to 1 for acceptable performance on a single machine
    val df = streamingData
      .repartition(1)
      .groupBy(window($"timestamp", "5 second", "5 second"), $"source_airport")
      .count()
      .withColumn("ts", $"window.start")
      .withColumnRenamed("count", "total_usage")
      .select($"ts", $"source_airport", $"total_usage")
      .orderBy($"ts".desc, $"total_usage".desc)
      .limit(10)

    val query = df.writeStream
      .outputMode("complete")
      .format("console")
      .start()

    // feed some data to the stream, approximately 1000 records per second
    inputDf.as[Flight].collect().foreach({ flight => {
      flight.timestamp = Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
      stream.addData(flight)
      Thread.sleep(1)
    }
    })

    query.awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    args match {
      case Array("task1") => Main.task1("/tmp/output")
      case Array("task2") => task2()
      case Array("task3") => task3()
      case _ => println("ERROR: Invalid parameters provided")
    }

    ()
  }
}

