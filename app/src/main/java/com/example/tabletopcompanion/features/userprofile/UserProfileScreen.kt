package com.example.tabletopcompanion.features.userprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tabletopcompanion.data.UserProfileRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen() {
    val context = LocalContext.current
    val userProfileRepository = remember { UserProfileRepository(context) }

    var currentUsername by remember { mutableStateOf(userProfileRepository.getUsername() ?: "") }
    var textFieldUsername by remember { mutableStateOf(userProfileRepository.getUsername() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current Username: $currentUsername",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = textFieldUsername,
            onValueChange = { textFieldUsername = it },
            label = { Text("Enter Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                userProfileRepository.saveUsername(textFieldUsername)
                currentUsername = textFieldUsername
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Username")
        }
    }
}
