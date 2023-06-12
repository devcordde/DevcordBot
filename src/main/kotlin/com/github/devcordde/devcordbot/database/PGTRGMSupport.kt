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

import org.jetbrains.exposed.sql.*

/**
 * The Similar operator from [https://www.postgresql.org/docs/9.6/pgtrgm.html#PGTRGM-OP-TABLE](pg_tgrm).
 */
class SimilarOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "%")

/**
 * Expression builder function for [SimilarOp].
 * @see SimilarOp
 */
infix fun <S1, S2> ExpressionWithColumnType<S1>.similar(other: Expression<S2>): Op<Boolean> {
    return SimilarOp(this, other)
}

/**
 * Expression builder function for [SimilarOp].
 * @param pattern the pattern to check similarity to
 * @see SimilarOp
 */
infix fun <S1> ExpressionWithColumnType<S1>.similar(pattern: String): Op<Boolean> =
    similar(textParam(pattern))

/**
 * Invokes the tgrm similarity function on the [expression] with [other].
 */
fun <S1, S2> similarity(
    expression: ExpressionWithColumnType<S1>,
    other: Expression<S2>
): Expression<String?> = CustomStringFunction("similarity", expression, other)

/**
 * Invokes the UPPER function on [expression].
 */
fun <S1> upper(
    expression: ExpressionWithColumnType<S1>
): ExpressionWithColumnType<String?> = CustomStringFunction("UPPER", expression)

/**
 * Invokes the tgrm similarity function on the [expression] with [pattern].
 */
fun <S1> similarity(
    expression: ExpressionWithColumnType<S1>,
    pattern: String
): Expression<String?> = similarity(expression, textParam(pattern))

private fun textParam(text: String): ExpressionWithColumnType<String?> = CustomStringFunction("text", stringParam(text))
