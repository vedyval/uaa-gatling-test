import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import uaa.Config._
import uaa.ScimApi._
import uaa.{UniqueGroupFeeder}

class ScimGroupsWorkoutSimulation extends Simulation {
  def createGroups = scenario("Create groups")
    .pause(2)
    .insertChain(createScimGroups(UniqueGroupFeeder(users, groups, avgGroupSize)))

  def apply = {
    Seq(createGroups.configure users 5 protocolConfig uaaHttpConfig)
  }
}