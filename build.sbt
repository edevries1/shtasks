name := "assignment"

version := "0.1"

scalaVersion := "2.13.6"

idePackagePrefix := Some("nl.schiphol")

libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "test"

mainClass in Compile := Some("nl.schiphol.Main")

packageName in Docker := "shtasks"
dockerExposedPorts += 4040
dockerRepository := Some("ghcr.io")
dockerUsername := Some("edevries1")
dockerUpdateLatest := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)