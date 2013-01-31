package controllers

import play.api.mvc._
import concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.WS
import akka.actor.{Props, ActorSystem}
import actors.Connection

object Application extends Controller {

  def index = Action {
    Async {
      WS.url("http://samy.dindane.com/hello.txt").head().map {
        response =>
          val file = java.io.File.createTempFile("hello", ".txt")

          val responseLength: Integer = response.header("Content-Length").getOrElse {
            throw new Exception("Could not retrieve the file length.")
          }.toInt

          val system = ActorSystem("Connections")

          val connection1 = system.actorOf(Props(new Connection(file, getStartOffset(responseLength, 1, 4), getEndOffset(responseLength, 1, 4))), name = "connection1")
          val connection2 = system.actorOf(Props(new Connection(file, getStartOffset(responseLength, 2, 4), getEndOffset(responseLength, 2, 4))), name = "connection2")
          val connection3 = system.actorOf(Props(new Connection(file, getStartOffset(responseLength, 3, 4), getEndOffset(responseLength, 3, 4))), name = "connection3")
          val connection4 = system.actorOf(Props(new Connection(file, getStartOffset(responseLength, 4, 4), getEndOffset(responseLength, 4, 4))), name = "connection4")

          Ok("Done!")
      }
    }
  }

  private def getEndOffset(responseLength: Integer, connection: Integer, connectionsNumber: Integer = 4): Integer =
    if (connection == connectionsNumber) responseLength - 1 else ((responseLength + (4 - responseLength % 4)) / 4) * connection - 1;

  private def getStartOffset(responseLength: Integer, connection: Integer, connectionsNumber: Integer = 4): Integer =
    ((responseLength + (4 - responseLength % 4)) / 4) * (connection - 1);

}

