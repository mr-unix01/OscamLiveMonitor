package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpressiveSectionLabel(
    titolo: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    compatto: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = coloreAccento.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = coloreAccento,
                modifier = Modifier
                    .padding(if (compatto) 6.dp else 7.dp)
                    .size(if (compatto) 17.dp else 19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = titolo,
            fontSize = if (compatto) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Composable
fun ExpressiveMiniInfo(
    etichetta: String,
    valore: String,
    compatto: Boolean,
    modifier: Modifier = Modifier
) {
    val temaScuroMini = androidx.compose.foundation.isSystemInDarkTheme()

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = if (temaScuroMini) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        } else {
            Color.White
        },
        border = androidx.compose.foundation.BorderStroke(
            if (temaScuroMini) 1.dp else 1.1.dp,
            if (temaScuroMini) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            } else {
                Color(0xFFD5DDD5)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compatto) 10.dp else 12.dp,
                vertical = if (compatto) 8.dp else 10.dp
            )
        ) {
            Text(
                text = etichetta,
                fontSize = if (compatto) 10.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            val valoreDisponibile =
                valore.isNotBlank() &&
                    !valore.equals("N/A", ignoreCase = true) &&
                    !valore.equals("Sconosciuta", ignoreCase = true) &&
                    !valore.equals("Sconosciuto", ignoreCase = true)

            Text(
                text = if (valoreDisponibile) valore else "—",
                fontSize = if (compatto) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (valoreDisponibile) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
