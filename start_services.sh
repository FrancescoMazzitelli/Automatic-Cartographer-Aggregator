#!/bin/bash

# Avvia Redis in background
redis-server &

# Avvia GeoServer in background
cd /home/geoserver && java -jar start.jar &

# Avvia l'applicazione gateway
java -jar /home/gateway/geomesa-gateway.jar --spring.config.location=file:/home/gateway/application.properties

# Mantieni il container in esecuzione
tail -f /dev/null
