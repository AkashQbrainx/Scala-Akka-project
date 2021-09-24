package com.qbrainx.actor
import akka.actor.SupervisorStrategy.{Restart, Resume, stop}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import MyActor.{EnterJson}
import com.qbrainx.model.Student
import com.qbrainx.model.StudentImplicits._
import com.qbrainx.repository.StudentDataBaseImpl._
import spray.json.JsonParser.ParsingException
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object MyActor{
  case object EnterJson
  case object Stop
  case object StopActor
}
class MyActor extends Actor {

  val child: ActorRef =context.actorOf(Props[MyChild],"myChild")

  override def preStart(): Unit = {
    println(s"${self.path} - actor is started")
  }
  override def postStop(): Unit = {
    println(s"${self.path} - actor is stopped")
  }
  override def receive: Receive = {
    case EnterJson =>
      val input: String =StdIn.readLine()
      println(input)
      child ! input
      context.become(studentState)

    case _=>println("case mismatch")
  }

  def studentState: Receive={
    case student:Student=>
      println(s"${self.path}-student received")
      insert(student)
      println(s"${self.path}-student details inserted")
      context.unbecome()
      println("invoking stop by poisonpill")
      self!PoisonPill
  }


  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Resume
      case _:ParsingException=>
        println("enter the correct Json string")
        stop
      case _: Exception=> stop
    }
 }

class MyChild extends Actor {

  override def receive: Receive = {
    case msg: String =>
      println(s"${self.path}-Json Received")
      println(msg.parseJson.prettyPrint)
      val student: Student = msg.parseJson.convertTo[Student]
      println(student)
      sender()!student
    case _=>println("Invalid cannot be inserted to db")
  }

}

