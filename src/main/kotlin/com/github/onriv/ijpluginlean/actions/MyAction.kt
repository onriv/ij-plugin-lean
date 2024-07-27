package com.github.onriv.ijpluginlean.actions

// import com.github.onriv.ijpluginlean.listeners.EditorCaretListener
// import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
// import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
// import com.github.onriv.ijpluginlean.lsp.LeanLspServerSupportProvider
// import com.github.onriv.ijpluginlean.lsp.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.PlainGoalParams
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.lsp.data.RpcCallParams
import com.github.onriv.ijpluginlean.project.LspService
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.ConcurrentHashMap

class MyCustomAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Display a dialog with 'Hello World!'
        Messages.showMessageDialog("Hello World!", "Greeting", Messages.getInformationIcon())
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData("editor") as? Editor
        if (e.project == null || editor == null) {
            return
        }
        val caret: Caret = editor.caretModel.primaryCaret
        val offset = caret.offset
        // println("Cursor moved to offset: $offset")
    }
}

class OpenLeanInfoView : AnAction() {
    private val sessions = ConcurrentHashMap<String, String>()

    override fun actionPerformed(e: AnActionEvent) {
        // println("TODO")
    }

    private var processed : Boolean = false

    override fun update(e: AnActionEvent) {
        // TODO dont know if it's a good position add it here or nos
        // TODO real log
        // e.project?.let { EditorCaretListener.register(it) }
        // println("TODO")

        e.project?.let {
            fileProcess(it)
            println(it.service<ExternalInfoViewService>())
        }

    }

    @Synchronized
    fun fileProcess(project: Project) {
        if (processed) {
            return
        }
        BuildWindowManager.getInstance(project).fileProgress()
        processed = true
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    //
//    private fun updateInfoView(project: Project, file: VirtualFile, caret: Caret) {
//        val lspServer = LspServerManager.getInstance(project)
//            .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull()
//        if (lspServer == null) {
//            return
//        }
//        sessions.computeIfAbsent(file.toString()) {k -> connectRpc(k) }
//
//        })
//
//    }
//
//    private fun connectRpc(k: String) : String {
//
//    }
//
//
//    override fun actionPerformed(e: AnActionEvent) {
//        try {
//            val currentFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
//            e.project?.let { project -> currentFile?.let {file ->
////                runBlocking {
////                    rpcConnect(project, file).collect { value ->
////                        sessions[file.toString()] = value
////                    }
////                }
//
//                val lspServer = LspServerManager.getInstance(project)
//                    .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull()
//                if (lspServer != null) {
//
//                    val st = SemanticTokensParams(TextDocumentIdentifier(tryFixWinUrl(file.url)))
//                    val resp1 = lspServer.sendRequestSync { (it as LeanLanguageServer).semanticTokensFull(st) }
//                    println(resp1)
//
//                    val st2 = DocumentSymbolParams(TextDocumentIdentifier(tryFixWinUrl(file.url)))
//                    val resp2 = lspServer.sendRequestSync { (it as LeanLanguageServer).documentSymbol(st2) }
//                    println(resp2)
//
//                    val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcConnect(RpcConnectParams(
//                        tryFixWinUrl(file.url)
//                    )) }
//                    resp?.let{r ->
//                        sessions[file.toString()] = r.sessionId
//                    }
//                }
//                rcpCall(project, file)
//            }}
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//
//    // copy from https://github.com/huggingface/llm-intellij/blob/main/src/main/kotlin/co/huggingface/llmintellij/LlmLsCompletionProvider.kt#L42
//    // TODO better way and pos for this?
//    fun rpcConnect(project: Project, file: VirtualFile): Flow<String> = channelFlow {
//        val lspServer = LspServerManager.getInstance(project)
//            .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull()
//        if (lspServer != null) {
//            val resp = lspServer.sendRequest { (it as LeanLanguageServer).rpcConnect(RpcConnectParams(
//                tryFixWinUrl(file.url)
//            )) }
//            resp?.let{r ->
//                send(r.sessionId)
//            }
//        }
//        awaitClose()
//    }
//
//    fun rcpCall(project: Project, file: VirtualFile) {
//        val lspServer = LspServerManager.getInstance(project)
//            .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull()
//        if (lspServer != null) {
//            sessions[file.url]?.let {s->
//                var textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
//                val editor = FileEditorManager.getInstance(project).selectedTextEditor
//                val caretOffset = editor?.caretModel?.offset
//                val logicalPosition = caretOffset?.let { editor.offsetToLogicalPosition(it) }
//                val line = logicalPosition?.line
//                val column = logicalPosition?.column
//                // TODO handle all null possible...
//                // val position = Position(line=line!!, character = column!!)
//                val position = Position(line=line!!, character = 0)
//                val rpcParams = RpcCallParams(
//                    sessionId = s.toString(),
//                    method = "Lean.Widget.getInteractiveGoals",
//                    params = PlainGoalParams(
//                        textDocument = textDocument,
//                        position = position
//                    ),
//                    textDocument = textDocument,
//                    position = position
//                )
//                val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcCall(rpcParams) }
//                resp?.let{r ->
//                    println(r)
//                }
//                val rpcParams1 = RpcCallParams(
//                    sessionId = s.toString(),
//                    method = "Lean.Widget.getInteractiveTermGoal",
//                    params = PlainGoalParams(
//                        textDocument = textDocument,
//                        position = position
//                    ),
//                    textDocument = textDocument,
//                    position = position
//                )
//                val resp1 = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcCall(rpcParams1) }
//                resp1?.let{r ->
//                    println(r)
//                }
//                val resp2 = lspServer.sendRequestSync { (it as LeanLanguageServer).leanPlainGoal(PlainGoalParams(
//                        textDocument = textDocument,
//                        position = position
//                    )) }
//                resp2?.let{r ->
//                    println(r)
//                }
//            }
//        }
//    }
//
//    fun tryFixWinUrl(url: String) : String {
//        if (!isWindows()) {
//            return url
//        }
//        // this is for windows ...
//        // TODO check it in linux/macos
//        // lean lsp server is using lowercase disk name
//        // TODO this is so ugly, make it better
//        return "file:///"+url.substring(7,8).lowercase() +url.substring(8).replaceFirst(":", "%3A")
//    }
//
//    fun detectOperatingSystem(): String {
//        val osName = System.getProperty("os.name").lowercase()
//
//        return when {
//            "windows" in osName -> "Windows"
//            listOf("mac", "nix", "sunos", "solaris", "bsd").any { it in osName } -> "*nix"
//            else -> "Other"
//        }
//    }
//
//    fun isWindows() : Boolean {
//        return detectOperatingSystem() == "Windows";
//    }
//
}