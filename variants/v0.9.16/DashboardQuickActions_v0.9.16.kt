package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardQuickAction(
    titolo: String,
    icona: ImageVector,
    coloreAccento: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val temaScuro = isSystemInDarkTheme()

    Card(
        modifier = modifier.height(72.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            coloreAccento.copy(alpha = 0.55f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (temaScuro) {
                coloreAccento.copy(alpha = 0.08f)
            } else {
                coloreAccento.copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = coloreAccento,
                modifier = Modifier.size(21.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = titolo,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
