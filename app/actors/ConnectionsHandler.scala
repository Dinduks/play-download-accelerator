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

  val actorSystems: mutable.Map[String, (Short, ActorRef)] = mutable.Map.empty
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
  }

  def createActor(responseLength: Int, target: FileChannel, source: WS.WSRequestHolder) = {
    for (i <- 1 to 4) {
      actorSystems.put(
        source.url,
        (1, context.actorOf(
            Props(new Connection(target, source, getStartOffset(responseLength, i), getEndOffset(responseLength, i))),
            name = "connection%d".format(i)
          )
        )
      )
    }
  }
  private def getEndOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    if (connection == connectionsNumber) responseLength - 1 else ((responseLength + (4 - responseLength % 4)) / 4) * connection - 1;

  private def getStartOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    ((responseLength + (4 - responseLength % 4)) / 4) * (connection - 1);

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