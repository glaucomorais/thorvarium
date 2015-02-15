package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Props
import models.User
import play.api.libs.json.{JsValue, Json}


class UserActor(user: User, board: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  override def preStart() = {
    BoardActor() ! Subscribe(user)
  }

  def receive = LoggingReceive {
    case command:JsValue if (command \ "type").asOpt[String].isDefined =>

      val t = (command \ "type").as[String]
      t match {
        case "message" => board ! Message(user, (command \ "content").as[String])
      }

    case message:Message if sender == board =>
      out ! Json.obj(
        "type" -> "message",
        "content" -> message.message,
        "user" -> message.user.id)

    case BoardMembers(members) if sender == board =>
      out ! Json.obj("command" -> "members", "value" -> members)

    case other => log.error(">>> Unhandled: " + other)
  }
}

object UserActor {
  def props(user: User)(out: ActorRef) = Props(new UserActor(user, BoardActor(), out))
}
