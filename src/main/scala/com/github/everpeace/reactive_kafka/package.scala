package com.github.everpeace

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.duration.Duration

package object reactive_kafka {

  def throttolableConsumerFlow(implicit system: ActorSystem, mat: ActorMaterializer): Source[Done, Consumer.Control] = {
    val c = system.settings.config.getConfig("throttolable-consumer")
    implicit val ec = system.dispatcher

    val bootstrapServers = c getString "bootstrap-servers"
    val topic = c getString "topic"
    val autoRestConfig = c getString "auto-offset-reset"
    val groupId = c getString "group-id"
    val throttle = c getInt "throttle"
    val throttlePer = Duration.fromNanos((c getDuration "throttle-per").toNanos)
    val throttleBurst = c getInt "throttle-burst"
    val logPer = c getInt "log-per"
    val offsetCommitBatchSize = c getInt "offset-commit-batch-size"
    val offsetCommitParallelism = c getInt "offset-commit-parallelism"

    val consumerSettings =
      ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId(groupId)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoRestConfig)

    val source = Consumer.committableSource(consumerSettings, Subscriptions.topics(Set(topic)))

    val throttled = if (throttle > 0)
      source.throttle(throttle, throttlePer, throttleBurst, ThrottleMode.shaping)
    else source

    throttled.statefulMapConcat(() => {
      var counter = 0
      msg => {
        if (counter % logPer == 0) {
          system.log.info(s"FakeConsumer consume: $msg")
          counter = 0
        }
        counter += 1
        msg :: Nil
      }
    }).batch(max = offsetCommitBatchSize, m => CommittableOffsetBatch.empty.updated(m.committableOffset))((batch, m) => batch.updated(m.committableOffset))
      .mapAsync(offsetCommitParallelism) { batch =>
      batch.commitScaladsl()
    }
  }
}
