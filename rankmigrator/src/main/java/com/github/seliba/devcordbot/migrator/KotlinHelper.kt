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

package com.github.seliba.devcordbot.migrator

import com.github.seliba.devcordbot.database.DevCordUser
import com.github.seliba.devcordbot.database.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import javax.sql.DataSource
import org.jetbrains.exposed.sql.transactions.transaction as kotlinTransaction

/**
 * A class which provides useful Kotlin methods for Java classes
 */
object KotlinHelper {
    /**
     * Connect to the database with a [DataSource]
     * @param dataSource The [DataSource] which should be used for the connection
     */
    @JvmStatic
    fun connectToDatabase(dataSource: DataSource): Database = Database.connect(dataSource)

    /**
     * Runs a transaction
     * @param runnable The [Runnable] which should be executed
     */
    @JvmStatic
    fun transaction(runnable: Runnable): Unit = kotlinTransaction { runnable.run() }

    /**
     * Create a new [DevCordUser] by the [id] and apply [init] to it
     * @param id The id of the [DevCordUser]
     * @param init The initial statement to apply
     */
    @JvmStatic
    fun createUser(id: Long, init: DevCordUser.() -> Unit): DevCordUser = DevCordUser.new(id, init)

    /**
     * Removes all [Users] from the database
     */
    @JvmStatic
    fun delete(): Int = kotlinTransaction { Users.deleteAll() }
}