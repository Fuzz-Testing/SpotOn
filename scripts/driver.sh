#!/bin/bash

#Driver used to run Zest with targetCode coverage as well as running SpotOn Guidance

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

echo "ROOT_DIR=$ROOT_DIR"
# Find JQF classes and JARs
project="jqf"
version="2.0-SNAPSHOT"

FUZZ_DIR="${ROOT_DIR}/fuzz/target/"
INST_DIR="${ROOT_DIR}/instrument/target/"

FUZZ_JAR="${FUZZ_DIR}/$project-fuzz-$version.jar"
INST_JAR="${INST_DIR}/$project-instrument-$version.jar"

# Compute classpaths (the /classes are only for development; 
#   if empty the JARs will have whatever is needed)
# adding static-analysis jar in the INST_CLASSPATH
INST_CLASSPATH="${INST_DIR}/classes:${INST_JAR}:${INST_DIR}/dependency/asm-9.1.jar:${INST_DIR}/dependency/static-analysis-1.0-SNAPSHOT.jar"
FUZZ_CLASSPATH="${FUZZ_DIR}/classes:${FUZZ_JAR}"

# If user-defined classpath is not set, default to '.'
if [ -z "${CLASSPATH}" ]; then
  CLASSPATH="."
fi  

# Java Agent config (can be turned off using env var)
if [ -z "$JQF_DISABLE_INSTRUMENTATION" ]; then
  JAVAAGENT="-javaagent:${INST_JAR}"
fi

# Run Java
if [ -n "$JAVA_HOME" ]; then
    java="$JAVA_HOME"/bin/java
else
    java="java"
fi
"$java" -ea \
  -Xbootclasspath/a:"$INST_CLASSPATH" \
  ${JAVAAGENT} \
  -Djanala.conf="${SCRIPT_DIR}/janala-analysis.conf" \
  -cp "${FUZZ_CLASSPATH}:${CLASSPATH}" \
  ${JVM_OPTS} \
  $@

