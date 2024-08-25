package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.project.LeanProjectService
import java.util.concurrent.CompletableFuture
import javax.swing.BorderFactory
import javax.swing.JEditorPane

/**
 * check :https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
 * for some cleaner way to write ui stuff
 */
class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {
    private val goals = JEditorPane()
    val editor : CompletableDeferred<EditorEx> = CompletableDeferred()

    /**
     * This si for displaying popup expr
     */
    val popupEditor : CompletableDeferred<EditorEx> = CompletableDeferred()
    val project = toolWindow.project
    val leanProject = project.service<LeanProjectService>()
    init {
        leanProject.scope.launch(Dispatchers.EDT) {
            try {
                editor.complete(createEditor())
            } catch (ex: Throwable) {
                // TODO should here log?
                editor.completeExceptionally(ex)
            }
            try {
                popupEditor.complete(createEditor())
            } catch (ex: Throwable) {
                // TODO should here log?
                popupEditor.completeExceptionally(ex)
            }
        }
    }

    private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

    // TODO
    private fun render(map: Map<*, *>): String {
        for (g in map["goals"] as List<*>) {
            return ""
        }
        return ""
    }

    /**
     * check the following links about thread and ui
     * - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009458040-Error-writing-data-in-a-tree-provided-by-a-background-thread
     * - https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html
     * Once it's not update but now the method revalidate and updateUI seem not required now.
     */
    fun updateGoal(goal: String) {
        leanProject.scope.launch {
            editor.await().document.setText(goal)
            goals.text = goal
        }
    }

    /**
     * create an editorEx for rendering the info view
     * **this is only for EDT**, create it using
     */
    private fun createEditor(): EditorEx {
        val editor = EditorFactory.getInstance()
            .createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
        // val editor = editorTextField.getEditor(true)!!
        with(editor.settings) {
            isRightMarginShown = false
            isLineNumbersShown = false
            isLineMarkerAreaShown = false
            isRefrainFromScrolling = true
            isCaretRowShown = false
            isUseSoftWraps = true
            setGutterIconsShown(false)
            additionalLinesCount = 0
            additionalColumnsCount = 1
            isFoldingOutlineShown = false
            isVirtualSpace = false
        }
        editor.headerComponent = null
        editor.setCaretEnabled(false)
        editor.setHorizontalScrollbarVisible(false)
        editor.setVerticalScrollbarVisible(true)
        editor.isRendererMode = true
        return editor
    }

    private var mouseMotionListener : EditorMouseMotionListener? = null
    /**
     *  // TODO this should add some UT for the rendering
     */
    suspend fun updateEditorMouseMotionListener(interactiveInfo: String, file: VirtualFile, logicalPosition: LogicalPosition,
                                                interactiveGoals: InteractiveGoals?, interactiveTermGoal : InteractiveTermGoal?) {
        val editorEx: EditorEx = editor.await()
        editorEx.document.setText(interactiveInfo)
        val support = EditorHyperlinkSupport.get(editorEx)
        setContent(editorEx.component)
        // TODO does it require new object for each update?
        //      it seems so, otherwise the hyperlinks seems mixed and requires remove
        //      but still, maybe only one is better, try to remove old hyperlinks
        //      check if multiple editors would leak or not
        if (mouseMotionListener != null) {
            editorEx.removeEditorMouseMotionListener(mouseMotionListener!!)
        }
        mouseMotionListener = InfoviewMouseMotionListener(leanProject.scope, this, support, file, logicalPosition, interactiveGoals, interactiveTermGoal)
        editorEx.addEditorMouseMotionListener(mouseMotionListener!!)
    }

}