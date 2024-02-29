#!/bin/bash

# Avvia Jetty
service jetty9 start

# Avvia OpenJDK 17
java -jar /app/geoserver/start.jar &

# Mantieni il container in esecuzione
tail -f /dev/null
