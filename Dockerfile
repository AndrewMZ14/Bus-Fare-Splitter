# Use OpenJDK since Android SDK needs Java
FROM openjdk:17-jdk-slim

# Set environment variables
ENV ANDROID_SDK_ROOT /sdk
ENV PATH $PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install required tools
RUN apt-get update && apt-get install -y \
    wget unzip git bash \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android command-line tools
RUN mkdir -p /sdk/cmdline-tools \
    && cd /sdk/cmdline-tools \
    && wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip -d /sdk/cmdline-tools/ \
    && mv /sdk/cmdline-tools/cmdline-tools /sdk/cmdline-tools/latest \
    && rm cmdline-tools.zip

# Accept licenses and install required SDK packages
RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Copy your Android project into the container
WORKDIR /workspace
COPY . /workspace

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Build the Android app
RUN ./gradlew assembleDebug

# Default command
CMD ["./gradlew", "assembleDebug"]
