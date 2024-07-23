#!/bin/sh

# Wrapper to run freerouting_cli jar assuming openjdk-jre is installed on
# the system. Invocation is compatible with the binary pack's jpackage
# generated elf. Runs only when installed in /opt/freerouting_cli/bin

BINDIR="`dirname $0`"
LIBDIR="$BINDIR/../lib"

exec java -jar "$LIBDIR/freerouting_cli.jar" "$@"

