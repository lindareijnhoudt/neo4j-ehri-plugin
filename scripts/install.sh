#!/bin/bash

#
# Install the EHRI libs into a Neo4j instance. One and only
# argument should specify the path the the NEO4JPATH which
#

BLUEPRINTS_VERS="2.1.0"
BLUEPRINTS_DEPS=( frames blueprints-core blueprints-neo4j-graph )

if [ $# -ne 1 ]; then
    echo "Usage: `basename $0` <NEO4JPATH>"
    exit 1
fi

NEO4JPATH=$1

if [ ! -e $NEO4JPATH -o ! -d $NEO4JPATH ]; then
    echo "Neo4j path '$NEO4JPATH' does not exist, or is not a directory"
    exit 2
fi


if [ ! -e $NEO4JPATH/plugin -o ! -e $NEO4JPATH/system/lib ]; then
    echo "Cannot detect 'plugin' or 'system/lib' directories in '$NEO4JPATH'. Are you sure this is the right dir?"
    exit 2
fi

# Check blueprints dependencies
FLAG=0
for dep in ${BLUEPRINTS_DEPS[@]}; do
    jar=${dep}-${BLUEPRINTS_VERS}.jar
    if [ ! -e $NEO4JPATH/system/lib/$jar ] ; then
        echo "Missing dependency: '$jar'. This must manually be installed in $NEO4JPATH/system/lib."
        FLAG=1
    fi
done
if [ $FLAG -eq 1 ] ; then
    echo "Missing manual dependencies."
    exit 3
fi

echo "Attempting package..."
mvn clean test-compile package || { echo "Maven package exited with non-zero status, install aborted..."; exit 4; }

echo "Copying `ls ehri-plugin/target/ehri*jar` to $NEO4JPATH/plugin" 
cp ehri-plugin/target/ehri*jar $NEO4JPATH/plugin
cp ehri-extension/target/ehri*jar $NEO4JPATH/plugin
cp ehri-extension/target/ehri*jar $NEO4JPATH/system/lib
cp ehri-frames/target/ehri*jar $NEO4JPATH/system/lib

exit 0
