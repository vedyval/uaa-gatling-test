/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */
package uaa

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import uaa.OAuthComponents._

import uaa.Config._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.core.feeder.Feeder
import org.omg.CosNaming._NamingContextExtStub
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder

/**
 * @author Luke Taylor
 */
object ScimApi {
  def scimClientLogin() = clientCredentialsAccessTokenRequest(
    username = scimClient.id,
    password = scimClient.secret,
    client_id = scimClient.id)

  /**
   * Creates 'n' users by invoking the SCIM API.
   *
   * Usernames can optionally be prefixed
   */
  def createScimUsers(userFeeder: UniqueUsernamePasswordFeeder): ChainBuilder =
    chain.exec(
      scimClientLogin()
    )
    .doIf(haveAccessToken)(
      chain.loop(
      chain.feed(userFeeder)
        .exec((s: Session) => {println("Creating user: %s" format(s.getAttribute("username"))); s})
        .exec(createUser)
      ).asLongAs(s => {userFeeder.hasNext})
    )

  def createScimGroups(groupFeeder: UniqueGroupFeeder): ChainBuilder =
    chain.exec(
      scimClientLogin()
    )
    .doIf(haveAccessToken)(
      chain.loop(
        chain.feed(groupFeeder)
          .exec(getMember("1"))
          .exec(getMember("2"))
          .exec((s: Session) => {println("============================================ Creating group: %s, with members: %s, %s"
            format(s.getAttribute("displayName"), s.getAttribute("memberId_1"), s.getAttribute("memberId_2")));
            s})
          .exec(createGroup)
      ).asLongAs(s => {groupFeeder.hasNext})
    )

  def getMember(suffix: String) = {
    http("Find user by name")
      .get("/Users")
      .queryParam("attributes", "id")
      .queryParam("filter","userName eq '${memberName_" + suffix + "}'")
      .header("Authorization", "Bearer ${access_token}")
      .asJSON
      .check(status.is(200), regex(""""id":"(.*?)"""").saveAs("memberId_" + suffix))
  }


  /**
   * Creates a SCIM user.
   *
   * A suitable access token must already be available in the session, as well as username and password values.
   *
   */
  def createUser =
      http("Create User")
        .post("/Users")
        .header("Authorization", "Bearer ${access_token}")
        .body("""{"name":{"givenName":"Shaun","familyName":"Sheep","formatted":"Shaun the Sheep"},"password":"${password}","userName":"${username}","emails":[{"value":"${username}@blah.com"}]}""")
        .asJSON
        .check(status.is(201), regex(""""id":"(.*?)"""").saveAs("__scimUserId"))

  /**
   * Creates a SCIM group.
   *
   * A suitable access token must already be available in the session, as well as displayName and memberIds.
   *
   */
  def createGroup =
      http("Create Group")
        .post("/Groups")
        .header("Authorization", "Bearer ${access_token}")
        .body("""{"displayName":"${displayName}", "members": [ {"value":"${memberId_1}", {"value":"${memberId_2}"}}]}""")
        .asJSON
        .check(status.is(201), regex(""""id":"(.*?)"""").saveAs("__scimGroupId"))


  /**
   * Finds a user and stores their ID in the session as `userId`. Uses the session attribute "username" for the
   * search.
   */
  def findUserByName =
    http("Find user by name")
      .get("/Users")
      .queryParam("attributes", "id")
      .queryParam("filter","userName eq '${username}'")
      .header("Authorization", "Bearer ${access_token}")
      .asJSON
      .check(status.is(200), regex(""""id":"(.*?)"""").saveAs("userId"))

  /**
   * Pulls a user by  `userId` and stores the data under `scimUser`
   */
  def getUser =
    http("Get User")
      .get("/Users/${userId}")
      .header("Authorization", "Bearer ${access_token}")
      .asJSON
      .check(status.is(200), regex(".*").saveAs("scimUser"))

  /**
   * Performs an update using the data in `scimUser`.
   */
  def updateUser =
    http("Update user")
      .put("/Users/${userId}")
      .header("Authorization", "Bearer ${access_token}")
      .body("${scimUser")
      .asJSON
      .check(status.is(204))


  def changePassword =
      http("Change Password")
        .put("/Users/${userId}/password")
        .header("Authorization", "Bearer ${access_token}")
        .body("""{"password":"${password}"}""")
        .asJSON
        .check(status.is(204))

}
