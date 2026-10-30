package com.mazlove.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.LocalDateTime
import scala.concurrent.duration._
import scala.util.Random

/**
 * Test de charge Gatling (syntaxe 3.9). L'API doit tourner avant le lancement :
 *   mvn -Pperf gatling:test
 *
 * Paramètres par variables d'environnement :
 *   MAZLOVE_BASE_URL     URL de l'API                            (défaut http://localhost:3001)
 *   MAZLOVE_USERS        utilisateurs virtuels par scénario      (défaut 50)
 *   MAZLOVE_PROVIDER_ID  praticien visé                          (défaut 1)
 *   MAZLOVE_JWT          jeton d'un compte CLIENT ; sans lui, seul le scénario public tourne
 *                        (la réservation exige d'être connecté).
 */
class SessionsPerformanceTest extends Simulation {

  private val baseUrl    = sys.env.getOrElse("MAZLOVE_BASE_URL", "http://localhost:3001")
  private val jwt        = sys.env.getOrElse("MAZLOVE_JWT", "")
  private val providerId = sys.env.getOrElse("MAZLOVE_PROVIDER_ID", "1")
  private val users      = sys.env.get("MAZLOVE_USERS").map(_.toInt).getOrElse(50)
  private val rampUp     = 10.seconds

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Un horaire différent par utilisateur virtuel, pour ne pas tester uniquement le refus
  // « créneau déjà réservé ». LocalDateTime.toString donne "2026-11-02T10:00", lu tel quel par l'API.
  private val startTimes = Iterator.continually(Map(
    "startTime" -> LocalDateTime.now()
      .plusDays(1 + Random.nextInt(60))
      .withHour(9 + Random.nextInt(8))
      .withMinute(0).withSecond(0).withNano(0)
      .toString
  ))

  /** Parcours anonyme typique : liste des praticiens proches puis ouverture d'une fiche. */
  private val browseProviders = scenario("Parcourir les praticiens")
    .exec(
      http("Lister les praticiens proches")
        .get("/api/providers")
        .queryParam("lat", "48.87")
        .queryParam("lng", "2.35")
        .check(status.is(200))
    )
    .pause(1.second, 3.seconds)
    .exec(
      http("Ouvrir une fiche")
        .get(s"/api/providers/$providerId")
        .check(status.in(200, 404))
    )

  /**
   * Réservation par un client connecté. Un horaire aléatoire tombe souvent hors des
   * disponibilités (400) ou sur un créneau pris (409) : ce sont des refus métier normaux,
   * on mesure ici le temps de réponse de toute la chaîne de validation.
   */
  private val bookSessions = scenario("Réserver une séance")
    .feed(startTimes)
    .exec(
      http("Créer une séance")
        .post("/api/sessions")
        .header("Authorization", s"Bearer $jwt")
        .body(StringBody(s"""{"providerId":$providerId,"startTime":"#{startTime}","duration":1}"""))
        .check(status.in(201, 400, 409))
    )

  private val populations =
    if (jwt.isEmpty) List(browseProviders.inject(rampUsers(users).during(rampUp)))
    else List(
      browseProviders.inject(rampUsers(users).during(rampUp)),
      bookSessions.inject(rampUsers(users).during(rampUp))
    )

  setUp(populations: _*)
    .protocols(httpProtocol)
    // Le test échoue si plus de 1 % des requêtes sont en erreur (5xx, timeouts, statuts inattendus).
    .assertions(global.failedRequests.percent.lt(1.0))
}
