#!/bin/bash

# Figure out script absolute path
pushd $(dirname $0) >/dev/null
SCRIPT_DIR=$(pwd)
popd >/dev/null

# The root dir is one up
ROOT_DIR=$(dirname $SCRIPT_DIR)

# Create classpath
cp="$ROOT_DIR/fuzz/target/classes:$ROOT_DIR/fuzz/target/test-classes"

for jar in $ROOT_DIR/fuzz/target/dependency/*.jar; do
  cp="$cp:$jar"
done

examples_cp="$ROOT_DIR/examples/target/classes:$ROOT_DIR/examples/target/test-classes"

for jar in $ROOT_DIR/examples/target/dependency/*.jar; do
  examples_cp="$examples_cp:$jar"
done

cp="$cp:$examples_cp"
#cp="$cp:/media/soha/aac0ad21-227d-4952-81c1-3117f40e4379/home/oem/git/serverfuzz-eval/eval-code/serverfuzz-eval/examples/target/jqf-examples-2.0-SNAPSHOT.jar"

echo $cp
