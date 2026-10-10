package com.roxelcode.app.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class FileNode(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long = 0L
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()
}

object FileManager {

    fun listFiles(context: Context, folderUri: Uri): List<FileNode> {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        return folder.listFiles()
            .map { f ->
                FileNode(
                    name = f.name ?: "unknown",
                    uri = f.uri,
                    isDirectory = f.isDirectory,
                    size = f.length()
                )
            }
            .sortedWith(
                compareByDescending<FileNode> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun readText(context: Context, fileUri: Uri): String {
        return try {
            val input = context.contentResolver.openInputStream(fileUri) ?: return ""
            val reader = BufferedReader(InputStreamReader(input))
            val sb = StringBuilder()
            reader.useLines { lines ->
                lines.forEachIndexed { i, line ->
                    if (i > 0) sb.append("\n")
                    sb.append(line)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            "// Ошибка чтения: ${e.message}"
        }
    }

    fun writeText(context: Context, fileUri: Uri, content: String): Boolean {
        return try {
            val output = context.contentResolver.openOutputStream(fileUri, "wt") ?: return false
            OutputStreamWriter(output).use { it.write(content) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createFile(context: Context, folderUri: Uri, name: String, mime: String = "text/plain"): Uri? {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        return folder.createFile(mime, name)?.uri
    }

    fun createFolder(context: Context, folderUri: Uri, name: String): Uri? {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        return folder.createDirectory(name)?.uri
    }

    fun delete(context: Context, uri: Uri): Boolean {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return false
        return doc.delete()
    }

    fun rename(context: Context, uri: Uri, newName: String): Boolean {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return false
        return doc.renameTo(newName)
    }

    fun isTextFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in listOf(
            "txt", "md", "py", "js", "ts", "html", "htm", "css", "scss",
            "json", "xml", "yaml", "yml", "kt", "java", "c", "cpp", "h",
            "cs", "go", "rs", "php", "rb", "swift", "sh", "bat", "ps1",
            "sql", "lua", "dart", "r", "m", "gradle", "properties", "ini",
            "cfg", "conf", "log", "csv", "tsv"
        )
    }
}
