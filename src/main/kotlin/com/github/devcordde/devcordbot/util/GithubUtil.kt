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

package com.github.devcordde.devcordbot.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Utility to interact with github gist api
 */
class GithubUtil(private val client: HttpClient) {

    /**
     * Retrieves a list of [GitHubContributor]s, that have contributed to the bot project on GitHub.
     */
    suspend fun retrieveContributors(): List<GitHubContributor> {
        return client.get(API_BASE) {
            url {
                path("repos", "devcordde", "devcordbot", "contributors")
            }
        }
    }

    companion object {
        private val API_BASE = Url("https://api.github.com")
    }
}

/**
 * Representation of a Bot contributor on github.
 *
 * @property name the GitHub username of the contributer
 * @property htmlUrl the url to the contributors GitHub profile
 */
@Serializable
data class GitHubContributor(@SerialName("login") val name: String, @SerialName("html_url") val htmlUrl: String)
