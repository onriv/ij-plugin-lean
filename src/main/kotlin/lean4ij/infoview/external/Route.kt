package lean4ij.infoview.external

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import lean4ij.infoview.LeanInfoviewColorSettingPage
import lean4ij.infoview.TextAttributesKeys
import lean4ij.infoview.external.data.InfoviewEvent
import lean4ij.lsp.LeanLanguageServer
import lean4ij.lsp.data.RpcCallParamsRaw
import lean4ij.lsp.data.RpcConnectParams
import java.awt.Color
import java.io.File
import java.nio.file.Files

/**
 * copy from https://github.com/ktorio/ktor-samples/blob/main/sse/src/main/kotlin/io/ktor/samples/sse/SseApplication.kt
 * define all routes for external infoview
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun externalInfoViewRoute(project: Project, service : ExternalInfoViewService) : Route.() -> Unit = {

    /**
     * see: https://ktor.io/docs/server-serving-spa.html#serve-customize
     * and https://ktor.io/docs/server-static-content.html
     */
    singlePageApplication {
        useResources = true
        ignoreFiles {
            val pathSegments = it.split(".jar!")
            // this is for handling path in jar file:
            // it seems that plugins are packaged like plugins/.../jar!/index.hmlt
            val path = pathSegments[pathSegments.size-1]
            if (path.startsWith("/assets")) {
                return@ignoreFiles false
            }
            if (path.startsWith("/fonts")) {
                return@ignoreFiles false
            }
            if (path == "/index.html") {
                return@ignoreFiles false
            }
            // TODO remove this
            if (path == "/vite.svg") {
                return@ignoreFiles false
            }
            true
        }
    }
    val scopeIO = CoroutineScope(Dispatchers.IO)

    /**
     * TODO here in fact it can raise exception, and makes the frontend stuck at waiting for server
     * TODO Dont know why, once got:
     * kotlinx.coroutines.channels.ClosedReceiveChannelException: Channel was closed
     * 	at kotlinx.coroutines.channels.BufferedChannel.getReceiveException(BufferedChannel.kt:1729)
     * 	at kotlinx.coroutines.channels.BufferedChannel.resumeWaiterOnClosedChannel(BufferedChannel.kt:2171)
     * 	at kotlinx.coroutines.channels.BufferedChannel.resumeReceiverOnClosedChannel(BufferedChannel.kt:2160)
     * 	at kotlinx.coroutines.channels.BufferedChannel.cancelSuspendedReceiveRequests(BufferedChannel.kt:2153)
     * 	at kotlinx.coroutines.channels.BufferedChannel.completeClose(BufferedChannel.kt:1930)
     * 	at kotlinx.coroutines.channels.BufferedChannel.isClosed(BufferedChannel.kt:2209)
     * 	at kotlinx.coroutines.channels.BufferedChannel.isClosedForSend0(BufferedChannel.kt:2184)
     * 	at kotlinx.coroutines.channels.BufferedChannel.isClosedForSend(BufferedChannel.kt:2181)
     * 	at kotlinx.coroutines.channels.BufferedChannel.completeCloseOrCancel(BufferedChannel.kt:1902)
     * 	at kotlinx.coroutines.channels.BufferedChannel.closeOrCancelImpl(BufferedChannel.kt:1795)
     * 	at kotlinx.coroutines.channels.BufferedChannel.close(BufferedChannel.kt:1754)
     * 	at kotlinx.coroutines.channels.SendChannel$DefaultImpls.close$default(Channel.kt:98)
     * 	at io.ktor.websocket.DefaultWebSocketSessionImpl$runIncomingProcessor$1.invokeSuspend(DefaultWebSocketSession.kt:222)
     * 	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
     * 	at kotlinx.coroutines.DispatchedTaskKt.resume(DispatchedTask.kt:235)
     * 	at kotlinx.coroutines.DispatchedTaskKt.resumeUnconfined(DispatchedTask.kt:191)
     * 	at kotlinx.coroutines.DispatchedTaskKt.dispatch(DispatchedTask.kt:163)
     * 	at kotlinx.coroutines.CancellableContinuationImpl.dispatchResume(CancellableContinuationImpl.kt:474)
     * 	at kotlinx.coroutines.CancellableContinuationImpl.completeResume(CancellableContinuationImpl.kt:590)
     * 	at kotlinx.coroutines.channels.BufferedChannelKt.tryResume0(BufferedChannel.kt:2896)
     * 	at kotlinx.coroutines.channels.BufferedChannelKt.access$tryResume0(BufferedChannel.kt:1)
     * 	at kotlinx.coroutines.channels.BufferedChannel$BufferedChannelIterator.tryResumeHasNext(BufferedChannel.kt:1689)
     * 	at kotlinx.coroutines.channels.BufferedChannel.tryResumeReceiver(BufferedChannel.kt:642)
     * 	at kotlinx.coroutines.channels.BufferedChannel.updateCellSend(BufferedChannel.kt:458)
     * 	at kotlinx.coroutines.channels.BufferedChannel.access$updateCellSend(BufferedChannel.kt:36)
     * 	at kotlinx.coroutines.channels.BufferedChannel.send$suspendImpl(BufferedChannel.kt:3089)
     * 	at kotlinx.coroutines.channels.BufferedChannel.send(BufferedChannel.kt)
     * 	at io.ktor.websocket.RawWebSocketJvm$1.invokeSuspend(RawWebSocketJvm.kt:68)
     * 	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
     * 	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
     * 	at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:173)
     * 	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:166)
     * 	at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:469)
     * 	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:569)
     * 	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:994)
     * 	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
     * 	at io.ktor.server.netty.EventLoopGroupProxy$Companion.create$lambda$1$lambda$0(NettyApplicationEngine.kt:296)
     * 	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
     * 	at java.base/java.lang.Thread.run(Thread.java:840)
     *
     */
    webSocket("/ws") {
        try {
            // send(Frame.Text("connected"))
            val outgoingJob = launch {
                val theme = createThemeCss(EditorColorsManager.getInstance().globalScheme)
                send(Frame.Text(Gson().toJson(InfoviewEvent("updateTheme", mapOf("theme" to theme)))))

                // TODO maybe this should be removed if disconnected for avoiding leak?
                project.messageBus.connect().subscribe<EditorColorsListener>(EditorColorsManager.TOPIC, EditorColorsListener { scheme ->
                    scopeIO.launch {
                        scheme?.let{
                            send(Frame.Text(Gson().toJson(InfoviewEvent("updateTheme", mapOf("theme" to createThemeCss(it))))))
                        }
                    }
                })

                val serverRestarted = service.awaitInitializedResult()
                send(Frame.Text(Gson().toJson(InfoviewEvent("serverRestarted", serverRestarted))))
                service.previousCursorLocation?.let {
                    // This is for showing the goal without moving the cursor at the startup
                    // TODO this should be handled earlier
                    send(Frame.Text(Gson().toJson(InfoviewEvent("changedCursorLocation", it))))
                }
                service.events().collect {
                    send(Frame.Text(Gson().toJson(it)))
                }
            }
            runCatching {
                while (true) {
                    // TODO the original example in https://ktor.io/docs/server-websockets.html#handle-multiple-session
                    //      is using consumeEach, but I am not familiar with it
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        val (requestId, method, data) = frame.readText().split(Regex(","), 3)
                        if (method == "createRpcSession") {
                            launch {
                                val params: RpcConnectParams = fromJson(data)
                                val session = service.getSession(params.uri)
                                val resp = mapOf("requestId" to requestId.toInt(), "method" to "rpcResponse", "data" to session)
                                send(Gson().toJson(resp))
                            }
                        }
                        if (method == "sendClientRequest") {
                            launch {
                                val params: RpcCallParamsRaw = fromJson(data)
                                val ret = service.rpcCallRaw(params)
                                val resp = mapOf("requestId" to requestId.toInt(), "method" to "rpcResponse", "data" to ret)
                                send(Gson().toJson(resp))
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                // TODO handle it seriously
                exception.printStackTrace()
                exception.cause?.printStackTrace()
                exception.cause?.cause?.printStackTrace()
                // kotlinx.coroutines.channels.ClosedReceiveChannelException: Channel was closed
            }.also {
                // TODO check what also means
                outgoingJob.cancel()
            }
            outgoingJob.join()
        } catch (ex: Exception) {
            ex.cause?.cause?.printStackTrace()
            ex.cause?.printStackTrace()
            ex.printStackTrace()
        }
    }

}

/**
 * TODO the vscode prefix is unnecessary?
 */
fun createThemeCss(scheme: EditorColorsScheme) : String {
    val foreground : String = scheme.defaultForeground.toHexRgba()
    val background : String = scheme.defaultBackground.toHexRgba()
    // read temp json, to debug color without restart
    // val themeStr = Files.readString(File("D:/theme.json").toPath())
    // TODO static this, rather than using a map
    val themeStr = """
        {
          "--colorf3f3f3": "DOCUMENTATION_COLOR",
          "--coloradd6ff": "SELECTION_BACKGROUND",
          "--color616161": "DEFAULT_IDENTIFIER.foreground",
          "--vscode-goal-hyp-color": "DEFAULT_INSTANCE_FIELD.foreground",
          "--vscode-goal-inaccessible": "DEFAULT_LINE_COMMENT.foreground"
        }
    """.trimIndent()
    val theme = Gson().fromJson(themeStr, Map::class.java) as Map<String, String>
    val themeSb = StringBuilder()
    theme.forEach { (t, u) ->
        if (!u.contains(".")) {
            val colorKey = ColorKey.find(u)
            themeSb.append("    ${t}: ${scheme.getColor(colorKey)!!.toHexRgba()};")
            return@forEach
        }
        val (keyStr,  attrStr) = u.split(".")
        val attrKey = TextAttributesKey.find(keyStr)
        val attr = scheme.getAttributes(attrKey)
        if (attrStr == "foreground") {
            themeSb.append("    ${t}: ${attr.foregroundColor.toHexRgba()};")
        }
        if (attrStr == "background") {
            themeSb.append("    ${t}: ${attr.backgroundColor.toHexRgba()};")
        }
        themeSb.append("\n")
    }
    return """
        :root {
            $themeSb
            --header-foreground-color: ${scheme.getAttributes(TextAttributesKeys.Header.key).foregroundColor.toHexRgba()};
            --vscode-editor-background: $background;
            --vscode-editor-foreground: $foreground;
        }
    """.trimIndent()
    // --vscode-diffEditor-insertedTextBackground: ${TextAttributesKeys.hexOf(scheme, TextAttributesKeys.InsertedText)};
    // --vscode-diffEditor-removedTextBackground: ${TextAttributesKeys.RemovedText.hexOf(scheme)};
    // --vscode-goal-hyp-color: ${TextAttributesKeys.GoalHyp.hexOf(scheme)};
    // --vscode-goal-inaccessible: ${TextAttributesKeys.GoalInaccessible.hexOf(scheme)};
}

private fun Color.toHexRgba() = "#" + String.format("%08X", rgb).let {
    // The order is different, in css alpha channel is in the last...
    it.substring(2, 8) + it.substring(0, 2)
}

private suspend inline fun <reified T> ApplicationCall.receiveJson(): T {
    return fromJson(receiveText())
}

private suspend fun ApplicationCall.respondJson(a: Any) {
    respond(toJson(a))
}
inline fun <reified T> fromJson(json: String) : T {
    return LeanLanguageServer.gson.fromJson(json, T::class.java)
}

fun toJsonElement(json: String): JsonElement {
    return LeanLanguageServer.gson.fromJson(json, JsonElement::class.java)
}

fun toJson(any: Any): String {
    return LeanLanguageServer.gson.toJson(any)
}