val scala3Version = "3.3.1"
val http4sVersion = "0.23.24"
val fs2DataVersion = "1.10.0"
val circeVersion = "0.14.1"

val http4sDeps = Seq("ember-client", "ember-server", "dsl", "circe")
  .map(s => "org.http4s" %% s"http4s-$s" % http4sVersion)
val fs2DataDeps = Seq("csv-generic", "json-circe")
  .map(s => "org.gnieh" %% s"fs2-data-$s" % fs2DataVersion)
val circeDeps = Seq("core", "generic", "parser")
  .map(s => "io.circe" %% s"circe-$s" % circeVersion)

lazy val root = project
  .in(file("."))
  .settings(
    name := "BilgeAdam",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "ch.qos.logback" % "logback-classic" % "1.3.0"
    ) ++ http4sDeps ++ fs2DataDeps ++ circeDeps,
    Compile / run / fork := true
  )
