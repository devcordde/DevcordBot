/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http//www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.seliba.devcordbot.util.jdoodle

/**
 * Language definition.
 */
@Suppress("KDocMissingDocumentation")
enum class Language(val langString: String, val codeInt: Int) {

    BASH("bash", 2),
    SH("bash", 2),
    BRAINFUCK("brainfuck", 0),
    BF("brainfuck", 0),
    C("c", 3),
    COFFEESCRIPT("coffeescript", 2),
    CPP("cpp14", 2),
    CS("csharp", 2),
    CSHARP("csharp", 2),
    DART("dart", 2),
    ELIXIR("elixir", 2),
    GOLANG("go", 2),
    GO("go", 2),
    HS("haskell", 0),
    HASKELL("haskell", 0),
    JAVA("java", 2),
    KOTLIN("kotlin", 1),
    LUA("lua", 1),
    JAVASCRIPT("nodejs", 2),
    JS("nodejs", 2),
    NODE("nodejs", 2),
    PASCAL("pascal", 2),
    PERL("perl", 2),
    PHP("php", 2),
    PYTHON("python3", 2),
    PY("python3", 2),
    RUBY("ruby", 2),
    RUST("rust", 2),
    SCALA("scala", 2),
    SQL("sql", 2),
    SWIFT("swift", 2)
}
