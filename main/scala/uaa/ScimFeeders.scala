package uaa

import com.excilys.ebi.gatling.core.feeder.Feeder
import java.util.concurrent.ConcurrentLinkedQueue

import collection.JavaConversions._
import util.Random
import collection.{immutable, mutable}

/**
 * User feeder which only supplies each user once.
 */
case class UniqueUsernamePasswordFeeder(usrs: Seq[User], password: Option[String] = None) extends Feeder {
  private val users = new ConcurrentLinkedQueue[User](usrs)

  println("%d users".format(users.size()))

  def next = {
    val user = users.remove()
    val pass = password match { case None => user.password; case Some(p) => p}
    Map("username" -> user.username, "password" -> pass)
  }

  def hasNext = !users.isEmpty
}

case class UniqueGroupFeeder(usrs: Seq[User], grps: Seq[Group], grpSize: Int = 2) extends Feeder {
  private val nameFeeder = new UniqueDisplayNameFeeder(grps)
  private val memberFeeder = new RandomGroupMemberFeeder(usrs, grpSize)

  def hasNext = nameFeeder.hasNext && memberFeeder.hasNext

  def next = {
    val map = mutable.HashMap.empty[String, String]
    map.putAll(nameFeeder.next)
    map.putAll(memberFeeder.next)
    println("===================================== next group: %s" format(map))
    map.toMap
  }
}

case class UniqueDisplayNameFeeder(grps: Seq[Group]) extends Feeder {
  private val groups = new ConcurrentLinkedQueue[Group](grps)

  println("%d groups".format(groups.size()))

  def next = {
    Map("displayName" -> groups.remove().displayName)
  }

  def hasNext = !groups.isEmpty
}

case class RandomGroupMemberFeeder(usrs: Seq[User], n: Int = 0) extends Feeder {
  private val users = usrs
  private val randGen = new Random
  private val num = n

  println("picking %d members from %d users" format(num, users.size))

  def next = {
    val members = mutable.HashMap.empty[String, String]
    (1 to num).foreach { i =>
      members += (("memberName_" + i) -> users.get(randGen.nextInt(users.size)).username)
    }
    println("===================================== next member set: %s" format(members))
    members.toMap
  }

  def hasNext = !users.isEmpty && num > 0
}
