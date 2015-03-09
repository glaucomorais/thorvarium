package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import game.GameLoop
import game.models.GamingSet
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Map.empty[Player, ActorRef]
  var board : ActorRef = BoardActor()

  var stepTimer : Cancellable = null
  var gameLoop : GameLoop = null
  var readyToTurn : Int = 0

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>

      log.info("== Player enter :: "+ subscribe.user.nickname +" ==")

      players += (new Player(subscribe.user, players.size + 1) -> subscribe.actor)
      context watch subscribe.actor

      if (players.size >= 2) {

        val now = DateTime.now().getMillis
        log.info("== Start choose timer at :: "+ now +" ==")

        players.map { _._2 ! StartGame(id,
          Person.list,
          Weapon.list,
          now) }

        timer(NothingSelected)
      }

    case set:PlayerSet =>

      players.find( _._1.user.id.get == set.user ) match {
        case Some(p) =>
          p._1.persons = set.persons
          if (players.count( _._1.persons.size >= 3 ) >= 2 && gameLoop == null) {
            stepTimer.cancel()

            val now = DateTime.now().getMillis
            log.info("== Game ready at :: "+ now +" ==")

            val playerSet = players.map(_._1).toSet
            gameLoop = new GameLoop(playerSet)
            players.map { _._2 ! GameReady(playerSet, now) }
          }
        case None => log.info("== PlayerSet not find :: "+ set.user +" ==")
      }

    case set:PlayerTurnSet =>
      if (gameLoop != null && set.input != null) {
        players.find( _._1.user.id.get == set.user ) match {
          case Some(p) =>
            gameLoop.players.find( _.user.id.get == p._1.user.id.get ) match {
              case Some(pl) => pl.input = set.input
              case None => log.info("== PlayerTurnSet not find on GameLoop :: "+ set.user +" ==")
            }
          case None => log.info("== PlayerTurnSet not find :: "+ set.user +" ==")
        }
        if (players.count(_._1.input != null) >= 2) {
          self ! TurnEnd
        }
      }

    case ReadyToTurn =>
      readyToTurn += 1
      if (readyToTurn >= 2) {
        players.map { _._2 ! TurnStart }
        timer(TurnEnd)
      }

    case TurnEnd if sender == self =>
      if (gameLoop != null && stepTimer != null) {
        stepTimer.cancel()

        players.map { _._2 ! PreTurn(gameLoop.players.map { p =>
          p.user.id.get.toString -> p.input
        }.toMap) }

        log.info("== Game turn start ==")
        gameLoop.loop()

        readyToTurn = 0
        gameLoop.newTurn()

        players.map { _._2 ! AfterTurn(
          players.map(_._1).toSet,
          gameLoop.turns) }
      }

    case NothingSelected if sender == self =>
      players.map { _._2 ! NothingSelected }
      endGame()

    case Terminated(user) =>
      players.find( u => u._2 == user ) match {
        case Some(x) =>
          players -= x._1
          context unwatch x._2

          players.map { _._2 ! Won }
          endGame()

        case None => log.error("== Terminated actor not found ==")
      }
  }

  def timer(message: Any) = {
    import system.dispatcher

    stepTimer = system.scheduler.scheduleOnce(
      Duration.create(40, TimeUnit.SECONDS),
      self,
      message)
  }

  def endGame() = {

    log.info("== Game has ended :: "+ id +" ==")

    if (stepTimer != null) {
      stepTimer.cancel()
    }

    board ! EndGame(id)
    context stop self
  }

  def getId = {
    id
  }
}

case class SubscribeGame(user: User, actor: ActorRef)
case class EndGame(id : String)
case class StartGame(id : String,
                     persons: List[Person],
                     weapons: List[Weapon],
                     now: Long)

case class PlayerSet(user: Long, persons: Map[String, Person])
case class GameReady(players: Set[Player], now: Long)
case class PreTurn(inputs: Map[String, GamingSet])
case class AfterTurn(players: Set[Player], turns: Int)
case class PlayerTurnSet(user: Long, input: GamingSet)

object TurnStart
object ReadyToTurn
object TurnEnd
object NothingSelected
object Won
object Lose