package com.example.telecom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import com.example.telecom.ui.screens.MainScreen
import com.example.telecom.ui.theme.LlamadasTheme
import com.example.telecom.ui.theme.TelecomTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    // Variables para mantener el estado temporal
    private var currentPhotoPath by mutableStateOf<String?>(null)
    private var tempContactName by mutableStateOf("")
    private var tempContactPhone by mutableStateOf("")
    private var contactPhoto by mutableStateOf<String?>(null)

    // Estado compartido para forzar la actualización
    private var refreshContacts by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.let { dir ->
            if (!dir.exists()) {
                dir.mkdir()
            }
        }

        setContent {
            LlamadasTheme {
                MainScreen(
                    onSaveContact = { name, phone ->
                        // 1️⃣ guarda nombre y teléfono
                        tempContactName  = name
                        tempContactPhone = phone

                        // 2️⃣ convierte el drawable en bitmap
                        val bmp = BitmapFactory.decodeResource(resources, R.drawable.contact)

                        // 3️⃣ crea un archivo igual que en dispatchTakePictureIntent()
                        val photoFile = try {
                            createImageFile()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            null
                        }

                        // 4️⃣ escribe el JPEG al disco
                        photoFile?.let { file ->
                            FileOutputStream(file).use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            }
                        }

                        // 5️⃣ dispara el refresh para que loadContactsFromStorage lo recoja
                        refreshContacts = !refreshContacts
                    },
                    refreshTrigger = refreshContacts
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also { file ->
            currentPhotoPath = file.absolutePath
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "${tempContactPhone}_${tempContactName.replace(" ", ".")}_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Foto tomada correctamente - actualizamos el estado para refrescar
                    refreshContacts = !refreshContacts

                    // Mostramos confirmación
                    Toast.makeText(
                        this,
                        "Contacto ${tempContactName} agregado",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Usuario canceló - borramos el archivo temporal
                    currentPhotoPath?.let { path ->
                        File(path).delete()
                    }
                }

                // Reseteamos valores temporales
                contactPhoto = null
                tempContactName = ""
                tempContactPhone = ""
            }
        }
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}