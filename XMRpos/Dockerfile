FROM eclipse-temurin:21-jdk-jammy

ARG ANDROID_SDK_VERSION=11076708_latest
ARG ANDROID_HOME=/opt/android-sdk
ENV ANDROID_HOME=${ANDROID_HOME} \
    ANDROID_SDK_ROOT=${ANDROID_HOME} \
    PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:/opt/gradle-8.9/bin:${PATH}" \
    DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget curl unzip git ca-certificates bash && rm -rf /var/lib/apt/lists/*

# Android cmdline-tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp/ && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && rm /tmp/cmdline-tools.zip

# Preinstall the SDK
ARG ANDROID_API_LEVEL=35
ARG BUILD_TOOLS_VERSION=35.0.0
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" \
               "platforms;android-${ANDROID_API_LEVEL}" \
               "build-tools;${BUILD_TOOLS_VERSION}"

# Gradle
RUN curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt && rm /tmp/gradle.zip

RUN printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'cd /workspace' \
  'if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then' \
  '  echo "Gradle wrapper missing. Bootstrapping with Gradle 8.9…"' \
  '  /opt/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --distribution-type bin' \
  'fi' \
  'chmod +x gradlew || true' \
  'exec "$@"' > /usr/local/bin/docker-entrypoint.sh \
 && chmod +x /usr/local/bin/docker-entrypoint.sh

WORKDIR /workspace
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./gradlew","--no-daemon","--max-workers=2","-Pkotlin.daemon.jvm.options=-Xmx512m","assembleDebug"]
