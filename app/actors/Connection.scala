package actors

import akka.actor.Actor
import akka.event.Logging
import concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.file.{StandardOpenOption, Paths}

class Connection(target: String, source: String, startOffset: Int, endOffset: Int) extends Actor {
  val log = Logging(context.system, this)

  val url = WS.url(source)
  val headers: (String, String) = ("Range", "bytes=%d-%d".format(startOffset, endOffset))

  val targetFileChannel = FileChannel.open(Paths.get(target),
    StandardOpenOption.CREATE, StandardOpenOption.SPARSE, StandardOpenOption.WRITE)


  def receive = {
    case _ => ()
  }

  override def preStart() = {
    log.info("Started a new actor handling the %d-%d part of the file".format(startOffset, endOffset))

    var position: Int = startOffset

    val toFile: Iteratee[Array[Byte], Unit] = Iteratee.foreach[Array[Byte]] { bytes =>
      targetFileChannel.write(ByteBuffer.wrap(bytes), position)
      position += bytes.length
    }

    url.withHeaders(headers).get(result => toFile)
  }
}
