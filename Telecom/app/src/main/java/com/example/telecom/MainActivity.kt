package com.example.telecom

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.telecom.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.example.telecom.ui.screens.MainScreen
import com.example.telecom.ui.theme.TelecomTheme
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.text.input.KeyboardType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


// Pantallas para la barra inferior
enum class BottomBarScreen(val label: String, val icon: @Composable () -> Unit) {
    Dialer("Llamar", { Icon(androidx.compose.material.icons.Icons.Default.Call, contentDescription = "Llamar") }),
    Contacts("Contactos", { Icon(androidx.compose.material.icons.Icons.Default.AccountBox, contentDescription = "Contactos") })
}

class MainActivity : ComponentActivity() {
    // Variables para manejar fotos y contactos
    private var currentPhotoPath by mutableStateOf<String?>(null)
    private var tempContactName by mutableStateOf("")
    private var tempContactPhone by mutableStateOf("")
    private var contactPhoto by mutableStateOf<String?>(null)
    private var refreshContacts by mutableStateOf(false)

    private lateinit var telecomManager: TelecomManager

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prepara directorio para fotos
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.takeIf { !it.exists() }?.mkdir()

        // Inicializa TelecomManager y PhoneAccount
        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = PhoneAccountHandle(
            ComponentName(this, MyConnectionService::class.java),
            "TELECOM_1"
        )
        val phoneAccount = PhoneAccount.builder(handle, "MiProveedor")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .build()
        telecomManager.registerPhoneAccount(phoneAccount)

        setContent {
            TelecomTheme {
                AppContent()
            }
        }
    }

    @Composable
    fun AppContent() {
        var currentScreen by rememberSaveable { mutableStateOf(BottomBarScreen.Dialer) }

        // Launcher para permiso y toma de foto
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast
                    .makeText(this, "Permiso CALL_PHONE denegado", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    BottomBarScreen.values().forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    BottomBarScreen.Dialer -> DialerScreen(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                        placeCall = { number -> placeCallSafely(number) }
                    )
                    BottomBarScreen.Contacts -> MainScreen(
                        onSaveContact = { name, phone ->
                            // 1️⃣ guarda datos temporales
                            tempContactName = name
                            tempContactPhone = phone
                            // 2️⃣ bitmap por defecto
                            val bmp = BitmapFactory.decodeResource(
                                resources, R.drawable.contact
                            )
                            // 3️⃣ crea archivo y escribe JPEG
                            val photoFile = try {
                                createImageFile()
                            } catch (e: IOException) {
                                e.printStackTrace(); null
                            }
                            photoFile?.let { file ->
                                FileOutputStream(file).use { out ->
                                    bmp.compress(
                                        Bitmap.CompressFormat.JPEG, 100, out
                                    )
                                }
                            }
                            refreshContacts = !refreshContacts
                        },
                        refreshTrigger = refreshContacts
                    )
                }
            }
        }
    }

    @Composable
    fun DialerScreen(
        onRequestPermission: () -> Unit,
        placeCall: (String) -> Unit
    ) {
        var numero by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = numero,
                onValueChange = { input -> numero = input.filter { it.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text("Número a llamar") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                when (ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.CALL_PHONE
                )) {
                    PackageManager.PERMISSION_GRANTED -> placeCall(numero)
                    else -> onRequestPermission()
                }
            }) {
                Text("Llamar")
            }
        }
    }


    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName =
            "${tempContactPhone}_${tempContactName.replace(' ', '.')}_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                refreshContacts = !refreshContacts
                Toast.makeText(
                    this,
                    "Contacto $tempContactName agregado",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                currentPhotoPath?.let { path -> File(path).delete() }
            }
            contactPhoto = null
            tempContactName = ""
            tempContactPhone = ""
        }
    }


    private fun placeCallSafely(numero: String) {
        val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, numero, null)
        try {
            telecomManager.placeCall(uri, null)
        } catch (se: SecurityException) {
            Toast.makeText(
                this,
                "No se pudo iniciar la llamada: permiso denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

