#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
MAX_FD="maximum"

warn() { echo "$*"; }
die() { echo; echo "$*"; echo; exit 1; }

cygwin=false; darwin=false; msys=false; nonstop=false
case "$(uname)" in
  CYGWIN*)  cygwin=true ;;
  Darwin*)  darwin=true ;;
  MSYS*|MINGW*) msys=true ;;
  NONSTOP*) nonstop=true ;;
esac

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ ! -x "$JAVACMD" ] && die "ERROR: JAVA_HOME='$JAVA_HOME' is invalid."
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || die "ERROR: No 'java' in PATH. Install Java 17+."
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
    case $MAX_FD in
      max*) MAX_FD=$(ulimit -H -n) || warn "Could not query max file descriptors" ;;
    esac
    case $MAX_FD in
      ''|soft) ;;
      *) ulimit -n "$MAX_FD" || warn "Could not set max file descriptors to $MAX_FD" ;;
    esac
fi

exec "$JAVACMD" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
