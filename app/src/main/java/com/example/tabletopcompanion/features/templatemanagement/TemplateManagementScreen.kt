package com.example.tabletopcompanion.features.templatemanagement

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(templateViewModel: TemplateViewModel = viewModel()) {
    val context = LocalContext.current
    // Observe the templates list from the ViewModel
    val templatesList = templateViewModel.templates

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
                // Stub for import functionality
                val newTemplateName = "New Template ${templatesList.size + 1}"
                templateViewModel.importTemplate(newTemplateName)
                Toast.makeText(context, "Importing '$newTemplateName'...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Import Template")
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
                items(items = templatesList, key = { it }) { templateName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(templateName, style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = {
                                templateViewModel.deleteTemplate(templateName)
                                Toast.makeText(context, "'$templateName' deleted.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
