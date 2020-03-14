/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.seliba.devcordbot.migrator;

import com.github.seliba.devcordbot.database.DevCordUser;
import com.github.seliba.devcordbot.database.Users;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.exposed.sql.Database;
import org.jetbrains.exposed.sql.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Migrator {

    private static final Map<Long, Long> userXp = new HashMap<>();
    private static final Map<Long, Long> userLevel = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Migrator.class);

    public static void main(@NotNull String[] args) {
        if (args.length != 1 || !args[0].endsWith(".toml")) {
            throw new IllegalArgumentException("Toml file has to be provided as argument");
        }

        var env = Dotenv.load();
        connectToDatabase(env);

        TomlData tomlData = new TomlData(args[0].substring(0, args[0].lastIndexOf('.')));
        tomlData.getEntries().forEach((key, value) -> {
            // In Datenbank tun
            if (key.endsWith(".xp")) {
                userXp.put(Long.parseLong(key.substring(0, key.lastIndexOf('.'))), (Long) value);
            } else if (key.endsWith(".level")) {
                userLevel.put(Long.parseLong(key.substring(0, key.lastIndexOf('.'))), (Long) value);
            }
        });

        KotlinHelper.delete();

        userXp.forEach((id, xp) -> {
           var level = userLevel.get(id);
           if (level == null) {
               logger.warn("User {} konnte nicht geladen werden", id);
               return;
           }
           KotlinHelper.transaction(() -> {
               var user = KotlinHelper.createUser(id, (devCordUser) -> {
                   devCordUser.setExperience(xp);
                   devCordUser.setLevel(level.intValue());
                   return Unit.INSTANCE;
               });
               logger.info("Der User(id={}, level={}, xp={}) mit Level wurde erstellt", id, level, xp);
           });
        });
    }

    private static void connectToDatabase(Dotenv env) {
        var config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s/%s", env.get("DATABASE_HOST"), env.get("DATABASE")));
        config.setUsername(env.get("DATABASE_USERNAME"));
        config.setPassword(env.get("DATABASE_PASSWORD"));
        var dataSource = new HikariDataSource(config);
        KotlinHelper.connectToDatabase(dataSource);
        KotlinHelper.transaction(() -> {
            SchemaUtils.INSTANCE.createMissingTablesAndColumns(new Users[] { Users.INSTANCE }, false);
        });
        logger.info("Erfolgreich mit der Datenbank verbunden!");
    }

}
