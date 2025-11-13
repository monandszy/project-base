#!/bin/bash

# A script to build the Docker image.
# Usage: ./docker_build.sh <project_name> <version> <root_project_name>

set -e

PROJECT_NAME=$1
VERSION=$2
ROOT_PROJECT_NAME=${3:-$PROJECT_NAME}

if [ -z "$PROJECT_NAME" ] || [ -z "$VERSION" ]; then
    echo "Usage: $0 <project_name> <version> [root_project_name]"
    exit 1
fi

JAR_PATH="build/libs/${PROJECT_NAME}-${VERSION}.jar"
IMAGE_TAG="${ROOT_PROJECT_NAME}/${PROJECT_NAME}:${VERSION}"

docker build \
    --build-arg "JAR_PATH=${JAR_PATH}" \
    -t "${IMAGE_TAG}" \
    -q \
    -f ./docker/Dockerfile \
    .

echo "Successfully built Docker image: ${IMAGE_TAG}"