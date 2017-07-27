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
    javaOptions in Universal ++= Seq(
      "-server",
      "-Dcom.sun.management.jmxremote.port=8999",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false"
    ),
    bashScriptExtraDefines ++= Seq(
      s"""addJava "-Xms${sys.env.getOrElse("JVM_HEAP_MIN", "${JVM_HEAP_MIN:-1024m}")}"""",
      s"""addJava "-Xmx${sys.env.getOrElse("JVM_HEAP_MAX", "${JVM_HEAP_MAX:-1024m}")}"""",
      s"""addJava "-XX:MaxMetaspaceSize=${sys.env.getOrElse("JVM_META_MAX", "${JVM_META_MAX:-512M}")}"""",
      s"""addJava "${sys.env.getOrElse("JVM_GC_OPTIONS", "${JVM_GC_OPTIONS:--XX:+UseG1GC}")}""""
    ),
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