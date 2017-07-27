package com.github.everpeace.reactive_kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object ThrottolableConsumer extends App {

  val config                = ConfigFactory.load()
  implicit val system = ActorSystem("FakeConsumer", config)
  implicit val materializer = ActorMaterializer()

  val (control, done) = fakeConsumerFlow.toMat(Sink.ignore)(Keep.both).run()

  sys.addShutdownHook({
    Await.result(control.shutdown(), 10 seconds)
    Await.result(done, 10 seconds)
    Await.result(system.terminate(), 10 seconds)
  })
}
