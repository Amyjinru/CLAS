FROM mysql:8.4.4

COPY database /opt/clas/database

ENTRYPOINT ["/bin/bash", "/opt/clas/database/bootstrap-and-migrate.sh"]
