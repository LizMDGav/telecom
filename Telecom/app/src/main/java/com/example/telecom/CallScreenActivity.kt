package com.example.telecom.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.telecom.ui.theme.TelecomTheme

class CallScreenActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelecomTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("En llamada") }) }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Llamando...")
                        Spacer(Modifier.height(24.dp))
                        Row {
                            Button(onClick = { /* silenciaaaar lógica */ }) {
                                Text("Silenciar")
                            }
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = {
                                // lógica para colgaaaar
                            }) {
                                Text("Colgar")
                            }
                        }
                    }
                }
            }
        }
    }
}
