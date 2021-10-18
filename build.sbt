name := "assignment"

version := "0.1"

scalaVersion := "2.13.6"

idePackagePrefix := Some("nl.schiphol")

libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "test"

mainClass in Compile := Some("nl.schiphol.Main")

dockerExposedPorts += 4040

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)