package com.example.tabletopcompanion.features.templatemanagement

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(templateViewModel: TemplateViewModel = viewModel()) {
    val context = LocalContext.current
    // Observe the templates list from the ViewModel
    val templatesList = templateViewModel.templates

    val importTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            templateViewModel.importTemplate(context, it)
            // Optionally, show a toast or message that import is in progress
            Toast.makeText(context, "Importing template...", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Manage Game Templates",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = {
                importTemplateLauncher.launch("application/zip")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Import Template (.zip)")
        }

        if (templatesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No templates available. Import some!")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = templatesList, key = { it.templateId }) { metadata ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(metadata.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Author: ${metadata.author ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Version: ${metadata.templateVersion}", style = MaterialTheme.typography.bodyMedium)
                            Text("Players: ${metadata.playerCountDescription}", style = MaterialTheme.typography.bodyMedium)
                            metadata.description?.let {
                                Text("Description: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    templateViewModel.deleteTemplate(metadata.templateId)
                                    Toast.makeText(context, "'${metadata.name}' deleted.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
