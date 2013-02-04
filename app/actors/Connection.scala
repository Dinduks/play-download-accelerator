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
    log.info("Started | Start offset: %d | End offset: %d".format(startOffset, endOffset))

    var position: Int = startOffset

    val toFile: Iteratee[Array[Byte], Unit] = Iteratee.foreach[Array[Byte]] { bytes =>
      target.write(ByteBuffer.wrap(bytes), position)
      position += bytes.length
    }

    source.withHeaders(headers).get(result => toFile).onComplete {
      case _ => {
        log.info("Done. Size: %s bytes".format(position - startOffset))
        context.stop(self)
      }
    }
  }

  override def postStop() = {
    log.info("Stopped.")
  }
}
