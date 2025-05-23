package com.example.tabletopcompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tabletop Companion",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = { navController.navigate("userProfile") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("User Profile")
        }

        Button(
            onClick = { navController.navigate("createRoom") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Create Room")
        }

        Button(
            onClick = { navController.navigate("joinRoom") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join Room")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add some space

        Button(
            onClick = { navController.navigate("templateManagement") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Templates")
        }
    }
}
