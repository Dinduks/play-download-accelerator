package actors

import akka.actor.Actor
import akka.event.Logging
import concurrent.ExecutionContext.Implicits.global
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS.WSRequestHolder

class Connection(target: FileChannel, source: WSRequestHolder, startOffset: Int, endOffset: Int) extends Actor {
  val log = Logging(context.system, this)
  val headers: (String, String) = ("Range", "bytes=%d-%d".format(startOffset, endOffset))

  def receive = {
    case _ => ()
  }

  override def preStart() = {
    log.info("Started a new actor handling the %d-%d part of the file".format(startOffset, endOffset))

    var position: Int = startOffset

    val toFile: Iteratee[Array[Byte], Unit] = Iteratee.foreach[Array[Byte]] { bytes =>
      target.write(ByteBuffer.wrap(bytes), position)
      position += bytes.length
    }

    source.withHeaders(headers).get(result => toFile)
  }
}
