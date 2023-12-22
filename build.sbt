val scala3Version = "3.3.1"
val http4sVersion = "0.23.24"
val circeVersion = "0.14.1"
lazy val root = project
  .in(file("."))
  .settings(
    name := "BilgeAdam",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.3.0",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion
    ),
    libraryDependencies ++= Seq(
      "org.gnieh" %% "fs2-data-csv-generic" % "1.10.0",
      "org.gnieh" %% "fs2-data-json-circe" % "1.10.0"
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    Compile / run / fork := true
  )
