/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.fromJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.proxy.CommonProxy
import java.io.IOException
import java.net.Proxy
import java.net.URI
import kotlin.reflect.KClass

private const val CLOUDFLARE_BASE_URL = "https://minecraftdev.org/versions/"
private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/minecraft-dev/minecraftdev.org/master/versions/"

val PLATFORM_VERSION_LOGGER = logger<PlatformVersion>()

suspend fun getVersionSelector(type: PlatformType): PlatformVersion {
    val versionJson = type.versionJson ?: throw UnsupportedOperationException("Incorrect platform type: $type")
    return getVersionJson(versionJson)
}

suspend inline fun <reified T : Any> getVersionJson(path: String): T {
    return getVersionJson(path, T::class)
}

suspend fun <T : Any> getVersionJson(path: String, type: KClass<T>): T {
    val text = getText(path)
    try {
        return Gson().fromJson(text, type)
    } catch (e: Exception) {
        val attachment = Attachment("JSON Document", text)
        attachment.isIncluded = true
        PLATFORM_VERSION_LOGGER.error("Failed to parse JSON document from '$path'", e, attachment)
        throw e
    }
}

suspend fun getText(path: String): String {
    return try {
        // attempt cloudflare
        doCall(CLOUDFLARE_BASE_URL + path)
    } catch (e: IOException) {
        PLATFORM_VERSION_LOGGER.warn("Failed to reach cloudflare URL ${CLOUDFLARE_BASE_URL + path}", e)
        // if that fails, attempt github
        try {
            doCall(GITHUB_BASE_URL + path)
        } catch (e: IOException) {
            PLATFORM_VERSION_LOGGER.warn("Failed to reach fallback GitHub URL ${GITHUB_BASE_URL + path}", e)
            throw e
        }
    }
}

private suspend fun doCall(urlText: String): String {
    val manager = FuelManager()
    manager.proxy = selectProxy(urlText)

    return manager.get(urlText)
        .header("User-Agent", "github_org/minecraft-dev/${PluginUtil.pluginVersion}")
        .header("Accepts", "application/json")
        .suspendable()
        .awaitString()
}

fun selectProxy(urlText: String): Proxy? {
    val uri = URI(urlText)
    val url = uri.toURL()

    val proxies = CommonProxy.getInstance().select(uri)
    for (proxy in proxies) {
        try {
            url.openConnection(proxy)
            return proxy
        } catch (_: IOException) {}
    }
    return null
}

data class PlatformVersion(var versions: List<String>, var selectedIndex: Int)
