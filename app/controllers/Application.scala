package controllers

import actors.Connection
import akka.actor.{Props, ActorSystem}
import concurrent.ExecutionContext.Implicits.global
import java.nio.channels.FileChannel
import java.nio.file.{StandardOpenOption, Paths}
import lib.Util
import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{Json, JsString, JsValue}
import play.api.libs.ws.WS
import scala.collection._

object Application extends Controller {

  val actorSystems: mutable.Map[String, ActorSystem] = mutable.Map.empty

  def index = Action {
    Ok(views.html.index())
  }

  def startDownload(sourceUrl: String) = {
    val fileName: String = if (2 > sourceUrl.split("/").size) System.currentTimeMillis.toString else sourceUrl.split("/").last
    val targetFilePath: String = "/tmp/%s".format(fileName)
    val source = WS.url(sourceUrl)

    source.head().map { response =>
      val responseLength: Int = response.header("Content-Length").getOrElse {
        // TODO: Show this message as a notification
        // TOFIX: When the file size can't be retrieved, I can't "see" the exception
        throw new Exception("Could not retrieve the file length.")
      }.toInt

      val target = FileChannel.open(Paths.get(targetFilePath),
        StandardOpenOption.CREATE, StandardOpenOption.SPARSE, StandardOpenOption.WRITE)
      Util.allocateFile(target, responseLength)

      // TOFIX: When given an invalid system name, I can't "see" the exception
      if (actorSystems.put(sourceUrl, ActorSystem("actorSystem%d".format(actorSystems.size))).isEmpty) {
        actorSystems.get(sourceUrl).get.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 1, 4), getEndOffset(responseLength, 1, 4))), name = "connection1")
        actorSystems.get(sourceUrl).get.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 2, 4), getEndOffset(responseLength, 2, 4))), name = "connection2")
        actorSystems.get(sourceUrl).get.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 3, 4), getEndOffset(responseLength, 3, 4))), name = "connection3")
        actorSystems.get(sourceUrl).get.actorOf(Props(new Connection(target, source, getStartOffset(responseLength, 4, 4), getEndOffset(responseLength, 4, 4))), name = "connection4")
      } else {
        // TODO: Show a notification if the file is already being downloaded
      }

      ()
    }
  }

  def ws = WebSocket.using[JsValue] { request =>
    val in = Iteratee.foreach[JsValue] { message =>
      (message \ "kind").as[String] match {
        case "newDownload" => startDownload((message \ "data").as[String])
      }
    }
    val out = Enumerator(Json.parse(Json.stringify(JsString("duh herro"))))

    (in, out)
  }

  private def getEndOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    if (connection == connectionsNumber) responseLength - 1 else ((responseLength + (4 - responseLength % 4)) / 4) * connection - 1;

  private def getStartOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    ((responseLength + (4 - responseLength % 4)) / 4) * (connection - 1);

}

