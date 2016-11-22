
FROM ubuntu:latest

# Check that everything is up to date
RUN apt-get -y update
RUN apt-get -y install gradle
# Make the dir /root/cave and use that
# as the working directory
WORKDIR /root/server

# Copy the required resources to the image
COPY src /root/server/src
COPY gradle /root/server/gradle
COPY *.gradle /root/server/
COPY gradlew /root/server/
COPY gradlew.bat /root/server/

ENTRYPOINT ["/root/cave/gradlew", "bootRun"]
