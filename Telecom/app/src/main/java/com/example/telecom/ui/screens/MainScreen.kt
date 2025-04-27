package com.example.telecom.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.telecom.R
import com.example.telecom.model.ContactData
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    onSaveContact: (String, String) -> Unit,
    refreshTrigger: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado para los contactos
    var contacts by remember { mutableStateOf<List<ContactData>>(emptyList()) }

    // Estados para los diálogos
    var showImportDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ContactData?>(null) }
    var showContactSelection by remember { mutableStateOf(false) }

    // Estados para campos manuales
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }

    // Estados para permisos
    val callPermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)
    val contactsPermissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    // Cargar contactos al inicio y cuando cambia el refreshTrigger
    LaunchedEffect(refreshTrigger) {
        contacts = loadContactsFromStorage(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Contactos") },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar contacto")
                    }
                }
            )
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            EmptyContactsView { showImportDialog = true }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        onCallClick = {
                            when {
                                callPermissionState.status.isGranted -> {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_CALL,
                                            Uri.parse("tel:${contact.phoneNumber}")
                                        )
                                    )
                                }
                                callPermissionState.status.shouldShowRationale -> {
                                    Toast.makeText(
                                        context,
                                        "Se necesita permiso para realizar llamadas",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    callPermissionState.launchPermissionRequest()
                                }
                                else -> {
                                    callPermissionState.launchPermissionRequest()
                                }
                            }
                        },
                        onDeleteClick = { showDeleteDialog = contact }
                    )
                    Divider()
                }
            }
        }

        // Diálogo para seleccionar modo de importación
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Agregar contacto") },
                text = { Text("¿Cómo deseas agregar el contacto?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showImportDialog = false
                            if (contactsPermissionState.status.isGranted) {
                                showContactSelection = true
                            } else {
                                contactsPermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Text("Importar de contactos")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportDialog = false
                            showManualDialog = true
                        }
                    ) {
                        Text("Ingresar manualmente")
                    }
                }
            )
        }

        // Diálogo para selección de contactos del dispositivo
        if (showContactSelection) {
            var importedContacts by remember { mutableStateOf<List<ContactData>>(emptyList()) }

            LaunchedEffect(Unit) {
                importedContacts = importDeviceContacts(context)
            }

            AlertDialog(
                onDismissRequest = { showContactSelection = false },
                title = { Text("Seleccionar contacto") },
                text = {
                    LazyColumn {
                        items(importedContacts) { contact ->
                            TextButton(
                                onClick = {
                                    manualName = contact.name
                                    manualPhone = contact.phoneNumber
                                    showContactSelection = false
                                    showManualDialog = true
                                }
                            ) {
                                Text(contact.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showContactSelection = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo para ingreso manual
        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = { Text("Nuevo contacto") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = manualPhone,
                            onValueChange = { manualPhone = it },
                            label = { Text("Teléfono") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualName.isBlank() || manualPhone.isBlank()) {
                                scope.launch {
                                    Toast.makeText(
                                        context,
                                        "Por favor completa todos los campos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                onSaveContact(manualName, manualPhone)

                                showManualDialog = false
                                manualName = ""
                                manualPhone = ""
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showManualDialog = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de confirmación para eliminar
        showDeleteDialog?.let { contact ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Eliminar contacto") },
                text = { Text("¿Eliminar a ${contact.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            contact.photoUri?.let { uri ->
                                if (!uri.startsWith("content://")) {
                                    File(uri).delete()
                                }
                            }
                            contacts = contacts.filterNot { it.id == contact.id }
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = null }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ContactItem(
    contact: ContactData,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onCallClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = contact.photoUri ?: R.drawable.contact,
                contentDescription = "Foto de ${contact.name}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar contacto",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyContactsView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No hay contactos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onAddClick) {
            Text("Agregar primer contacto")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun importDeviceContacts(context: Context): List<ContactData> {
    return try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null
        )?.use { cursor ->
            val contacts = mutableListOf<ContactData>()

            val idColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (cursor.moveToNext()) {
                val contact = ContactData(
                    id = if (idColumn != -1) cursor.getLong(idColumn).toString() else UUID.randomUUID().toString(),
                    name = if (nameColumn != -1) cursor.getString(nameColumn) else "",
                    phoneNumber = if (numberColumn != -1) {
                        cursor.getString(numberColumn)?.replace("[^0-9]".toRegex(), "") ?: ""
                    } else "",
                    photoUri = if (photoColumn != -1) cursor.getString(photoColumn) else null
                )

                if (contact.name.isNotBlank() && contact.phoneNumber.isNotBlank()) {
                    contacts.add(contact)
                }
            }

            contacts.distinctBy { it.phoneNumber }
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e("ContactImport", "Error al importar contactos", e)
        emptyList()
    }
}

private fun loadContactsFromStorage(context: Context): List<ContactData> {
    val contacts = mutableListOf<ContactData>()
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    storageDir?.listFiles()?.forEach { file ->
        val fileName = file.nameWithoutExtension
        val parts = fileName.split("_")
        if (parts.size >= 2) {
            val phone = parts[0]
            val name = parts[1].replace(".", " ")
            contacts.add(ContactData(name = name, phoneNumber = phone, photoUri = file.absolutePath))
        }
    }

    return contacts.sortedBy { it.name }
}