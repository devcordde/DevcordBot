# DevcordBot

## setup dev environment
- copy .env.example to .env
- enter at minimum DISCORD_TOKEN and GUILD_ID (and all you want to test)
- start the dev database `> docker-compose -f docker-compose.develop.yml up -d`
- enter the database information
  - DATABASE_HOST="localhost"
  - DATABASE="postgres"
  - DATABASE_USERNAME="postgres"
  - DATABASE_PASSWORD="DevcordRoxx2021"
- enable `pg_trgm` for the database
  - enter `docker exec -it test-db bash` to access the docker image's bash shell
  - switch the user to postgres with `su postgres`
  - enter `psql` to access the postgres database and run `CREATE EXTENSION pg_trgm;` to install the required postgres extension pg_trgm
  - exit the image's bash shell
- the bot is now ready to start

## setup productive environment
- copy .env.example to .env
- enter all properties
- run `docker-compose up -d`

## setup redeploy command
We recommend [this library](https://github.com/adnanh/webhook) for the redeployment of the bot. Simply set up the 
webhook for executing `docker-compose pull && docker-compose up -d` in the directory where you have the 
`docker-compose.yml` and `.env` file. Then enter the webhook URL at `REDEPLOY_HOST` and the Token at `REDEPLOY_TOKEN`.
Example Config:
````json
{
    "id": "redeploy-devcordbot",
    "execute-command": "/path/to/devcordbot/redeploy.sh",
    "command-working-directory": "/path/to/devcordbot",
    "trigger-rule":
    {
      "match":
      {
        "type": "value",
        "value": "YOUR_SECRET_TOKEN",
        "parameter":
        {
          "source": "header",
          "name": "Redeploy-Token"
        }
      }
    }
  }
````
redeploy.sh:
```shell script
#!/bin/sh
docker-compose pull && docker-compose up -d
```

# Discord logger
Some events can be logged to a Discord webhook. To do this, add the following into a `logback.xml`  or point the `LOGGER_CONFIG` to another file:
```xml
    <appender name="Discord" class="com.github.devcordde.devcordbot.util.DiscordWebhookAppender">
        <webhookUrl><!-- <the url> --></webhookUrl>
    </appender>
```
