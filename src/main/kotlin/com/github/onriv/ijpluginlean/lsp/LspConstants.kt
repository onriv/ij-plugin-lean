package com.github.onriv.ijpluginlean.lsp

object LspConstants {
    const val LEAN_LANGUAGE_SERVER_ID = "LeanLanguageServerId"
    const val LEAN_PLAIN_GOAL = "\$/lean/plainGoal"
    const val LEAN_PLAIN_TERM_GOAL = "\$/lean/plainTermGoal"
    const val LEAN_RPC_CONNECT = "\$/lean/rpc/connect"
    const val LEAN_RPC_CALL = "\$/lean/rpc/call"
    const val LEAN_RPC_KEEP_ALIVE = "\$/lean/rpc/keepAlive"
    const val RPC_METHOD_INFO_TO_INTERACTIVE = "Lean.Widget.InteractiveDiagnostics.infoToInteractive"
    const val RPC_METHOD_GET_INTERACTIVE_GOALS = "Lean.Widget.getInteractiveGoals"
    const val FILE_PROGRESS = "\$/lean/fileProgress"
}