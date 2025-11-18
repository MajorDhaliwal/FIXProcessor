To run the containers where the docker-compose.yml file is:

chmod +x start.sh
./start.sh

To access postgres using pgadmin:
http://localhost:5050

username: admin@local.com
password: admin

If a password prompts to access the database, it is 'postgres'. (No quotes)

PGAdmin automatically connects with the database using /pgadmin/servers.json

To view the APIs via Swagger:
http://localhost:8080/swagger-ui/index.html#/

Reports will be read and generated in /reports



