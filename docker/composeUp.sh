mkdir -p postgres/datado
#docker volume create --name=pgdata
docker-compose -p data -f compose-data.yml up -d