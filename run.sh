#!/bin/sh

export COMPOSE_FILE_PATH="${PWD}/target/classes/docker/docker-compose.yml"

if [ -z "${M2_HOME}" ]; then
  export MVN_EXEC="mvn"
else
  export MVN_EXEC="${M2_HOME}/bin/mvn"
fi

start() {
    docker volume create max-version-policy-acs-volume
    docker volume create max-version-policy-db-volume
    docker volume create max-version-policy-ass-volume
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d
}

start_share() {
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d max-version-policy-share
}

start_acs() {
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d max-version-policy-acs
}

down() {
    if [ -f "$COMPOSE_FILE_PATH" ]; then
        docker-compose -f "$COMPOSE_FILE_PATH" down
    fi
}

purge() {
    docker volume rm -f max-version-policy-acs-volume
    docker volume rm -f max-version-policy-db-volume
    docker volume rm -f max-version-policy-ass-volume
}

build() {
    $MVN_EXEC clean package
}

build_share() {
    docker-compose -f "$COMPOSE_FILE_PATH" kill max-version-policy-share
    yes | docker-compose -f "$COMPOSE_FILE_PATH" rm -f max-version-policy-share
    $MVN_EXEC clean package -pl max-version-policy-share,max-version-policy-share-docker
}

build_acs() {
    docker-compose -f "$COMPOSE_FILE_PATH" kill max-version-policy-acs
    yes | docker-compose -f "$COMPOSE_FILE_PATH" rm -f max-version-policy-acs
    $MVN_EXEC clean package -pl max-version-policy-integration-tests,max-version-policy-platform,max-version-policy-platform-docker
}

tail() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs -f
}

tail_all() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs --tail="all"
}

prepare_test() {
    $MVN_EXEC verify -DskipTests=true -pl max-version-policy-platform,max-version-policy-integration-tests,max-version-policy-platform-docker
}

test() {
    $MVN_EXEC verify -pl max-version-policy-platform,max-version-policy-integration-tests
}

case "$1" in
  build_start)
    down
    build
    start
    tail
    ;;
  build_start_it_supported)
    down
    build
    prepare_test
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  reload_share)
    build_share
    start_share
    tail
    ;;
  reload_acs)
    build_acs
    start_acs
    tail
    ;;
  build_test)
    down
    build
    prepare_test
    start
    test
    tail_all
    down
    ;;
  test)
    test
    ;;
  *)
    echo "Usage: $0 {build_start|build_start_it_supported|start|stop|purge|tail|reload_share|reload_acs|build_test|test}"
esac