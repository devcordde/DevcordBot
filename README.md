# DevcordBot

## setup dev environment
- copy .env.example to .env
- enter at minimum DISCORD_TOKEN and GUILD_ID (and all you want to test)
- start the dev database `> docker-compose up -d -f docker-compose.develop.yml`
- enter the database information
  - DATABASE_HOST="localhost"
  - DATABASE="postgres"
  - DATABASE_USERNAME="postgres"
  - DATABASE_PASSWORD=""
  

## setup productive environment
- copy .env.example to .env
- enter all properties
- run `docker-compose up -d`

## setup redeploy command
We recommend [this library](https://github.com/adnanh/webhook) for the redeployment of the bot. Simply setup the 
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