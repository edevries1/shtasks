# Airport analyzer

# Building

The project can be built using a bash script, like this: 

```shell
# requires a github personal access token to work
$ ./build.sh
```

# Running

To run the project, either run it locally using one of these commands:

```shell
# valid tasks are task1, task2 and task3
$ sbt "run task1" 
# OR
$ ./run.sh task1 
```

## Task 1

Since I am familiar with the Spark batch API, this one was rather easy. Just load the file
and perform the group by source_airport and sort it by the count. 

Initially I had the file downloaded, but in the final solution decided to go for the `SparkFiles` solution
to not require the routes.dat file to be available locally. The only problem is that it does
not overwrite the output file, so running it twice throws an error.

### Schema 

At first I created this schema:

```scala
  val FLIGHT_SCHEMA = new StructType()
    .add("airline", StringType, false)
    .add("airline_id", StringType, false)
    .add("source_airport", StringType, false)
    .add("source_airport_id", StringType, false)
    .add("destination_airport", StringType, false)
    .add("destination_airport_id", StringType, false)
    .add("codeshare", StringType, true)
    .add("stops", IntegerType, false)
    .add("equipment", StringType, true)
```

After doing part 3 of the assignment, I found that the case class solution was better so I switched to that and refactored all the other tasks to also use that.

## Task 2

I have no actual experience with the streaming API but it did not look so different. I figured that 
simply loading all the data into the stream should work almost the same as the batch API, 
and it did. 

It writes the complete output of the current streamed data to the console. I did not experience
any problems here.


## Task 3

This is where it got tricky. First of all, having a streamed dataset without timestamps
did not make sense, so I assumed that I could just append a timestamp to each row and
treat it like timestamped data. 

So I started out doing that. Unfortunately, it already took me a while to figure out
how to get the data from the CSV file as an iterable object, because I was not using
the case class yet and my limited Scala experience was starting to show. In the end, 
I got it to do what I wanted it to do, namely send about 1000 records per second into the
datastream, like so:

```scala
inputDf.as[Flight].collect().foreach({ flight => {
      flight.timestamp = Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
      stream.addData(flight)
      Thread.sleep(1)
}})
```

Now the hard part came. The previous code from task 2 did not seem to work for this case.
Mostly I ran into the constraint I could not do an `orderBy` with the output mode `Update`
which was kind of what I wanted. There was always some limitation in Spark that prevented me from
having a quick solution.

The initial query looked like this:

```scala
    val df = streamingData
      .withColumn("timestamp", expr("cast(timestamp as timestamp)"))
      .groupBy(window($"timestamp", "5 second", "5 second"), $"source_airport")
      .count()
      .withColumnRenamed("count", "total_usage")
      .select($"window.start".as("window"), $"source_airport", $"total_usage")
      .orderBy($"window".asc, $"total_usage".desc)
      .limit(10)
      .writeStream
      .outputMode("complete")
      .format("console")
      .start()
```

This however would output ALL the windows, so I went on a long journey to have Spark do what I wanted it to do and output just the latest window. 
It took many attempts to figure out that this was actually not so far from the final solution.

I also tried to apply a row_number over another window where I applied sorting and partitioning,
but this made Spark not happy as it really wanted to have a timestamp for partitioning. The 
confusing part is that it was actually a timestamp, but Spark would not budge, so I had to move on.

Another attempt had the outputMode `update` write to a in-memory table (with `queryName`) and 
try to set up a second stream that reads from that and does the grouping by, but this quickly 
seemed to be the wrong way to go because of added complexity. I was still not able to believe
a seemingly simple task like this required this much complexity. 

I also tried something with `foreachBatch`, but it was so terribly slow that
I gave up on that too. This was a snippet of that code:

```scala
      .writeStream
      .outputMode("update")
      .foreachBatch({ (ds: Dataset[Row], batch: Long) => {
        println(s"Batch $batch")
        ds.orderBy($"window".desc, $"total_usage".desc).limit(10).show()
      }})
```

That code inside the `foreachBatch` was taking several minutes on my pretty fast machine.

So after a lot of trial and error, I got to the solution where I initially started 
out on, with just a single change: add `repartition(1)` to the query, so it works in acceptable runtimes.

In the end I selected a window of 5 seconds and a sliding interval of 5 seconds. When running the code,
it should finish in about a minute in a half with regards to the initial data size. 

## Task 4

I added some tests to test the first task

# Dockerizing it

I thought adding everything to a docker image would be a good idea. I started out with some
tool to build a fat .jar file, but that failed horribly due to version conflicts
in all kinds of packages. So I quickly tried something else, called `sbt-native-packager` which can
just generate the docker stuff for you. See `build.sh` for how to build this project.

I decided to put everything on the public github container registry so you can run it all without authentication configurations.

# Conclusion

Honestly, I am not happy with how much trouble I had implementing part 3 of the assignment. 
The time spent on part 3 was really too long (about 5 hours). However, I did learn a great deal
of information about the Spark Structured Streaming API.


 


