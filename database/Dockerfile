FROM mysql:8.0

WORKDIR /seed
COPY *.sql ./
COPY deploy-entrypoint.sh /usr/local/bin/uniwiki-db-deploy
RUN sed -i 's/\r$//' /usr/local/bin/uniwiki-db-deploy \
    && chmod +x /usr/local/bin/uniwiki-db-deploy

ENTRYPOINT ["/usr/local/bin/uniwiki-db-deploy"]
