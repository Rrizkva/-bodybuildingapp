package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlanEditor: () -> Unit,
    onNavigateToPlanManagement: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "برنامه‌ی تمرینی",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("plan_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LargeMenuActionButton(
                title = "ویرایش برنامه‌ی تمرینی",
                subtitle = "تنظیم روزها، حرکات، تعداد ست و تکرارها یا حذف برنامه",
                icon = Icons.Default.EditCalendar,
                gradient = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                ),
                testTag = "plan_hub_editor_button",
                onClick = onNavigateToPlanEditor
            )

            LargeMenuActionButton(
                title = "مدیریت برنامه‌ی تمرینی",
                subtitle = "خروجی گرفتن از داده‌های تمرینی و ورود فایل پشتیبان",
                icon = Icons.Default.FolderZip,
                gradient = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                    )
                ),
                testTag = "plan_hub_management_button",
                onClick = onNavigateToPlanManagement
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
