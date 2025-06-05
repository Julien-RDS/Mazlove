package com.mazlove.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SessionsPerformanceTest extends Simulation {

  // Configuration de base
  val httpProtocol = http
    .baseUrl("http://localhost:3001")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Scénario de création de sessions
  val createSessionScenario = scenario("Create Sessions")
    .exec(
      http("Create Session")
        .post("/api/sessions")
        .body(StringBody("{\"providerId\":\"provider1\",\"startTime\":\"${startTime}\",\"duration\":1}"))
        .check(status.is(201))
    )

  // Scénario de validation d'arrivée
  val validateArrivalScenario = scenario("Validate Arrival")
    .exec(
      http("Validate Arrival")
        .post("/api/sessions/${sessionId}/validate-arrival")
        .body(StringBody("{\"code\":\"123456\"}"))
        .check(status.is(200))
    )

  // Scénario de fin de session
  val confirmEndScenario = scenario("Confirm End")
    .exec(
      http("Confirm End")
        .post("/api/sessions/${sessionId}/confirm-end")
        .check(status.is(200))
    )

  // Configuration des utilisateurs
  val users = 100
  val rampUpDuration = 10

  // Lancement des scénarios
  setUp(
    createSessionScenario.inject(
      rampUsers(users) over (rampUpDuration seconds)
    ).protocols(httpProtocol),

    validateArrivalScenario.inject(
      rampUsers(users) over (rampUpDuration seconds)
    ).protocols(httpProtocol),

    confirmEndScenario.inject(
      rampUsers(users) over (rampUpDuration seconds)
    ).protocols(httpProtocol)
  )
}
