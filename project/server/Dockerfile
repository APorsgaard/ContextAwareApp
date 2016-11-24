
FROM ubuntu:latest

# Check that everything is up to date
RUN apt-get -y update
RUN apt-get -y install gradle default-jre default-jdk
# Make the dir /root/cave and use that
# as the working directory
WORKDIR /root/server

# Copy the required resources to the image
COPY src /root/server/src
COPY gradle /root/server/gradle
COPY *.gradle /root/server/
COPY gradlew /root/server/
COPY gradlew.bat /root/server/
COPY build/libs /root/server/build/

ENTRYPOINT ["java", "-jar", "/root/server/build/server-0.0.1-SNAPSHOT.jar"]
