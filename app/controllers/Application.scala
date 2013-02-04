package controllers

import actors.Connection
import akka.actor.{Props, ActorSystem}
import concurrent.ExecutionContext.Implicits.global
import java.nio.channels.FileChannel
import java.nio.file.{StandardOpenOption, Paths}
import lib.Util
import play.api.Play.current
import play.api.mvc._
import play.api.libs.concurrent.Akka
import play.api.libs.ws.WS
import scala.concurrent.duration._
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def temp = {
    val sourceUrl: String = "http://wallpaper.metalship.org/images/megadeth.jpg"
    val targetFilePath: String = "/tmp/megadeth.jpg"
    val source = WS.url(sourceUrl)

    Async {
      source.head().map { response =>
        val responseLength: Int = response.header("Content-Length").getOrElse {
          throw new Exception("Could not retrieve the file length.")
        }.toInt

        val target = FileChannel.open(Paths.get(targetFilePath),
          StandardOpenOption.CREATE, StandardOpenOption.SPARSE, StandardOpenOption.WRITE)
        Util.allocateFile(target, responseLength)

        Akka.system.scheduler.scheduleOnce(0 second) {
          val system = ActorSystem("Connections")

          system.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 1, 4), getEndOffset(responseLength, 1, 4))), name = "connection1")
          system.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 2, 4), getEndOffset(responseLength, 2, 4))), name = "connection2")
          system.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 3, 4), getEndOffset(responseLength, 3, 4))), name = "connection3")
          system.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 4, 4), getEndOffset(responseLength, 4, 4))), name = "connection4")
        }

        Ok("")
      }
    }

  }

  def webSockets = WebSocket.using[String] { request =>
    val in = Iteratee.foreach[String](println).mapDone { _ =>
      println("lol")
    }

    val out = Enumerator("herp")

    (in, out)
  }

  private def getEndOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    if (connection == connectionsNumber) responseLength - 1 else ((responseLength + (4 - responseLength % 4)) / 4) * connection - 1;

  private def getStartOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    ((responseLength + (4 - responseLength % 4)) / 4) * (connection - 1);

}

