package actors

import akka.actor.Actor
import akka.event.Logging
import concurrent.ExecutionContext.Implicits.global
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS.WSRequestHolder
import lib.Util
import util.{Success, Failure}

class Connection(target: FileChannel, source: WSRequestHolder, responseLength: Int, connection: Int, connectionsCounter: Int) extends Actor {

  val log = Logging(context.system, this)
  val (startOffset, endOffset) = Util.getStartAndEndOffsets(responseLength, connection, connectionsCounter)
  val headers: (String, String) = ("Range", "bytes=%d-%d".format(startOffset, endOffset))

  def receive = {
    case _ => ()
  }

  override def preStart() = {
    var position: Int = startOffset

    val toFile: Iteratee[Array[Byte], Unit] = Iteratee.foreach[Array[Byte]] { bytes =>
      target.write(ByteBuffer.wrap(bytes), position)
      position += bytes.length
    }

    source.withHeaders(headers).get(result => toFile).onComplete {
      case Success(_) => context.parent ! FinishedDownloadingPart(source.url, connection)
      case Failure(_) => FailedDownload(source.url, connection)
    }
  }

}
