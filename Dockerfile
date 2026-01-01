FROM eclipse-temurin:24-alpine

LABEL maintainer="qubership.org"
LABEL org.opencontainers.image.authors="qubership.org"
LABEL org.opencontainers.image.description="Qubership Inventory Tool CLI"
LABEL org.opencontainers.image.source="https://github.com/netcracker/qubership-inventory-tool-cli"

RUN mkdir /app
COPY target/qubership-inventory-tool-cli-*-fat.jar /app/inventory-tool.jar
COPY target/inventory-tool /app/inventory-tool
COPY docker/ci.properties /app/inventory-tool/default/config/profiles/ci.properties
COPY docker/logback.xml /app/logback.xml

# Alternate container commands
COPY docker/ci-exec.sh      /usr/local/bin/ci-exec
COPY docker/ci-assembly.sh  /usr/local/bin/ci-assembly
COPY docker/ci-obfuscate.sh /usr/local/bin/ci-obfuscate

USER root
RUN mkdir -p /var/input /var/output \
    && chmod a+rx /usr/local/bin/*
RUN chown -R 1001:1001 /app
USER 1001

CMD [ "java", "-Xmx512m", "-Dlogback.configurationFile=/app/logback.xml", "-jar", "/app/inventory-tool.jar", "ci-exec", "--dockerMode=true" ]
