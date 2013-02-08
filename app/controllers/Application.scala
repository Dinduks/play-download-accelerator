package controllers

import actors.{SetChannel, AddConnection, ConnectionsHandler}
import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.mvc._
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.JsValue
import play.api.Logger

object Application extends Controller {

  val log: Logger = Logger("axel")
  val actorSystem: ActorSystem = ActorSystem("axelActorSystem")
  val connectionsHandler: ActorRef = actorSystem.actorOf(Props(new ConnectionsHandler()), name = "connectionsHandler")

  def index = Action {
    Ok(views.html.index())
  }

  def startDownload(sourceUrl: String) = {
    val fileName: String = if (2 > sourceUrl.split("/").size) System.currentTimeMillis.toString else sourceUrl.split("/").last
    val target: String = "/tmp/%s".format(fileName)

    connectionsHandler ! AddConnection(sourceUrl, target)
  }

  def ws = WebSocket.using[JsValue] { request =>
    log.info("New connection")

    val in = Iteratee.foreach[JsValue] { message =>
      (message \ "kind").as[String] match {
        case "newDownload" => startDownload((message \ "data").as[String])
      }
    }.mapDone { _ => log.info("Disconnected") }

    val (out, channel) = Concurrent.broadcast[JsValue]
    connectionsHandler ! SetChannel(channel)

    (in, out)
  }

}
