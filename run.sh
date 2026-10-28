#!/usr/bin/env bash
# Script to run Enterprise CRM Application

# Auto-detect JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null)"
    fi
fi

# Locate Maven
MVN_BIN=""
if command -v mvn &> /dev/null; then
    MVN_BIN="mvn"
elif [ -f "/Users/vanshinainal/.m2/wrapper/dists/apache-maven-3.9.6-bin/439sdfsg2nbdob9ciift5h5nse/apache-maven-3.9.6/bin/mvn" ]; then
    MVN_BIN="/Users/vanshinainal/.m2/wrapper/dists/apache-maven-3.9.6-bin/439sdfsg2nbdob9ciift5h5nse/apache-maven-3.9.6/bin/mvn"
fi

if [ -z "$MVN_BIN" ]; then
    echo "Error: Maven not found."
    exit 1
fi

echo "Starting Enterprise CRM Application with Java at $JAVA_HOME..."
"$MVN_BIN" spring-boot:run
