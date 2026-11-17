#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -f .env ]]; then
    set -a
    source .env
    set +a
fi
: "${VIEWER_KEY:?VIEWER_KEY fehlt}" "${ADMIN_KEY:?ADMIN_KEY fehlt}" "${STATION_KEY:?STATION_KEY fehlt}"
if [[ $VIEWER_KEY == "$ADMIN_KEY" || $VIEWER_KEY == "$STATION_KEY" || $ADMIN_KEY == "$STATION_KEY" ]]; then
    echo "Drei unterschiedliche Schlüssel erforderlich" >&2
    exit 1
fi
./gradlew bootJar
jar=build/libs/floodmonitor.jar
pids=()
stop() {
    if (( ${#pids[@]} )); then
        kill "${pids[@]}" 2>/dev/null || true
        wait "${pids[@]}" 2>/dev/null || true
    fi
}
trap stop EXIT
trap 'exit 130' INT TERM
IFS=, read -ra stations <<< "${STATION_PORTS:-ST-001:8101,ST-002:8102,ST-003:8103,ST-004:8104,ST-005:8105}"
for endpoint in "${stations[@]}"; do
    env -u VIEWER_KEY -u ADMIN_KEY java -jar "$jar" station "${endpoint%:*}" "${endpoint##*:}" &
    pids+=("$!")
done
java -jar "$jar" &
central=$!
pids+=("$central")
wait "$central"
