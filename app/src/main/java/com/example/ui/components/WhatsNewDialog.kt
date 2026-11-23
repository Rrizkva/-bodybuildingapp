package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class WhatsNewVersionSection(
    val versionTitle: String,
    val items: List<String>
)

private val whatsNewSections = listOf(
    WhatsNewVersionSection(
        versionTitle = "نسخه ۲.۵",
        items = listOf(
            "لوزی جایگزین خط اسلایدر شد و مقدار فشار و سختی فقط روی اعداد طبیعی ۱ تا ۱۰ قرار میگیرد.",
            "عدد انتخابشده بالای لوزی نمایش داده میشود و با حرکت لوزی جابهجا میشود.",
            "رنگ پشت لوزی با افزایش عدد پررنگتر و با کاهش عدد کمرنگتر میشود."
        )
    ),
    WhatsNewVersionSection(
        versionTitle = "نسخه ۲.۱",
        items = listOf(
            "باکس «هفته قبل» در صفحه ثبت حرکت اضافه شد. ستها، وزن و تکرار جلسهی قبلی همان حرکت را نشان میدهد.",
            "امتیاز فشار و سختی بهصورت نوار انتخاب از ۱ تا ۱۰ با برچسب «سبک» و «سنگین» تغییر کرد.",
            "در آنالیز، با باز شدن جزئیات یک حرکت، جزئیات حرکت قبلی بسته میشود."
        )
    ),
    WhatsNewVersionSection(
        versionTitle = "نسخه ۲.۰",
        items = listOf(
            "صفحهی خوشآمدگویی با لوگو و نمایش شمارهی نسخه اضافه شد.",
            "با ورود دوباره به برنامه، اگر تمرین ناتمام باشد، میتوانی «ادامه تمرین» یا «شروع تازه» را انتخاب کنی.",
            "دکمهی «برگشت» به حرکت قبلی اضافه شد.",
            "بخش «برنامهی تمرینی» با دو گزینهی «ویرایش برنامهی تمرینی» و «مدیریت برنامهی تمرینی» اضافه شد. خروجی و ورود اطلاعات به بخش مدیریت منتقل شد.",
            "کرش هنگام ذخیرهی برنامهی تمرینی برطرف شد و برنامهی جدید جایگزین قبلی میشود.",
            "گزینهی «حذف برنامه» با پیام تأیید اضافه شد. سابقهی جلسات حفظ میشود.",
            "امکان حذف جلسه در بخش آنالیز اضافه شد.",
            "کامنت برای هر حرکت اضافه شد و چند خطی است. کامنت در آنالیز زیر همان حرکت نمایش داده میشود.",
            "کادر تماس با سازنده در صفحهی اصلی اضافه شد.",
            "با بهروزرسانی برنامه، اطلاعات قبلی حفظ میشوند."
        )
    )
)

@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* User must tap متوجه شدم to mark version as seen */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("whats_new_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "تغییرات جدید",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    whatsNewSections.forEach { section ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = section.versionTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            section.items.forEach { bullet ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                                    )
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("whats_new_dismiss_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "متوجه شدم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
