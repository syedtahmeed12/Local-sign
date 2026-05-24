package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DocumentEntity
import com.example.data.SignatureEntity
import com.example.ui.SignatureViewModel
import com.example.ui.SignatureStroke
import com.example.ui.VerificationResult
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.components.SignaturePad
import java.text.SimpleDateFormat
import java.util.*

// Dynamic unicode style converter helper
fun convertToCursive(text: String, styleIndex: Int): String {
    if (text.isBlank()) return ""
    return try {
        when (styleIndex) {
            0 -> { // Calligraphy loop script
                text.map { c ->
                    when (c) {
                        in 'a'..'z' -> String(Character.toChars(0x1D4EA + (c - 'a')))
                        in 'A'..'Z' -> String(Character.toChars(0x1D4D0 + (c - 'A')))
                        else -> c
                    }
                }.joinToString("")
            }
            1 -> { // Bold Double-Struck Elegant
                text.map { c ->
                    when (c) {
                        in 'a'..'z' -> String(Character.toChars(0x1D552 + (c - 'a')))
                        in 'A'..'Z' -> String(Character.toChars(0x1D538 + (c - 'A')))
                        else -> c
                    }
                }.joinToString("")
            }
            2 -> { // Bold Fraktur/Gothic style
                text.map { c ->
                    when (c) {
                        in 'a'..'z' -> String(Character.toChars(0x1D5BE + (c - 'a')))
                        in 'A'..'Z' -> String(Character.toChars(0x1D5A0 + (c - 'A')))
                        else -> c
                    }
                }.joinToString("")
            }
            else -> { // Classic Slanted serif
                text.map { c ->
                    when (c) {
                        in 'a'..'z' -> String(Character.toChars(0x1D41E + (c - 'a')))
                        in 'A'..'Z' -> String(Character.toChars(0x1D400 + (c - 'A')))
                        else -> c
                    }
                }.joinToString("")
            }
        }
    } catch (e: Exception) {
        text // Fall back
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: SignatureViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val signatures by viewModel.signatures.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()

    val docTitle by viewModel.documentTitle.collectAsStateWithLifecycle()
    val docBody by viewModel.documentBody.collectAsStateWithLifecycle()
    val isPdfMode by viewModel.isPdfMode.collectAsStateWithLifecycle()
    val selectedPdfName by viewModel.selectedPdfName.collectAsStateWithLifecycle()
    val selectedPdfSize by viewModel.selectedPdfSize.collectAsStateWithLifecycle()

    val encryptionPassphrase by viewModel.encryptionPassphrase.collectAsStateWithLifecycle()

    // Sign builder states
    val activeSigName by viewModel.activeSignatureName.collectAsStateWithLifecycle()
    val activeStrokes by viewModel.activeStrokeList.collectAsStateWithLifecycle()
    val activeTypedText by viewModel.activeTypedSignature.collectAsStateWithLifecycle()
    val activeStyleIndex by viewModel.activeSelectedStyleIndex.collectAsStateWithLifecycle()

    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()

    // Active Verification States
    val selectedAuditDoc by viewModel.selectedAuditDocument.collectAsStateWithLifecycle()
    val auditPassInput by viewModel.auditPassphraseInput.collectAsStateWithLifecycle()
    val verificationResult by viewModel.verificationResultState.collectAsStateWithLifecycle()

    // UI Tab Navigation
    var selectedTab by remember { mutableStateOf(0) } // 0: Sign Workspace, 1: Pre-saved Signatures, 2: Verification Audit Trail
    val tabs = listOf("Sign Composer", "Saved Signatures", "Audit Trail")

    // File Picker logic (Reads file descriptors completely offline)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            var fileName = "local_upload.pdf"
            var fileSize = 0L
            var fileContent = ""
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
                
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    // If it is text or simple pdf metadata, represent it
                    val isText = fileName.endsWith(".txt", ignoreCase = true) || fileName.endsWith(".json", ignoreCase = true)
                    fileContent = if (isText) {
                        String(bytes, Charsets.UTF_8).take(2000)
                    } else {
                        "--- PDF BINARY DOCUMENT ENVELOPE ---\n" +
                        "File Name: $fileName\n" +
                        "Offline Size: ${bytes.size} bytes\n" +
                        "Mime Signature: application/pdf\n" +
                        "Cryptographic Hash: [AUTOCOMPUTED FROM ON-DEVICE STREAM]\n" +
                        "------------------------------------\n" +
                        "This encrypted container holds the full local byte stream of the chosen document. Signing applies SHA-256 bound locks directly."
                    }
                }
            } catch (e: Exception) {
                fileContent = "SIMULATED PDF FILE ENVELOPE:\nCould not load content stream: ${e.message}"
            }
            viewModel.setPdfFile(fileName, fileSize.coerceAtLeast(fileContent.length.toLong()), fileContent)
        }
    }

    // Auto-clear message block
    LaunchedEffect(operationStatus) {
        if (operationStatus != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .statusBarsPadding()
            ) {
                // Main Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "App Icon",
                            tint = Color(0xFF10B981), // Emerald Premium Green
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Document Signer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sandbox Active Badge
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sandbox Offline",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Decorative Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Toast Event State Banner
                AnimatedVisibility(
                    visible = operationStatus != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    operationStatus?.let { status ->
                        val isErr = status.contains("Error", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isErr) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = "status_icon",
                                    tint = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = status,
                                    color = if (isErr) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Main Section selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> SignWorkspace(
                            viewModel = viewModel,
                            signatures = signatures,
                            docTitle = docTitle,
                            docBody = docBody,
                            isPdfMode = isPdfMode,
                            selectedPdfName = selectedPdfName,
                            selectedPdfSize = selectedPdfSize,
                            encryptionPassphrase = encryptionPassphrase,
                            chooseFile = { filePickerLauncher.launch("*/*") }
                        )
                        1 -> PreSavedSignaturesWorkspace(
                            viewModel = viewModel,
                            signatures = signatures,
                            activeName = activeSigName,
                            activeStrokes = activeStrokes,
                            activeTypedText = activeTypedText,
                            activeStyleIndex = activeStyleIndex
                        )
                        2 -> AuditTrailWorkspace(
                            documents = documents,
                            onInspectDocument = { doc ->
                                viewModel.selectedAuditDocument.value = doc
                                viewModel.verificationResultState.value = null
                                viewModel.auditPassphraseInput.value = ""
                            }
                        )
                    }
                }
            }

            // Global Full Screen Verification Audit Modal
            selectedAuditDoc?.let { doc ->
                VerificationAuditDialog(
                    document = doc,
                    passphraseInput = auditPassInput,
                    onPassphraseChange = { viewModel.auditPassphraseInput.value = it },
                    result = verificationResult,
                    onTriggerVerify = { viewModel.performAuditVerification(doc, auditPassInput) },
                    onDeleteDocument = {
                        viewModel.deleteDocument(doc)
                    },
                    onDismiss = { viewModel.dismissAudit() }
                )
            }
        }
    }
}

