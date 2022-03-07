#!/bin/bash

# Install prerequisites for server

# Java JDK 17
wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.2_8.tar.gz
tar xvf OpenJDK17U-jdk_x64_linux_hotspot_17.0.2_8.tar.gz
sudo mv jdk-17.0.2+8 /opt/jdk-17.0.2+8

export JAVA_HOME=/opt/jdk-17.0.2+8
export PATH=$JAVA_HOME/bin:$PATH
source ~/.bashrc
