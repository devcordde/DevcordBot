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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.seliba.devcordbot.database

import com.github.seliba.devcordbot.database.Users.experience
import com.github.seliba.devcordbot.database.Users.id
import com.github.seliba.devcordbot.database.Users.level
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * Representation of the user table in the database.
 * @property id The id row of the table, is also the primary key
 * @property level The level row of the table
 * @property experience The experience row of the table
 */
object Users : IdTable<Long>() {
    override val id: Column<EntityID<Long>> = long("id").entityId()
    val level: Column<Int> = integer("level").default(1)
    val experience: Column<Long> = long("experience").default(0L)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
