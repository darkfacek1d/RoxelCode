package com.roxelcode.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxelcode.app.editor.BuiltInLanguages
import com.roxelcode.app.editor.CustomLanguageStorage
import com.roxelcode.app.editor.LanguageDef
import com.roxelcode.app.editor.SyntaxHighlighter
import com.roxelcode.app.files.FileManager
import com.roxelcode.app.files.FileNode
import com.roxelcode.app.ui.theme.RoxelCodeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val OWNER = "darkfacek1d"

data class OpenTab(
    val name: String,
    val uri: Uri?,
    val language: LanguageDef?,
    var content: String,
    var modified: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoxelCodeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(13f) }
    var activeLine by remember { mutableStateOf(1) }
    var highlightEnabled by remember { mutableStateOf(true) }

    var projectUri by remember { mutableStateOf<Uri?>(null) }
    var projectName by remember { mutableStateOf("Нет проекта") }
    val filesInFolder = remember { mutableStateListOf<FileNode>() }

    val tabs = remember { mutableStateListOf<OpenTab>() }
    var activeTabIndex by remember { mutableStateOf(-1) }

    var customLanguages by remember { mutableStateOf<List<LanguageDef>>(emptyList()) }
    LaunchedEffect(Unit) {
        customLanguages = CustomLanguageStorage.load(context)
    }

    fun allLanguages(): List<LanguageDef> = BuiltInLanguages.ALL + customLanguages

    fun detectLanguage(name: String): LanguageDef? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return allLanguages().firstOrNull { it.extensions.contains(ext) }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            projectUri = uri
            projectName = uri.lastPathSegment?.substringAfterLast(':') ?: "Проект"
            scope.launch {
                val list = withContext(Dispatchers.IO) {
                    FileManager.listFiles(context, uri)
                }
                filesInFolder.clear()
                filesInFolder.addAll(list)
                snackbarHostState.showSnackbar("Открыто: ${list.size} файлов")
            }
        }
    }

    fun openFileInTab(node: FileNode) {
        val existingIdx = tabs.indexOfFirst { it.uri == node.uri }
        if (existingIdx >= 0) {
            activeTabIndex = existingIdx
            return
        }
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                FileManager.readText(context, node.uri)
            }
            tabs.add(
                OpenTab(
                    name = node.name,
                    uri = node.uri,
                    language = detectLanguage(node.name),
                    content = text
                )
            )
            activeTabIndex = tabs.size - 1
            activeLine = 1
        }
    }

    fun closeTab(idx: Int) {
        if (idx < 0 || idx >= tabs.size) return
        tabs.removeAt(idx)
        activeTabIndex = when {
            tabs.isEmpty() -> -1
            idx >= tabs.size -> tabs.size - 1
            else -> idx
        }
    }

    fun saveActive() {
        val tab = tabs.getOrNull(activeTabIndex) ?: return
        val uri = tab.uri ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                FileManager.writeText(context, uri, tab.content)
            }
            if (ok) {
                tab.modified = false
                snackbarHostState.showSnackbar("Сохранено: ${tab.name}")
            } else {
                snackbarHostState.showSnackbar("Ошибка сохранения")
            }
        }
    }

    RoxelCodeTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF14141F).copy(alpha = 0.98f),
                    drawerContentColor = Color.White
                ) {
                    DrawerContent(
                        projectName = projectName,
                        highlightEnabled = highlightEnabled,
                        onOpenProject = {
                            folderPicker.launch(null)
                            scope.launch { drawerState.close() }
                        },
                        onToggleHighlight = {
                            highlightEnabled = !highlightEnabled
                        },
                        onOpenLanguages = {
                            showLanguages = true
                            scope.launch { drawerState.close() }
                        },
                        onClose = { scope.launch { drawerState.close() } },
                        onAbout = {
                            showAbout = true
                            scope.launch { drawerState.close() }
                        },
                        onSearch = {
                            showSearch = true
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0A0A0F),
                                Color(0xFF14141F),
                                Color(0xFF1A1A2E)
                            )
                        )
                    )
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                            GlassTopBar(
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onRunClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Запуск...")
                                    }
                                },
                                onSaveClick = { saveActive() },
                                onSearchClick = { showSearch = !showSearch },
                                onHighlightClick = {
                                    highlightEnabled = !highlightEnabled
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (highlightEnabled) "Подсветка вкл"
                                            else "Подсветка выкл"
                                        )
                                    }
                                },
                                highlightEnabled = highlightEnabled,
                                onMoreClick = { showMenu = true },
                                showMenu = showMenu,
                                onDismissMenu = { showMenu = false },
                                onMenuAction = { action ->
                                    showMenu = false
                                    when (action) {
                                        "settings" -> scope.launch {
                                            snackbarHostState.showSnackbar("Настройки (скоро)")
                                        }
                                        "about" -> showAbout = true
                                        "forum" -> scope.launch {
                                            snackbarHostState.showSnackbar("Форум (скоро)")
                                        }
                                        "languages" -> showLanguages = true
                                    }
                                }
                            )
                            if (showSearch) {
                                SearchBar(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onClose = {
                                        showSearch = false
                                        searchQuery = ""
                                    }
                                )
                            }

                            if (tabs.isNotEmpty()) {
                                GlassTabBar(
                                    tabs = tabs,
                                    activeIndex = activeTabIndex,
                                    onTabClick = { activeTabIndex = it },
                                    onTabClose = { closeTab(it) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        Row(Modifier.fillMaxSize().weight(1f)) {
                            GlassFileExplorer(
                                files = filesInFolder,
                                hasProject = projectUri != null,
                                activeUri = tabs.getOrNull(activeTabIndex)?.uri,
                                onFileClick = { node ->
                                    if (!node.isDirectory) openFileInTab(node)
                                },
                                onOpenProject = { folderPicker.launch(null) }
                            )
                            GlassCodeEditor(
                                tab = tabs.getOrNull(activeTabIndex),
                                fontSize = fontSize,
                                activeLine = activeLine,
                                highlightEnabled = highlightEnabled,
                                onLineClick = { activeLine = it },
                                onContentChange = { new ->
                                    if (activeTabIndex >= 0) {
                                        tabs[activeTabIndex].content = new
                                        tabs[activeTabIndex].modified = true
                                    }
                                }
                            )
                        }
                        GlassBottomBar(
                            fileName = tabs.getOrNull(activeTabIndex)?.name ?: "—",
                            language = tabs.getOrNull(activeTabIndex)?.language?.name ?: "Plain",
                            line = activeLine,
                            totalLines = (tabs.getOrNull(activeTabIndex)?.content ?: "").split("\n").size,
                            chars = (tabs.getOrNull(activeTabIndex)?.content ?: "").length,
                            fontSize = fontSize,
                            highlightEnabled = highlightEnabled,
                            onFontIncrease = { if (fontSize < 24f) fontSize += 1f },
                            onFontDecrease = { if (fontSize > 9f) fontSize -= 1f }
                        )
                    }
                }
            }
        }

        if (showAbout) {
            GlassAboutDialog(onDismiss = { showAbout = false })
        }

        if (showLanguages) {
            LanguagesDialog(
                customLanguages = customLanguages,
                onDismiss = { showLanguages = false },
                onAdd = { lang ->
                    val updated = customLanguages + lang
                    customLanguages = updated
                    CustomLanguageStorage.save(context, updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Добавлен язык: ${lang.name}")
                    }
                },
                onDelete = { lang ->
                    val updated = customLanguages - lang
                    customLanguages = updated
                    CustomLanguageStorage.save(context, updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Удалён: ${lang.name}")
                    }
                }
            )
        }
    }
}

