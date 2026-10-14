package com.blackclaw.android.tool.impl

import com.blackclaw.android.knowledge.DirectZimLibrary
import com.blackclaw.android.knowledge.DirectZimReader
import com.blackclaw.android.knowledge.ZimContentIndex
import com.blackclaw.android.knowledge.ZimConsultEngine
import com.blackclaw.android.knowledge.ZimIndexService
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

class ZimSearchTool : BaseTool() {
    override fun getName() = "zim_search"
    override fun getDisplayName() = "Search offline ZIM"
    override fun getDescriptionEN() = "Search titles and, when built, the local full-content index of a .zim archive offline. Returns bounded snippets and never injects an entire archive."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = listOf(
        ToolParameter("query", "string", "Topic or article to search offline.", true),
        ToolParameter("library", "string", "Nombre o ruta del .zim. Opcional si solo existe uno.", false),
        ToolParameter("limit", "integer", "Maximum results, 1-10. Default 5.", false),
        ToolParameter("scope", "string", "auto | title | content. Default auto.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val resolution = DirectZimLibrary.resolve(optionalString(params, "library", ""))
        val file = resolution.file ?: return ToolResult.error(resolution.message)
        val query = requireString(params, "query")
        val limit = optionalInt(params, "limit", 5).coerceIn(1, 10)
        val scope = optionalString(params, "scope", "auto").lowercase()
        if (scope !in setOf("auto", "title", "content")) return ToolResult.error("scope must be auto, title, or content")
        return runCatching {
            val contentAvailable = ZimContentIndex.exists(ClawApplication.instance, file)
            if (scope == "content" && !contentAvailable) {
                return@runCatching ToolResult.error("No auxiliary content index exists yet for ${file.name}. To consult without indexing, use zim_consult; zim_index is an optional fallback.")
            }
            val contentHits = if (scope != "title" && contentAvailable) {
                DirectZimReader(file).use { reader ->
                    ZimContentIndex.open(ClawApplication.instance, file, reader.titleEntryCount).use { it.search(query, limit) }
                }
            } else emptyList()
            DirectZimReader(file).use { reader ->
                val titleHits = if (scope != "content") reader.searchTitles(query, limit) else emptyList()
                val seen = HashSet<String>()
                val rendered = ArrayList<String>()
                contentHits.forEach { hit ->
                    if (seen.add(hit.path)) rendered += "${hit.title} · path=${hit.path}\n   ${hit.snippet}"
                }
                titleHits.forEach { hit ->
                    if (seen.add(hit.path)) rendered += "${hit.title} · path=${hit.path} · title match"
                }
                if (rendered.isEmpty()) ToolResult.success("No matches in ${file.name}.")
                else ToolResult.success(buildString {
                    appendLine("Results from ${reader.libraryInfo()}:")
                    rendered.take(limit).forEachIndexed { i, value -> appendLine("${i + 1}. $value") }
                    append("Source: ${file.name} (local ZIM archive, no internet). Use zim_read with title_or_path.")
                    if (!contentAvailable) append(" To answer questions without indexing the archive, use zim_consult with the main topics.")
                })
            }
        }.getOrElse { ToolResult.error("Could not search ${file.name}: ${it.message}") }
    }
}

class ZimConsultTool : BaseTool() {
    override fun getName() = "zim_consult"
    override fun getDisplayName() = "Query ZIM library"
    override fun getDescriptionEN() = "Consult a local .zim like a reference book without pre-indexing it: select likely articles from the ZIM title index, read only those articles, and return small relevant source-labelled passages. Pass core entities in topics for best retrieval."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = listOf(
        ToolParameter("question", "string", "Specific question to answer using the offline library.", true),
        ToolParameter("topics", "string", "Main entities or topics separated by commas; for example: France, Paris. Recommended.", false),
        ToolParameter("library", "string", "Name or path of the .zim. Optional when only one exists.", false),
        ToolParameter("max_articles", "integer", "Candidate articles to consult, 1-8. Default 5.", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val resolution = DirectZimLibrary.resolve(optionalString(params, "library", ""))
        val file = resolution.file ?: return ToolResult.error(resolution.message)
        val question = requireString(params, "question")
        val topics = optionalString(params, "topics", "")
        return runCatching {
            DirectZimReader(file).use { reader ->
                val excerpts = ZimConsultEngine.consult(
                    reader, question, topics, optionalInt(params, "max_articles", 5),
                )
                if (excerpts.isEmpty()) {
                    ToolResult.success("No candidate chapters found for that query in ${file.name}. Try specifying proper names or main topics in topics.")
                } else ToolResult.success(buildString {
                    appendLine("Offline consultation in ${reader.libraryInfo()}")
                    appendLine("Question: $question")
                    excerpts.forEachIndexed { index, excerpt ->
                        appendLine("\n[${index + 1}] ${excerpt.title} · path=${excerpt.path}")
                        appendLine(excerpt.text)
                    }
                    append("Exclusive source: ${file.name} (local ZIM). Selected excerpts; the archive and full articles were not added to context.")
                })
            }
        }.getOrElse { ToolResult.error("Could not consult ${file.name}: ${it.message}") }
    }
}

class ZimIndexTool : BaseTool() {
    override fun getName() = "zim_index"
    override fun getDisplayName() = "Local ZIM index"
    override fun getDescriptionEN() = "Optional fallback for archives or queries not resolved by zim_consult. Start, resume, rebuild, pause, or inspect a persistent local full-content index."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = listOf(
        ToolParameter("action", "string", "start | resume | rebuild | stop | status", true),
        ToolParameter("library", "string", "Nombre o ruta del .zim. Opcional si solo existe uno.", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val action = requireString(params, "action").lowercase().trim()
        if (action == "stop" || action == "pause" || action == "pausar") {
            ZimIndexService.stop(ClawApplication.instance)
            return ToolResult.success("Pausing the ZIM index. Progress is saved so it can be resumed.")
        }
        val resolution = DirectZimLibrary.resolve(optionalString(params, "library", ""))
        val file = resolution.file ?: return ToolResult.error(resolution.message)
        return when (action) {
            "start", "resume", "reanudar", "rebuild", "reconstruir" -> {
                if (ZimIndexService.isRunning) {
                    ToolResult.success("${ZimIndexService.activePath ?: "A ZIM library"} is already being indexed.")
                } else if (ZimIndexService.start(ClawApplication.instance, file, action in setOf("rebuild", "reconstruir"))) {
                    ToolResult.success("Indexing of ${file.name} started in the background. You can view progress and pause it from the notification.")
                } else ToolResult.error("Android did not allow the indexer to start. Open BlaqClaw and try again.")
            }
            "status", "estado" -> {
                if (!ZimContentIndex.exists(ClawApplication.instance, file)) {
                    ToolResult.success("${file.name}: content index has not been created yet.")
                } else runCatching {
                    DirectZimReader(file).use { reader ->
                        ZimContentIndex.open(ClawApplication.instance, file, reader.titleEntryCount).use { index ->
                            val s = index.status()
                            ToolResult.success("${file.name}: ${s.percent}% · ${s.indexed} articles · ${s.skipped} skipped · " +
                                if (s.complete) "complete" else if (ZimIndexService.isRunning) "indexing" else "paused")
                        }
                    }
                }.getOrElse { ToolResult.error("Could not read status: ${it.message}") }
            }
            else -> ToolResult.error("action must be start, resume, rebuild, stop, or status")
        }
    }
}

class ZimReadTool : BaseTool() {
    override fun getName() = "zim_read"
    override fun getDisplayName() = "Read ZIM article"
    override fun getDescriptionEN() = "Read one article directly from a local .zim archive and return a bounded offline text excerpt with source attribution."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = listOf(
        ToolParameter("title_or_path", "string", "Exact title or path returned by zim_search.", true),
        ToolParameter("library", "string", "Nombre o ruta del .zim. Opcional si solo existe uno.", false),
        ToolParameter("max_chars", "integer", "Maximum characters, 500-12000. Default 5000.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val resolution = DirectZimLibrary.resolve(optionalString(params, "library", ""))
        val file = resolution.file ?: return ToolResult.error(resolution.message)
        return runCatching {
            DirectZimReader(file).use { reader ->
                val article = reader.readArticle(requireString(params, "title_or_path"), optionalInt(params, "max_chars", 5_000).coerceIn(500, 12_000))
                ToolResult.success("Offline article: ${article.title}\nSource: ${file.name} · ${article.path}\n\n${article.text}")
            }
        }.getOrElse { ToolResult.error("Could not read ${file.name}: ${it.message}") }
    }
}
