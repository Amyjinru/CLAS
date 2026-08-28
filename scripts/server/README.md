# CLAS server operations

The server entrypoint is `scripts/server/clas`. On the cloud server it is installed
as `/usr/local/bin/clas`.

Common commands:

```bash
clas deploy
clas restart
clas status
clas logs
clas health
```

Runtime configuration lives in `/etc/clas/clas.env`. The default values match the
course demo server:

```bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=clas
MYSQL_USER=root
MYSQL_PASSWORD=123456
AMAP_WEB_SERVICE_KEY=
```

`clas deploy` performs these steps:

1. Install or refresh systemd and nginx configuration.
2. Build the Vue frontend.
3. Build the Spring Boot backend jar.
4. Apply `database/migration-*.sql` files in sorted order.
5. Restart the backend service and reload nginx.
6. Verify `/api/health` locally through backend and nginx.
