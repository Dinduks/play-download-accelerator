package actors

import akka.actor.Actor
import akka.event.Logging
import java.io.File
import play.api.libs.iteratee.{Enumeratee, Iteratee}
import play.api.libs.ws.WS

class Connection(file: File, startOffset: Int, endOffset: Int) extends Actor {
  val log = Logging(context.system, this)
  val url = WS.url("http://samy.dindane.com/hello.txt")

  def receive = {
    case _ => ()
  }

  override def preStart() = {
    log.info("Started a new actor handling the %d-%d part of the file".format(startOffset, endOffset))

    val consume = Iteratee.foreach[Array[Byte]] { byte =>
        println(new String(byte))
    }
    val toConsole = Enumeratee.map[Array[Byte]](bytes => bytes)
    //    url.get(result => toConsole &> consume)
  }
}