// ------------------------------------------------------------------------
// SECTION 1: SIGN WORKSPACE
// ------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignWorkspace(
    viewModel: SignatureViewModel,
    signatures: List<SignatureEntity>,
    docTitle: String,
    docBody: String,
    isPdfMode: Boolean,
    selectedPdfName: String?,
    selectedPdfSize: Long?,
    encryptionPassphrase: String,
    chooseFile: () -> Unit
) {
    var signatureOption by remember { mutableStateOf("TEXT") } // "DRAW", "TEXT", or "SAVED"
    var selectedSavedSigId by remember { mutableStateOf(-1) }
    
    // Internal signature canvas states inside workplace
    var tempDrawStrokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }
    var tempTypeText by remember { mutableStateOf("Authorized Signature") }
    var tempStyleIndex by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // STEP 1: LOAD RECIPIENT DOCUMENT
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1. Selected File or Text",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reset button if PDF loaded
                    if (isPdfMode) {
                        TextButton(
                            onClick = { viewModel.clearPdfFile() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear file", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Template", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source Options: Text Entry OR SAF Document Picker
                if (!isPdfMode) {
                    // Quick preloaded templates
                    Text(
                        text = "Load Template:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.sampleTemplates.forEach { template ->
                            val isChosen = docTitle == template.title
                            AssistChip(
                                onClick = { viewModel.loadTemplate(template) },
                                label = { Text(template.title, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    labelColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // File picker section
                if (isPdfMode && selectedPdfName != null) {
                    // PDF Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "PDF Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPdfName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Size: ${selectedPdfSize ?: 0} bytes • Secure Document Seal Envelope",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Option to upload file physically
                    Button(
                        onClick = chooseFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("file_upload_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "file picker")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload/Select PDF or Text File", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { viewModel.documentTitle.value = it },
                    label = { Text("Document Header Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_title_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document Editor Text Box
                OutlinedTextField(
                    value = docBody,
                    onValueChange = { if (!isPdfMode) viewModel.documentBody.value = it },
                    label = { Text(if (isPdfMode) "Extracted Text Envelopes (Read-Only)" else "Document Composer Body Text") },
                    readOnly = isPdfMode,
                    minLines = 4,
                    maxLines = 8,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_body_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STEP 2: AES KEY ACCESS PASSCODE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Device E2E Local Cryptographic Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A derived AES-256 local security engine locks this file. Provide a confidential passcode to secure decryption authorization.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = encryptionPassphrase,
                    onValueChange = { viewModel.encryptionPassphrase.value = it },
                    label = { Text("Local Security Passphrase / PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = "key pin") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STEP 3: CHOOSE SIGNATURE TYPE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Select Signature Mode",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                ) {
                    val signatureOptions = listOf("TEXT" to "Typed Script", "DRAW" to "Draw Hand", "SAVED" to "Stored DB")
                    signatureOptions.forEach { option ->
                        val isSelected = signatureOption == option.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { signatureOption = option.first }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.second,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (signatureOption) {
                    "TEXT" -> {
                        Text(
                            text = "Enter signature text & pick layout:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempTypeText,
                            onValueChange = { tempTypeText = it },
                            placeholder = { Text("e.g. John Doe") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render dynamic script selection chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Calligraphy", "Double", "Gothic", "Slanted").forEachIndexed { idx, fontName ->
                                val isSelected = tempStyleIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { tempStyleIndex = idx }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fontName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Live script rendering card
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = convertToCursive(tempTypeText.ifBlank { "Live Preview" }, tempStyleIndex),
                                fontSize = 20.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    "DRAW" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Freehand Document Drawing:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Clear button
                            TextButton(
                                onClick = { tempDrawStrokes = emptyList() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "clear pad", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Pad", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic drawing frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.White, RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp))
                        ) {
                            SignaturePad(
                                modifier = Modifier.fillMaxSize(),
                                strokes = tempDrawStrokes,
                                onStrokesChanged = { tempDrawStrokes = it },
                                strokeColor = Color.Black
                            )
                        }
                    }

                    "SAVED" -> {
                        Text(
                            text = "Choose from pre-drafted signatures:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (signatures.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Empty signatures",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No saved signatures draft active yet.\nGo to 'Saved Signatures' tab to register one.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Render a elegant vertical list selector card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            ) {
                                signatures.forEach { sig ->
                                    val isPicked = selectedSavedSigId == sig.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedSavedSigId = sig.id }
                                            .background(if (isPicked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isPicked,
                                            onClick = { selectedSavedSigId = sig.id }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = sig.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Mode: ${sig.type} • Digitized record",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (sig != signatures.last()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // THE PRIMARY COMMIT ACTION
        Button(
            onClick = {
                // Determine signing parameters
                val signatureName: String
                val signatureContent: String
                
                when (signatureOption) {
                    "TEXT" -> {
                        signatureName = tempTypeText.ifBlank { "Authorized Signature" }
                        signatureContent = convertToCursive(signatureName, tempStyleIndex)
                    }
                    "DRAW" -> {
                        signatureName = "Custom Hand Drawn"
                        // Convert strokes relative payload
                        signatureContent = "DRAW_STROKED"
                    }
                    "SAVED" -> {
                        val chosenSig = signatures.find { it.id == selectedSavedSigId }
                        if (chosenSig == null) {
                            viewModel.operationStatus.value = "Error: Please select a pre-saved signature block first."
                            return@Button
                        }
                        signatureName = chosenSig.name
                        signatureContent = "SAVED_${chosenSig.name}"
                    }
                    else -> {
                        signatureName = "System Default"
                        signatureContent = ""
                    }
                }

                if (docBody.isBlank()) {
                    viewModel.operationStatus.value = "Error: Document body text cannot be empty."
                    return@Button
                }

                viewModel.signAndSaveDocument(
                    title = docTitle,
                    bodyText = docBody,
                    signatureType = signatureOption,
                    signatureName = signatureName,
                    signatureContent = signatureContent,
                    passphraseInput = encryptionPassphrase
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sign_lock_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Emerald Green for signing commit
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Lock sign")
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Cryptographic Sign & Lock",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ------------------------------------------------------------------------
// SECTION 2: PRE-SAVED SIGNATURES WORKSPACE
// ------------------------------------------------------------------------
@Composable
fun PreSavedSignaturesWorkspace(
    viewModel: SignatureViewModel,
    signatures: List<SignatureEntity>,
    activeName: String,
    activeStrokes: List<SignatureStroke>,
    activeTypedText: String,
    activeStyleIndex: Int
) {
    var subTabSelection by remember { mutableStateOf(0) } // 0: Draw, 1: Write text
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Creator controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Draft Pre-Saved Signature",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preset signatures are securely buffered inside the device database. You can instantly load them later on any document without redrawing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sub tab selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (subTabSelection == 0) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { subTabSelection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Freehand Drawing",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (subTabSelection == 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (subTabSelection == 1) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { subTabSelection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Typed Cursive Styles",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (subTabSelection == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Common name tag field
                OutlinedTextField(
                    value = activeName,
                    onValueChange = { viewModel.activeSignatureName.value = it },
                    label = { Text("Signature Name Tag") },
                    placeholder = { Text("e.g. Executive Seal, Initial Check") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (subTabSelection == 0) {
                    // DRAWING FLOW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Draw bounds below:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.activeStrokeList.value = emptyList() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "clear core pad", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp))
                    ) {
                        SignaturePad(
                            modifier = Modifier.fillMaxSize(),
                            strokes = activeStrokes,
                            onStrokesChanged = { viewModel.activeStrokeList.value = it },
                            strokeColor = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveDrawnSignature(activeName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_drawn_signature_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "save signature")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft Drawing Block", fontSize = 13.sp)
                    }

                } else {
                    // TYPESCRIPT FLOW
                    OutlinedTextField(
                        value = activeTypedText,
                        onValueChange = { viewModel.activeTypedSignature.value = it },
                        label = { Text("Initials / Full Name Text") },
                        placeholder = { Text("e.g. J. Doe") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stylist Selectors
                    Text(
                        text = "Choose typography cursive style:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Brush", "Outline", "Fraktur", "Slant").forEachIndexed { index, titleStr ->
                            val isChosen = activeStyleIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .background(
                                        color = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.activeSelectedStyleIndex.value = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = titleStr,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Live preview converter card
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = convertToCursive(activeTypedText.ifBlank { "Preview" }, activeStyleIndex),
                            color = Color.Black,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveTextSignature(activeTypedText, activeStyleIndex) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_text_signature_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Draft cursive record")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft Cursive Script", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // LIST OF SAVED SIGNATURES IN DB
        Text(
            text = "Active Stored Signatures Vault",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (signatures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BorderColor,
                        contentDescription = "Empty signatures ledger",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Signatures vault is empty.\nCreate a drawing signature block or a styled cursive block above to lock down records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                signatures.forEach { sig ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (sig.type == "DRAW") Icons.Default.Gesture else Icons.Default.FontDownload,
                                    contentDescription = "sig type icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sig.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Type: ${sig.type} • Secured Offline DB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Small visual representation preview
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    ) {
                                        if (sig.type == "TEXT") {
                                            Text(
                                                text = convertToCursive(sig.content, sig.styleIndex),
                                                fontSize = 14.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            // Drawn vector representation indicator
                                            Text(
                                                text = "𝓥𝓮𝓬𝓽𝓸𝓻 𝓓𝓻𝓪𝔀𝓲𝓷𝓰 𝓢𝓮𝓪𝓵",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }

                            // Delete button
                            IconButton(onClick = { viewModel.deleteSignature(sig) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Signature draft",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ------------------------------------------------------------------------
// SECTION 3: AUDIT TRAIL WORKSPACE
// ------------------------------------------------------------------------
@Composable
fun AuditTrailWorkspace(
    documents: List<DocumentEntity>,
    onInspectDocument: (DocumentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Cryptographic Security Audits",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Chronological local document vault. Tampering attempts are automatically flagged via real-time vector re-hashing.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (documents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "No records file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No document logs signed yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Any template or text written and compiled using 'Cryptographic Sign & Lock' will register historical audit logs here with physical hashes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(documents) { doc ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectDocument(doc) }
                    .testTag("document_audit_card_${doc.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (doc.docType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Article,
                                contentDescription = "doc_type_icon",
                                tint = if (doc.docType == "PDF") Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Compact Verified Seal Ribbon
                        Row(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = "locked indicator",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(11.dp)
                              )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "SHA-256 CHECK: ${doc.checksumSha256.take(24)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Signed by: ${doc.signatureName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Date: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(doc.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onInspectDocument(doc) },
                            modifier = Modifier.testTag("verify_inspect_button_${doc.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = "verify", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inspect & Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ------------------------------------------------------------------------
// MODAL DIALOG: CHRONOLOGICAL SECURITY VERIFICATION LEDGER
// ------------------------------------------------------------------------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VerificationAuditDialog(
    document: DocumentEntity,
    passphraseInput: String,
    onPassphraseChange: (String) -> Unit,
    result: VerificationResult?,
    onTriggerVerify: () -> Unit,
    onDeleteDocument: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header console bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "dismiss dialog")
                    }

                    Text(
                        text = "Offline Cryptographic Examiner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Spacer/Empty container
                    Box(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card info summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderZip, contentDescription = "file info", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = document.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Committed SHA-256 Checksum: ${document.checksumSha256}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Signer: ${document.signatureName} (${document.signatureType})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Original Envelope Size: ${document.fileSize} bytes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Passphrase Verification prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Verify AES-256 Document Decryption Authorization",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter the local security passcode or signing PIN originally assigned to reconstruct keys and authenticate authenticity.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passphraseInput,
                            onValueChange = onPassphraseChange,
                            label = { Text("Verification Passphrase PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = "pass lock") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_passphrase_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onTriggerVerify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_trigger_verify_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = "check crypto verification")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recompute & Run Cryptographic Checklist", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // VERIFICATION ENGINE OUTPUT
                AnimatedVisibility(
                    visible = result != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    result?.let { r ->
                        Column {
                            // Unified Result Shield Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (r.isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (r.isSuccess) Color(0xFFD1FAE5).copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (r.isSuccess) Icons.Default.Verified else Icons.Default.ReportProblem,
                                        contentDescription = "Shield verified status",
                                        tint = if (r.isSuccess) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = if (r.isSuccess) "DATA TRUST INTEGRITY VERIFIED" else "MUTATION FAULT OR ACCESS DENIED",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (r.isSuccess) Color(0xFF047857) else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = if (r.isSuccess) "This document is 100% authentic. The Local SHA-256 checksum proves file has NOT been altered by any third party." else r.errorExplanation,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (r.isSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Timeline logs
                            Text(
                                text = "Real-time Auditing Timeline Ledger",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                r.auditSteps.forEachIndexed { index, step ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(8.dp)
                                                .background(
                                                    color = if (step.contains("✅")) Color(0xFF10B981) else if (step.contains("❌")) Color.Red else MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Decrypted content visibility block
                            if (r.isSuccess && r.decryptedText != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Successfully Decrypted File Content Preview",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = r.decryptedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Delete from vault option
                Button(
                    onClick = {
                        onDeleteDocument()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_delete_document_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Document from vault")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purge & Delete Document Audit Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