@Composable
fun GlassTopBar(
    onMenuClick: () -> Unit,
    onRunClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHighlightClick: () -> Unit,
    highlightEnabled: Boolean,
    onMoreClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
        )
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Меню", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("RoxelCode", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("by $OWNER", color = Color(0xFF8B5CF6), fontSize = 10.sp)
            }
            IconButton(onClick = onHighlightClick) {
                Icon(
                    Icons.Default.Palette, "Подсветка",
                    tint = if (highlightEnabled) Color(0xFF8B5CF6) else Color.White.copy(0.4f)
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Поиск", tint = Color.White.copy(0.9f))
            }
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Save, "Сохранить", tint = Color.White.copy(0.9f))
            }
            IconButton(onClick = onRunClick) {
                Icon(Icons.Default.PlayArrow, "Запуск", tint = Color(0xFF8B5CF6))
            }
            Box {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, "Ещё", tint = Color.White.copy(0.9f))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu,
                    modifier = Modifier.background(Color(0xFF1E1E2E).copy(alpha = 0.98f))
                ) {
                    DropdownMenuItem(
                        text = { Text("Свои языки", color = Color.White) },
                        onClick = { onMenuAction("languages") }
                    )
                    DropdownMenuItem(
                        text = { Text("Настройки", color = Color.White) },
                        onClick = { onMenuAction("settings") }
                    )
                    DropdownMenuItem(
                        text = { Text("Форум", color = Color.White) },
                        onClick = { onMenuAction("forum") }
                    )
                    DropdownMenuItem(
                        text = { Text("О приложении", color = Color.White) },
                        onClick = { onMenuAction("about") }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp)
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, "Поиск", tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Найти в файле...", color = Color.White.copy(0.4f), fontSize = 14.sp)
                    }
                    inner()
                }
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Закрыть", tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun GlassTabBar(
    tabs: List<OpenTab>,
    activeIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp)
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs.size) { idx ->
                val tab = tabs[idx]
                val isActive = idx == activeIndex
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0f,
                    animationSpec = tween(220),
                    label = "tabAlpha"
                )
                Row(
                    modifier = Modifier.height(40.dp)
                        .background(if (isActive) Color.White.copy(alpha = 0.09f) else Color.Transparent)
                        .clickable { onTabClick(idx) }
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.width(3.dp).height((14 + 4 * alpha).dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = alpha), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tab.name + if (tab.modified) " *" else "",
                        color = if (isActive) Color.White else Color.White.copy(0.55f),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Close, "Закрыть",
                        tint = Color.White.copy(0.5f),
                        modifier = Modifier.size(16.dp).clickable { onTabClose(idx) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.08f)))
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    projectName: String,
    highlightEnabled: Boolean,
    onOpenProject: () -> Unit,
    onToggleHighlight: () -> Unit,
    onOpenLanguages: () -> Unit,
    onClose: () -> Unit,
    onAbout: () -> Unit,
    onSearch: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("RoxelCode", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Mobile IDE by $OWNER", color = Color(0xFF8B5CF6), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(10.dp)
        ) {
            Column {
                Text("ПРОЕКТ", color = Color.White.copy(0.4f), fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(projectName, color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        DrawerItem(Icons.Default.Folder, "Открыть проект") { onOpenProject() }
        DrawerItem(
            Icons.Default.Palette,
            if (highlightEnabled) "Подсветка: ВКЛ" else "Подсветка: ВЫКЛ"
        ) { onToggleHighlight() }
        DrawerItem(Icons.Default.Code, "Свои языки") { onOpenLanguages() }
        DrawerItem(Icons.Default.Save, "Сохранить всё") { onClose() }
        DrawerItem(Icons.Default.Search, "Найти в файлах") { onSearch() }
        DrawerItem(Icons.Default.Settings, "Настройки") { onClose() }
        DrawerItem(Icons.Default.Info, "О приложении") { onAbout() }
        Spacer(Modifier.weight(1f))
        Text("v0.2.0 • darkfacek1d", color = Color.White.copy(0.4f), fontSize = 11.sp)
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White.copy(0.9f), fontSize = 15.sp)
    }
}

@Composable
fun GlassFileExplorer(
    files: List<FileNode>,
    hasProject: Boolean,
    activeUri: Uri?,
    onFileClick: (FileNode) -> Unit,
    onOpenProject: () -> Unit
) {
    Box(
        modifier = Modifier.width(190.dp).fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("ПРОЕКТ", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (!hasProject) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onOpenProject() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, "Открыть", tint = Color(0xFF8B5CF6), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Открыть папку", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Выбери папку с кодом",
                            color = Color.White.copy(0.5f),
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (files.isEmpty()) {
                Text("Папка пуста", color = Color.White.copy(0.4f), fontSize = 12.sp)
            } else {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    files.forEach { node ->
                        val isActive = node.uri == activeUri
                        Row(
                            Modifier.fillMaxWidth()
                                .background(
                                    if (isActive) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onFileClick(node) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (node.isDirectory) Icons.Default.Folder else Icons.Default.Code,
                                node.name,
                                tint = if (isActive) Color(0xFF8B5CF6)
                                else if (node.isDirectory) Color(0xFF3B82F6)
                                else Color.White.copy(0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                node.name,
                                color = if (isActive) Color.White else Color.White.copy(0.8f),
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCodeEditor(
    tab: OpenTab?,
    fontSize: Float,
    activeLine: Int,
    highlightEnabled: Boolean,
    onLineClick: (Int) -> Unit,
    onContentChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
        )
        if (tab == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Code, "Код", tint = Color.White.copy(0.2f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Открой папку и выбери файл", color = Color.White.copy(0.4f), fontSize = 13.sp)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tab.name, color = Color.White.copy(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (tab.modified) {
                        Spacer(Modifier.width(4.dp))
                        Text("*", color = Color(0xFF8B5CF6), fontSize = 16.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (tab.language != null) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tab.language.name, color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(0.08f))
                Spacer(Modifier.height(8.dp))

                val lines = tab.content.split("\n")
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    lines.forEachIndexed { index, line ->
                        val lineNum = index + 1
                        val isActive = lineNum == activeLine
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (isActive) Color(0xFF8B5CF6).copy(alpha = 0.10f) else Color.Transparent)
                                .clickable { onLineClick(lineNum) }
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                "$lineNum".padStart(3),
                                color = if (isActive) Color(0xFF8B5CF6) else Color.White.copy(0.3f),
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (fontSize + 6).sp,
                                modifier = Modifier.width((fontSize * 2.4f).dp)
                            )
                            val annotated = SyntaxHighlighter.highlight(
                                line.ifEmpty { " " },
                                tab.language,
                                highlightEnabled
                            )
                            Text(
                                annotated,
                                color = SyntaxHighlighter.DEFAULT_COLOR,
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (fontSize + 6).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBottomBar(
    fileName: String,
    language: String,
    line: Int,
    totalLines: Int,
    chars: Int,
    fontSize: Float,
    highlightEnabled: Boolean,
    onFontIncrease: () -> Unit,
    onFontDecrease: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp).background(Color.Black.copy(alpha = 0.30f))) {
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent)
                )
            )
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fileName.ifEmpty { "—" }, color = Color.White.copy(0.7f), fontSize = 11.sp, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("Ln $line / $totalLines", color = Color.White.copy(0.55f), fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Text("$chars", color = Color.White.copy(0.55f), fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(0.06f))
                    .clickable { onFontDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Text("A-", color = Color.White.copy(0.8f), fontSize = 11.sp)
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(0.06f))
                    .clickable { onFontIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Text("A+", color = Color.White.copy(0.8f), fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(language, color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassAboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ОК", color = Color(0xFF8B5CF6))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("RoxelCode", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("v0.2.0", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
            }
        },
        text = {
            Column {
                Text("Владелец: darkfacek1d", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Мобильная IDE для Android. Подсветка синтаксиса для 20+ языков, свои языки, работа с реальными файлами.",
                    color = Color.White.copy(0.75f),
                    fontSize = 13.sp
                )
            }
        },
        containerColor = Color(0xFF1E1E2E).copy(alpha = 0.96f)
    )
}

@Composable
fun LanguagesDialog(
    customLanguages: List<LanguageDef>,
    onDismiss: () -> Unit,
    onAdd: (LanguageDef) -> Unit,
    onDelete: (LanguageDef) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    if (showAddForm) {
        AddLanguageDialog(
            onDismiss = { showAddForm = false },
            onSave = { lang ->
                onAdd(lang)
                showAddForm = false
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { showAddForm = true }) {
                Icon(Icons.Default.Add, "Добавить", tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Добавить", color = Color(0xFF8B5CF6))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color.White.copy(0.7f))
            }
        },
        title = {
            Column {
                Text("Свои языки", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Встроенных: ${BuiltInLanguages.ALL.size} • Своих: ${customLanguages.size}",
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (customLanguages.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Code, "Языки", tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Пока нет своих языков", color = Color.White.copy(0.5f), fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Нажми «Добавить», чтобы создать", color = Color.White.copy(0.4f), fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn {
                        items(customLanguages.size) { idx ->
                            val lang = customLanguages[idx]
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(lang.keywordColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        lang.name.take(1).uppercase(),
                                        color = lang.keywordColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(lang.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        ".${lang.extensions.joinToString(", .")} • ${lang.keywords.size} слов",
                                        color = Color.White.copy(0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { onDelete(lang) }) {
                                    Icon(Icons.Default.Delete, "Удалить", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E2E).copy(alpha = 0.96f)
    )
}

@Composable
fun AddLanguageDialog(
    onDismiss: () -> Unit,
    onSave: (LanguageDef) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var extensions by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var lineComment by remember { mutableStateOf("//") }
    var keywordColorHex by remember { mutableStateOf("C586C0") }
    var stringColorHex by remember { mutableStateOf("CE9178") }
    var numberColorHex by remember { mutableStateOf("B5CEA8") }
    var commentColorHex by remember { mutableStateOf("6A9955") }

    fun parseColor(hex: String): Color {
        return try {
            Color(("FF" + hex.trim().removePrefix("#")).toLong(16))
        } catch (e: Exception) {
            Color(0xFFC586C0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val lang = LanguageDef(
                            id = "custom_${name.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}",
                            name = name,
                            extensions = extensions.split(",").map { it.trim().removePrefix(".") }.filter { it.isNotBlank() },
                            keywords = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            keywordColor = parseColor(keywordColorHex),
                            stringColor = parseColor(stringColorHex),
                            numberColor = parseColor(numberColorHex),
                            commentColor = parseColor(commentColorHex),
                            functionColor = parseColor(keywordColorHex),
                            lineComment = lineComment,
                            isCustom = true
                        )
                        onSave(lang)
                    }
                }
            ) {
                Text("Сохранить", color = Color(0xFF8B5CF6))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.White.copy(0.7f))
            }
        },
        title = {
            Text("Новый язык", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState())
            ) {
                FormField("Название", name) { name = it }
                FormField("Расширения (через запятую)", extensions) { extensions = it }
                FormField("Символ комментария", lineComment) { lineComment = it }
                Spacer(Modifier.height(8.dp))
                Text("Ключевые слова (через запятую):", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.06f))
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            if (keywords.isEmpty()) {
                                Text("if, else, for, while, return...", color = Color.White.copy(0.35f), fontSize = 12.sp)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Цвета (hex без #):", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(parseColor(keywordColorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text("Ключевые слова", color = Color.White.copy(0.8f), fontSize = 12.sp, modifier = Modifier.width(120.dp))
                    FormFieldSmall(keywordColorHex) { keywordColorHex = it }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(parseColor(stringColorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text("Строки", color = Color.White.copy(0.8f), fontSize = 12.sp, modifier = Modifier.width(120.dp))
                    FormFieldSmall(stringColorHex) { stringColorHex = it }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(parseColor(numberColorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text("Числа", color = Color.White.copy(0.8f), fontSize = 12.sp, modifier = Modifier.width(120.dp))
                    FormFieldSmall(numberColorHex) { numberColorHex = it }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(parseColor(commentColorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text("Комментарии", color = Color.White.copy(0.8f), fontSize = 12.sp, modifier = Modifier.width(120.dp))
                    FormFieldSmall(commentColorHex) { commentColorHex = it }
                }
            }
        },
        containerColor = Color(0xFF1E1E2E).copy(alpha = 0.98f)
    )
}

@Composable
fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(0.06f))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("...", color = Color.White.copy(0.35f), fontSize = 13.sp)
                    }
                    inner()
                }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun FormFieldSmall(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.width(90.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(0.06f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(Color(0xFF8B5CF6)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
