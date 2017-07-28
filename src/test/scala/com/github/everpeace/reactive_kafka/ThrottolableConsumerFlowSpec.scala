package com.github.everpeace.reactive_kafka

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class ThrottolableConsumerFlowSpec extends FlatSpec with Matchers with EmbeddedKafka with RandomPortSupport {
  val kafkaPort = temporaryServerPort()
  val zkPort = temporaryServerPort()
  val TOPIC = "topic1"

  implicit val system = ActorSystem("ThrottolableConsumerFlowSpec", ConfigFactory.parseString(
    s"""
      |throttolable-consumer {
      |  bootstrap-servers = "localhost:$kafkaPort"
      |  topic = "$TOPIC"
      |  group-id = "throttolable-consumer-flow-spec"
      |  throttle = 0
      |  offset-commit-batch-size = 2
      |  offset-commit-parallelism = 10
      |}
    """.stripMargin))
  implicit val materializer = ActorMaterializer()
  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort, zkPort)
  implicit val byteArraySer = new ByteArraySerializer
  implicit val stringSer = new StringSerializer

  def createMsg(n: Int): Seq[(Array[Byte], String)] = {
    def gen = {
      val key = scala.util.Random.alphanumeric.take(10).mkString.getBytes()
      val msg = scala.util.Random.alphanumeric.take(10).mkString
      key -> msg
    }
    Iterator.tabulate(n)(_ => gen).toSeq
  }

  "FakeConsumerFlow" should "consume messages correctly" in withRunningKafka {
    createCustomTopic("topic1")
    val p = TestProbe("sinkProbe")

    val control = throttolableConsumerFlow.toMat(Sink.actorRef(p.ref, Done))(Keep.left).run()

    Thread.sleep((5 second).toMillis)

    val n = 100
    createMsg(n).foreach(kv => publishToKafka(TOPIC, kv._1, kv._2))

    (0 until n).foreach(_ => p.expectMsgType[Done])

    Await.result(control.shutdown(), 1 minute)
  }
}
