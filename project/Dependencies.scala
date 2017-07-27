import sbt._

object Dependencies {
  lazy val reactiveKafka = ("com.typesafe.akka" %% "akka-stream-kafka" % "0.16").excludeAll(
    ExclusionRule(organization = "org.apache.kafka")
  )
  lazy val kafka = "org.apache.kafka" %% "kafka" % "0.10.0.1"

//  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val akkaTestkit = "com.typesafe.akka" %% s"akka-testkit" % "2.4.18"

  lazy val embeddedKafka = ("net.manub"     %% "scalatest-embedded-kafka" % "0.9.0").excludeAll(
    ExclusionRule(organization = "org.apache.kafka"),
    ExclusionRule(organization = "com.typesafe.akka")
  )
}
