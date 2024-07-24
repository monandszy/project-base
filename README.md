# project-base
Initial base for my java projects

in essence, an experimental mess

Gradle Tasks:
generateBaseCompose (generates a compose file based on a template)
composeUp -Ppname=<dockerProjectName> (compose-<projectName>.yml)
composeDown -Ppname=<dockerProjectName> (compose-<projectName>.yml)
composeBaseUp (runs observability and data)
composeBaseDown
stopContainer -Ppname=<dockerProjectName> -Ppcontainer=<serviceName>
runTunnel (runs run_tunnel.sh)

Docker Modules Tasks:
moduleUp -Ppprofile=<dev|prod>
extractLayers (extracts dependencies)
docker (creates docker image)

Git Flow Wrapper Tasks:
featureStart -Pbranch=<name>
featureFinish -Pbranch=<name>
releaseStart
releaseFinish
hotfixStart -Pbranch=<name>
hotfixFinish -Pbranch=<name>

printVersion