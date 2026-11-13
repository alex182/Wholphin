# Dev container for the Wholphin accessibility fork.
# All builds, tests and formatting run here so the host stays untouched.
FROM eclipse-temurin:21-jdk-noble

ARG CMDLINE_TOOLS_VERSION=13114758
ARG BUILD_TOOLS_VERSION=36.0.0
ARG PLATFORM_VERSION=37.0
ARG KTLINT_VERSION=1.8.0
ARG USER_ID=1000
ARG GROUP_ID=1000

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/home/dev/.gradle
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends \
        git unzip curl ca-certificates python3 && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -fsSL -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip && \
    unzip -q /tmp/tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/tools.zip && \
    yes | sdkmanager --licenses > /dev/null && \
    sdkmanager --install "platform-tools" "platforms;android-${PLATFORM_VERSION}" "build-tools;${BUILD_TOOLS_VERSION}" > /dev/null

RUN curl -fsSL -o /usr/local/bin/ktlint https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint && \
    chmod +x /usr/local/bin/ktlint

# Reuse the image's existing uid-1000 user if present, renamed to "dev".
RUN (getent passwd ${USER_ID} && usermod -l dev -d /home/dev -m $(getent passwd ${USER_ID} | cut -d: -f1)) || \
    useradd -m -u ${USER_ID} -o dev; \
    mkdir -p /home/dev/.gradle && chown -R ${USER_ID}:${GROUP_ID} /home/dev $ANDROID_HOME

USER dev
WORKDIR /workspace
RUN git config --global --add safe.directory /workspace
