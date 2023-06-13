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

package com.github.devcordde.devcordbot.commands.general.jdoodle

/**
 * Language definition.
 */
@Suppress("KDocMissingDocumentation")
enum class Language(val lang: String, val code: Int, val humanReadable: String) {

    BASH("bash", 3, "shell"),
    BRAINFUCK("brainfuck", 0, "Brainf**k"),
    C("c", 4, "CLang"),
    COFFEESCRIPT("coffeescript", 3, "CoffeeScript"),
    CPP("cpp14", 4, "C++"),
    CSHARP("csharp", 3, "C#"),
    DART("dart", 3, "Dart"),
    ELIXIR("elixir", 3, "Elixir"),
    GO("go", 3, "Go"),
    HASKELL("haskell", 3, "Haskell"),
    JAVA("java", 3, "Java"),
    KOTLIN("kotlin", 2, "Kotlin"),
    LUA("lua", 2, "Lua"),
    JAVASCRIPT("nodejs", 3, "JavaScript"),
    NODEJS("nodejs", 3, "NodeJS"),
    PHP("php", 3, "PHP"),
    PYTHON("python3", 3, "Python"),
    RUBY("ruby", 3, "Ruby"),
    RUST("rust", 3, "Rust"),
    SCALA("scala", 3, "Scala"),
    SWIFT("swift", 3, "Swift")
}
