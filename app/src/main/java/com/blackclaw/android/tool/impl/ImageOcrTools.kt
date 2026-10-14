package com.blackclaw.android.tool.impl

import android.net.Uri
import com.blackclaw.android.perception.ImageOcr
import com.blackclaw.android.perception.ReceiptParser
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolRegistry
import com.blackclaw.android.tool.ToolResult

/**
 * Read text from a photo/image via on-device OCR (ML Kit). The image is passed
 * by content Uri or file path (the chat attach flow provides the Uri).
 */
class OcrImageTool : BaseTool() {
    override fun getName() = "ocr_image"
    override fun getDisplayName() = "Read image"
    override fun getDescriptionEN() =
        "Extract text from a photo/image using on-device OCR. Provide 'uri' (content://…) or " +
        "'path' (/storage/…). Use to read documents, menus, signs, screenshots the user shares. " +
        "Returns the recognized text."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "reads text from a photo/image with on-device OCR"
    override fun getParameters() = listOf(
        ToolParameter("uri", "string", "Image content Uri (content://…).", false),
        ToolParameter("path", "string", "Image file path (/storage/…).", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val uri = optionalString(params, "uri", "")
        val path = optionalString(params, "path", "")
        val text = when {
            uri.isNotBlank() -> ImageOcr.recognizeUri(Uri.parse(uri))
            path.isNotBlank() -> ImageOcr.recognizeFile(path)
            else -> return ToolResult.error("Provide the image 'uri' or 'path'.")
        }
        return if (text.isBlank()) ToolResult.success("No text was detected in the image.")
        else ToolResult.success("Detected text:\n${text.take(6000)}")
    }
}

/**
 * Scan a receipt photo: OCR + extract the total amount and merchant, and log it
 * as an expense in the assistant hub finance.
 */
class ScanReceiptTool : BaseTool() {
    override fun getName() = "scan_receipt"
    override fun getDisplayName() = "Scan receipt"
    override fun getDescriptionEN() =
        "Scan a receipt photo: OCR it, extract the total and merchant, and log it as an expense. " +
        "Provide 'uri' or 'path' of the photo. Optional 'category'. Returns what was logged."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "scans a receipt photo and records the expense automatically"
    override fun getParameters() = listOf(
        ToolParameter("uri", "string", "Receipt image Uri.", false),
        ToolParameter("path", "string", "Receipt image path.", false),
        ToolParameter("category", "string", "Optional expense category.", false),
        ToolParameter("log", "boolean", "Log it to finances (default true). False = just read it.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val uri = optionalString(params, "uri", "")
        val path = optionalString(params, "path", "")
        val text = when {
            uri.isNotBlank() -> ImageOcr.recognizeUri(Uri.parse(uri))
            path.isNotBlank() -> ImageOcr.recognizeFile(path)
            else -> return ToolResult.error("Provide the receipt 'uri' or 'path'.")
        }
        if (text.isBlank()) return ToolResult.error("Could not read the receipt. Try better lighting or focus.")

        val receipt = ReceiptParser.parse(text)
        val amount = receipt.amount
            ?: return ToolResult.success("I read the receipt but could not find a clear total. Text:\n${text.take(800)}")
        val merchant = receipt.merchant?.takeIf { it.isNotBlank() } ?: "Receipt"
        val log = optionalBoolean(params, "log", true)
        if (!log) {
            return ToolResult.success("Receipt: $merchant — total ${"%.2f".format(amount)} (not recorded).")
        }
        val category = optionalString(params, "category", "compras")
        val r = ToolRegistry.getInstance().executeTool("assistant_finance", mapOf(
            "description" to merchant,
            "amount" to -kotlin.math.abs(amount),  // expense = negative
            "category" to category,
        ))
        return if (r.isSuccess)
            ToolResult.success("🧾 Receipt recorded: $merchant, ${"%.2f".format(amount)} (expense in $category).")
        else ToolResult.success("I read the receipt ($merchant, ${"%.2f".format(amount)}) but could not record it: ${r.error}")
    }
}
