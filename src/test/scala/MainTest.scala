package nl.schiphol

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec

class MainSpec extends AnyFlatSpec {

  def fixture = new {
    val spark = SparkSession.builder.master("local[*]").getOrCreate()
  }

  def createFlight(airline: String, source_airport: String, destination_airport: String): Flight = {
    Flight(airline = airline, airline_id = "X", source_airport = source_airport, source_airport_id = "X", destination_airport = destination_airport, destination_airport_id = "X", codeshare = null, stops = 0, equipment = "X", timestamp = null)
  }

  "Task 1 query" should "group airports" in {
    val data: List[Flight] = List(
      createFlight("A", "ABC", "DEF"),
      createFlight("A", "ABC", "DEF"),
      createFlight("A", "DEF", "DEF"),
      createFlight("A", "XYZ", "DEF"),
    )

    val df = fixture.spark.createDataFrame(data)
    val result = Main.getTask1Query(df).collect()

    // test that the ABC source is the number 1 result
    assert(result(0).getString(0) === "ABC")
    assert(result(0).getLong(1) === 2)
    assert(result.length === 3)

    // test that the rest is sorted alphabetical
    assert(result(1).getString(0) === "DEF")
    assert(result(1).getLong(1) === 1)

    assert(result(2).getString(0) === "XYZ")
    assert(result(2).getLong(1) === 1)
  }

  "Task 1 query" should "return an empty list with no input" in {
    val data: List[Flight] = List()

    val df = fixture.spark.createDataFrame(data)
    val result = Main.getTask1Query(df).collect()

    assert(result.length === 0)
  }
}
