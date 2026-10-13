#!/usr/bin/env bash
# Dev client with hotswap.
#   ./dev.sh client    start Minecraft (JetBrains Runtime, hotswap agent on port 5006)
#   ./dev.sh hotswap   compile and push changed classes into that client
#   ./dev.sh watch     push again whenever sources change
#   ./dev.sh status    check that the client is listening
#   ./dev.sh build     production build
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdks/temurin-25}"
export PATH="$JAVA_HOME/bin:$PATH"
# Keep downloads in the home Gradle cache. A sandbox may point this at /tmp.
export GRADLE_USER_HOME="$HOME/.gradle"
JBR="${RUBY_JBR_HOME:-$HOME/.local/jdks/jbr-25}"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
	echo "JDK 25 not found at $JAVA_HOME" >&2
	exit 1
fi

cmd="${1:-}"
if [[ $# -gt 0 ]]; then
	shift
fi

case "$cmd" in
	client)
		if [[ ! -x "$JBR/bin/java" ]]; then
			echo "JetBrains Runtime not found at $JBR" >&2
			echo "Enhanced hotswap needs that runtime. Download a JBR 25 SDK into that directory." >&2
			exit 1
		fi
		if ! "$JBR/bin/java" -XX:+AllowEnhancedClassRedefinition -version >/dev/null 2>&1; then
			echo "The JVM at $JBR rejected -XX:+AllowEnhancedClassRedefinition" >&2
			exit 1
		fi
		if "$JAVA_HOME/bin/java" -cp build/dev/ruby-hotswap-agent.jar -Druby.hotswap.port=5006 ruby.dev.HotswapAgent --ping >/dev/null 2>&1; then
			echo "A dev client is already listening on port 5006." >&2
			exit 1
		fi
		echo "Preparing the client, then launching outside Gradle so ./dev.sh hotswap can compile while the game is open."
		RUBY_EXPORT_LAUNCH=1 ./gradlew runClient --console=plain "$@"
		echo "Minecraft is starting. In another terminal: ./dev.sh hotswap    or    ./dev.sh watch"
		exec bash build/dev/run-client.sh
		;;
	hotswap)
		./gradlew hotswap --console=plain "$@"
		;;
	watch)
		./gradlew hotswap --continuous --console=plain "$@"
		;;
	status)
		./gradlew hotswap --console=plain -Pruby.hotswap.ping=true
		;;
	build)
		./gradlew build --console=plain "$@"
		;;
	*)
		echo "Usage: ./dev.sh client|hotswap|watch|status|build" >&2
		exit 1
		;;
esac
