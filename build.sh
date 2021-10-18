#!/usr/bin/env bash 

echo $GITHUB_PAT1 | docker login ghcr.io -u edevries1 --password-stdin

sbt docker:publish

