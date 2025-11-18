FIX Message Processing Pipeline

A complete end-to-end system for ingesting, parsing, streaming, storing, and reporting on FIX (Financial Information eXchange) protocol messages.

This project demonstrates a modern distributed architecture using:
-Spring Boot API
-Redis Streams (Producer + Consumer)
-PostgreSQL
-Docker Compose
-Modular Maven Project
-Automated FIX report generation

Takes a log file in /logs and uses apis to process the log into a report which can be found in /reports


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



