import sbtassembly.AssemblyPlugin.assemblySettings


scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8",
  fork in run := true,
  parallelExecution in ThisBuild := false,
  parallelExecution in Test := false
)

lazy val versions = new {
  val logback = "1.0.13"
  val jodaTime = "2.9.4"
  val jodaConvert = "1.8"
  val akkaActor = "2.4.12"
  val akkaBetterFiles = "2.16.0"
  val betterFiles = "2.16.0"
  val tests = new {
    val specs2 = "3.7"
    val scalaMock = "3.2.2"
    val scalaTest ="2.2.6"
    val mockito = "1.9.5"
    val akkaTestkit = "2.4.12"
  }
}

lazy val core = project.
  in(file("core"))
  .settings(commonSettings: _*)
  .settings(assemblySettings: _*)
  .settings(
    name := "http-log-monitor-core",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "joda-time" % "joda-time" % versions.jodaTime,
      "org.joda" % "joda-convert" % versions.jodaConvert,
      "com.typesafe.akka" %% "akka-actor" % versions.akkaActor,
      "com.typesafe.akka" %% "akka-testkit" % versions.tests.akkaTestkit,
      "org.mockito" % "mockito-core" % versions.tests.mockito % "test",
      "org.scalatest" %% "scalatest" % versions.tests.scalaTest % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % versions.tests.scalaMock % "test",
      "org.specs2" %% "specs2" % versions.tests.specs2 % "test"
    )
  )

lazy val monitor = project.
  in(file("monitor"))
  .settings(commonSettings: _*)
  .settings(assemblySettings: _*)
  .settings(
    name := "http-log-monitor-monitor",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % versions.betterFiles,
      "com.github.pathikrit"  %% "better-files-akka"  % versions.akkaBetterFiles
    )
  ).dependsOn(core)