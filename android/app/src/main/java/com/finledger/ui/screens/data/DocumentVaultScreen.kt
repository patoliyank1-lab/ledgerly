package com.finledger.ui.screens.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.DocumentType
import com.finledger.domain.util.DateFormatter
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.screens.transactions.TransactionSearchBar
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentVaultScreen(
    onBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<DocumentType?>(null) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingUri = uri
    }

    val filtered = state.documents.filter { doc ->
        (selectedType == null || doc.type == selectedType) &&
            (query.isBlank() || doc.name.contains(query, ignoreCase = true) || doc.notes.contains(query, ignoreCase = true))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Document Vault", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add document")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TransactionSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search receipts, invoices, agreements…",
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Vault is empty",
                    subtitle = "Store receipts, invoices, bank statements, loan agreements, and warranty docs offline.",
                    actionLabel = "Add Document",
                    onAction = { picker.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(filtered, key = { it.id }) { doc ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(doc.id) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${doc.type.displayName} · ${DateFormatter.formatDate(doc.createdAt)} · ${doc.fileSizeBytes / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteDocument(doc.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = FinLedgerTheme.colors.negative)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUri?.let { uri ->
        var name by remember { mutableStateOf("") }
        var docType by remember { mutableStateOf(DocumentType.RECEIPT) }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text("Save to Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Document Name") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        listOf(DocumentType.RECEIPT, DocumentType.INVOICE, DocumentType.LOAN_AGREEMENT, DocumentType.BANK_STATEMENT, DocumentType.OTHER).forEach { t ->
                            FilterChip(selected = docType == t, onClick = { docType = t }, label = { Text(t.displayName) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveDocument(uri, name, docType, notes)
                        pendingUri = null
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pendingUri = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun DocumentViewerScreen(
    documentId: Long,
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val doc by viewModel.observeDocument(documentId).collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(doc?.name ?: "Document", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val current = doc
        if (current == null) {
            Column(modifier = Modifier.padding(padding).padding(Spacing.lg)) {
                Text("Document not found")
            }
            return@Scaffold
        }

        val bitmap = remember(current.fileUri) {
            val file = File(current.fileUri)
            if (!file.exists()) null
            else if (current.mimeType.contains("pdf")) renderFirstPdfPage(file)
            else BitmapFactory.decodeFile(file.absolutePath)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = current.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("Preview unavailable for ${current.mimeType}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun renderFirstPdfPage(file: File): Bitmap? {
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

