FROM openjdk:8-slim

# Git is needed in order to update the dls submodule
RUN apt-get update && apt-get install -y wget protobuf-compiler git

RUN mkdir /temporal-java-samples
WORKDIR /temporal-java-samples
