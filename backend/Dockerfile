# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
# A proxy CA may be supplied as a BuildKit secret in managed environments.
RUN --mount=type=secret,id=proxy_ca --mount=type=cache,target=/root/.m2/repository \
    if [ -f /run/secrets/proxy_ca ]; then \
      cp "$JAVA_HOME/lib/security/cacerts" /tmp/cacerts && \
      keytool -importcert -noprompt -storepass changeit -keystore /tmp/cacerts -alias session-proxy -file /run/secrets/proxy_ca && \
      export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/tmp/cacerts -Djavax.net.ssl.trustStorePassword=changeit"; \
    fi && \
    if [ -n "$HTTPS_PROXY" ]; then \
      proxy_address="${HTTPS_PROXY#*://}"; \
      proxy_host="${proxy_address%:*}"; proxy_port="${proxy_address##*:}"; \
      printf '<settings><proxies><proxy><id>build-proxy</id><active>true</active><protocol>http</protocol><host>%s</host><port>%s</port></proxy></proxies></settings>\n' "$proxy_host" "$proxy_port" > /tmp/maven-settings.xml; \
      set -- -s /tmp/maven-settings.xml; \
    fi && \
    mvn "$@" -B -q package -Dmaven.test.skip=true && rm -f /tmp/cacerts /tmp/maven-settings.xml

# Runtime : JRE seul sur Alpine (ni JDK, ni Maven, ni curl ; le healthcheck utilise le wget de busybox).
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Utilisateur non-root. uploads/ (volume) et logs/ (profil prod, cf. logback-prod.xml) doivent lui appartenir.
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads /app/logs && chown app:app /app/uploads /app/logs
COPY --from=build --chown=app:app /app/target/backend-*.jar app.jar
USER app
VOLUME /app/uploads
EXPOSE 3001
# La JVM dimensionne son tas sur la mémoire du conteneur, pas sur celle de l'hôte.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
