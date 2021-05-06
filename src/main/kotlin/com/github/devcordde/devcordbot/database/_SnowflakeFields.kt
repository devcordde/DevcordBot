/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
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

package com.github.devcordde.devcordbot.database

import dev.kord.common.entity.Snowflake
import mu.KotlinLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

private val LOG = KotlinLogging.logger {}

/**
 * Adds a column of type [Snowflake] to the table
 *
 * @see SnowflakeColumnType
 */
fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType)

/**
 * Implementation of [ColumnType] which wraps a [Snowflake] in a [Long]
 * @see Snowflake
 * @see Snowflake.value
 */
object SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is Snowflake -> value
            is Long -> Snowflake(value)
            is String -> Snowflake(value)
            else -> {
                LOG.warn { "Unexpected type of snowflake: ${value::class.qualifiedName}" }
                Snowflake(value.toString())
            }
        }
    }

    override fun valueToDB(value: Any?): Any {
        require(value is Snowflake) { "Value has to be Snowflake" }

        return value.value
    }

    override fun valueToString(value: Any?): String = value.toString()

    override fun notNullValueToDB(value: Any): Any = valueToDB(value)
}

/**
 * Shorhand for [Entity<Snowflake>][Entity].
 */
abstract class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

/**
 * Shorhand for [EntityClass<Snowflake>][EntityClass].
 */
abstract class SnowflakeEntityClass<out E : SnowflakeEntity>(table: IdTable<Snowflake>, entityType: Class<E>? = null) :
    EntityClass<Snowflake, E>(table, entityType)
