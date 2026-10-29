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

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/backend-*.jar app.jar
VOLUME /app/uploads
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]
