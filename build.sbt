import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.releaseProcess
import ReleaseTransformations._

lazy val root = (project in file("."))
  .enablePlugins(AshScriptPlugin, DockerPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.github.everpeace",
      scalaVersion := "2.11.8"
    )),
    name := "throttlable-perf-consumer",
    libraryDependencies ++= Seq(
      reactiveKafka,
      kafka,
      embeddedKafka % Test,
      scalaTest % Test,
      akkaTestkit % Test
    ),
    maintainer in Docker := "Shingo Omura <https://github.com/everpeace>",
    dockerRepository := Some("everpeace"),
    mainClass in Compile := Some("com.github.everpeace.reactive_kafka.ThrottolableConsumer"),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      runClean,
      ReleaseStep(releaseStepTask(publish in Docker)),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )