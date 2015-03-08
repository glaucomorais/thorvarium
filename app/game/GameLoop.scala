package game

import _root_.models.Player

class GameLoop(var player1 : Player, var player2: Player) {

  var state = GameLoop.WaitingInput
  var steps = 0
  var turns = 0

  def reset() = {
    steps = 0
  }

  def update(elapsed : Long) = {

    Set(player1, player2).map { p =>
      if (p.input != null) {
        applyMovement(p, elapsed)
      }
    }

    if (steps <= 0) {
      state = GameLoop.WaitingInput
    }
  }

  def applyMovement(p: Player, elapsed : Long) = {
    if (p.input.movements != null) {

      p.input.movements.map { m =>

        val person = p.persons(m._1)
        val angle = Math.atan2(m._2.x - person.x, m._2.y - person.y)

        if (Math.abs(person.x.toInt - m._2.x.toInt) > 1) {
          person.x += Math.sin(angle) * (((GameLoop.MaxSpeed / 100.0) * person.speed) / 1000)
          steps += 1
        }
        if (Math.abs(person.y.toInt - m._2.y.toInt) > 1) {
          person.y += Math.cos(angle) * (((GameLoop.MaxSpeed / 100.0) * person.speed) / 1000)
          steps += 1
        }
      }
    }
  }

  def newTurn() = {
    turns += 1
    player1.input = null
    player2.input = null
  }
}

object GameLoop {
  val WaitingInput = 1
  val Running = 2

  val sceneWidth = 500
  val sceneHeight = 500

  val MaxSpeed = 2.0 // 2 pixels per second
  val MaxDistance = 120.0
}
