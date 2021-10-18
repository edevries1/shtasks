package nl.schiphol

import java.sql.Timestamp

case class Flight(
                   airline: String,
                   airline_id: String,
                   source_airport: String,
                   source_airport_id: String,
                   destination_airport: String,
                   destination_airport_id: String,
                   codeshare: Option[String],
                   stops: Integer,
                   equipment: String,
                   var timestamp: Timestamp
                 )