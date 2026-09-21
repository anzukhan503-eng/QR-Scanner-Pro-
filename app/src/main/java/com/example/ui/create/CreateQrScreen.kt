package com.example.ui.create

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.generator.QrCodeGenerator
import kotlinx.coroutines.launch

enum class QrCreateType(val label: String, val icon: ImageVector) {
    TEXT("Text", Icons.Default.TextFields),
    URL("URL", Icons.Default.Language),
    WIFI("Wi-Fi", Icons.Default.Wifi),
    PHONE("Phone", Icons.Default.Phone),
    EMAIL("Email", Icons.Default.AlternateEmail),
    CONTACT("Contact", Icons.Default.Person),
    SMS("SMS", Icons.Default.Sms)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrScreen(
    onNavigateBack: () -> Unit,
    onSaveToHistory: (qrType: String, content: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    val currentType = QrCreateType.values()[selectedTypeIndex]

    // Form Fields
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }
    var isSecurityExpanded by remember { mutableStateOf(false) }

    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }

    var smsPhone by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    // Generated QR state
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedRawContent by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_qr_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create QR Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("create_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Type Selector Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTypeIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                QrCreateType.values().forEachIndexed { index, type ->
                    Tab(
                        selected = selectedTypeIndex == index,
                        onClick = {
                            selectedTypeIndex = index
                            generatedBitmap = null
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Inputs for Selected Type
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                when (currentType) {
                    QrCreateType.TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Text content") },
                            placeholder = { Text("Enter message, notes, or data...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("input_qr_text"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    QrCreateType.URL -> {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Website URL") },
                            placeholder = { Text("https://example.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_qr_url"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    QrCreateType.WIFI -> {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Network Name (SSID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Wi-Fi Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Security type chips
                        Text(
                            text = "Security Encryption",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WPA", "WEP", "nopass").forEach { sec ->
                                FilterChip(
                                    selected = wifiSecurity == sec,
                                    onClick = { wifiSecurity = sec },
                                    label = { Text(if (sec == "nopass") "None" else sec) }
                                )
                            }
                        }
                    }

                    QrCreateType.PHONE -> {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1 234 567 8900") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    QrCreateType.EMAIL -> {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Recipient Email") },
                            placeholder = { Text("contact@domain.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailSubject,
                            onValueChange = { emailSubject = it },
                            label = { Text("Subject (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailBody,
                            onValueChange = { emailBody = it },
                            label = { Text("Message Body (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    QrCreateType.CONTACT -> {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactOrg,
                            onValueChange = { contactOrg = it },
                            label = { Text("Company / Organization (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    QrCreateType.SMS -> {
                        OutlinedTextField(
                            value = smsPhone,
                            onValueChange = { smsPhone = it },
                            label = { Text("Recipient Phone") },
                            placeholder = { Text("+1 234 567 8900") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = smsMessage,
                            onValueChange = { smsMessage = it },
                            label = { Text("SMS Message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Generate Button
                Button(
                    onClick = {
                        val formattedString: String
                        val displayTitle: String

                        when (currentType) {
                            QrCreateType.TEXT -> {
                                formattedString = textInput.trim()
                                displayTitle = formattedString.take(30)
                            }
                            QrCreateType.URL -> {
                                formattedString = urlInput.trim()
                                displayTitle = formattedString
                            }
                            QrCreateType.WIFI -> {
                                formattedString = QrCodeGenerator.formatWifi(wifiSsid.trim(), wifiPassword.trim(), wifiSecurity)
                                displayTitle = "Wi-Fi: $wifiSsid"
                            }
                            QrCreateType.PHONE -> {
                                formattedString = QrCodeGenerator.formatPhone(phoneInput.trim())
                                displayTitle = "Phone: $phoneInput"
                            }
                            QrCreateType.EMAIL -> {
                                formattedString = QrCodeGenerator.formatEmail(emailInput.trim(), emailSubject.trim(), emailBody.trim())
                                displayTitle = "Email: $emailInput"
                            }
                            QrCreateType.CONTACT -> {
                                formattedString = QrCodeGenerator.formatContact(contactName.trim(), contactPhone.trim(), contactEmail.trim(), contactOrg.trim())
                                displayTitle = "Contact: $contactName"
                            }
                            QrCreateType.SMS -> {
                                formattedString = QrCodeGenerator.formatSms(smsPhone.trim(), smsMessage.trim())
                                displayTitle = "SMS to $smsPhone"
                            }
                        }

                        if (formattedString.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill in the required fields")
                            }
                            return@Button
                        }

                        val bmp = QrCodeGenerator.generateQrBitmap(formattedString)
                        if (bmp != null) {
                            generatedBitmap = bmp
                            generatedRawContent = formattedString
                            onSaveToHistory(currentType.name, formattedString, displayTitle)
                            scope.launch {
                                snackbarHostState.showSnackbar("QR Code generated successfully!")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Could not generate QR code. Content may be too long.")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_qr_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate QR Code",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Generated Output Display Card
                val bitmap = generatedBitmap
                if (bitmap != null) {
                    Spacer(modifier = Modifier.height(28.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_qr_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Generated QR Code",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // High-res QR preview
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action buttons: Save to Gallery, Share, Copy Content
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val saved = QrCodeGenerator.saveBitmapToGallery(context, bitmap)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (saved) "QR Code saved to Pictures/QRScannerPro"
                                                else "Unable to save image"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_qr_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Image")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val uri = QrCodeGenerator.saveBitmapToCache(context, bitmap)
                                        if (uri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_TEXT, generatedRawContent)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("share_qr_image_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("QR Content", generatedRawContent)
                                    clipboard.setPrimaryClip(clip)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Content copied to clipboard")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Content")
                            }
                        }
                    }
                }
            }
        }
    }
}
