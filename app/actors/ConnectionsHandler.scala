package actors

import akka.actor.{ActorRef, Props, Actor}
import collection.mutable
import concurrent.{ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.WS
import java.nio.channels.FileChannel
import java.nio.file.{StandardOpenOption, Paths}
import lib.Util
import play.api.libs.iteratee.Concurrent.Channel

class ConnectionsHandler extends Actor {

  type filePart = mutable.Map[Int, ActorRef]
  val actorSystems: mutable.Map[String, filePart] = mutable.Map.empty
  var channel: Channel[JsValue] = _

  def receive = {
    case AddConnection(url, targetFilePath) => {
      val source = WS.url(url)

      source.head().map { response =>
        response.status match {
          case 200 => {
            response.header("Content-Length") match {
              case Some(responseLength) => {
                channel.push(Json.obj(
                  "kind" -> "info",
                  "data" -> "Starting a new download..."
                ))
                val target: FileChannel = allocateAndGetFileChannel(targetFilePath, responseLength.toInt)
                createActor(responseLength.toInt, target, source);
              }
              case None => {
                channel.push(Json.obj(
                  "kind" -> "error",
                  "data" -> "Could not retrieve the file length"
                ))
              }
            }
          }
          case _ => {
            channel.push(Json.obj(
              "kind" -> "error",
              "data" -> "Can't download this file (status code ≠ 200)"
            ))
          }
        }
      }
    }
    case SetChannel(channel) => this.channel = channel
    case FinishedDownloadingPart(url, part) => {
      context.stop(actorSystems.get(url).get.get(part).get)
      actorSystems.get(url).get.remove(part)

      if (actorSystems.get(url).get.isEmpty)
      {
        actorSystems.remove(url)

        channel.push(Json.obj(
          "kind" -> "downloadFinished",
          "data" -> "Download finished"
        ))
      }
    }
    case FailedDownload(url, part) => {
      channel.push(Json.obj(
        "kind" -> "downloadFailure",
        "data" -> "Failed retriving the part %d of %s".format(part, url)
      ))
    }
  }

  def createActor(responseLength: Int, target: FileChannel, source: WS.WSRequestHolder) = {
    actorSystems.put(source.url, mutable.Map.empty)
    for (i <- 1 to 4) {
      actorSystems.get(source.url).get.put(
        i,
        context.actorOf(
          Props(new Connection(target, source, responseLength, i, 4)),
          name = "connection%d".format(i)
        )
      )
    }
  }

  def allocateAndGetFileChannel(targetFilePath: String, responseLength: Int) = {
    val target: FileChannel = FileChannel.open(
      Paths.get(targetFilePath),
      StandardOpenOption.CREATE, StandardOpenOption.SPARSE, StandardOpenOption.WRITE
    )

    Util.allocateFile(target, responseLength)

    target
  }

}

case class AddConnection(url: String, targetFilePath: String)
case class SetChannel(channel: Channel[JsValue])
case class FinishedDownloadingPart(url: String, part: Int)
case class FailedDownload(url: String, part: Int)
