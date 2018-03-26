package util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait MaterializerSupport {
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()
}
