import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

// CurrencyConverterCard
@Composable
fun CurrencyConverterCard(
    currency: String,
    exchangeRate: Double?,
    paymentValue: String,
    targetXMRvalue: BigDecimal? = null,
    elevation: CardElevation = CardDefaults.cardElevation(6.dp),
    color: CardColors = CardDefaults.elevatedCardColors()
) {
    ElevatedCard(
        elevation = elevation,
        colors = color,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column {
                if (currency != "") {
                    Text(text = currency, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.width(48.dp).padding(vertical = 7.5.dp)
                    )
                }

                if (exchangeRate != null) {
                    Text(
                        text = "1 XMR = $exchangeRate $currency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.width(48.dp).padding(vertical = 7.5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column (
                horizontalAlignment = Alignment.End
            ) {
                if (exchangeRate != null && exchangeRate != 0.0) {
                    val rate = BigDecimal.valueOf(exchangeRate)
                    val xmrAmount = targetXMRvalue ?: paymentValue.toDoubleOrNull()?.let {
                        BigDecimal.valueOf(it).divide(rate, 12, RoundingMode.HALF_UP)
                    }

                    if (xmrAmount != null) {
                        val fiatAmount = xmrAmount.multiply(rate).setScale(3, RoundingMode.HALF_UP)
                        Text(text = "${fiatAmount.toPlainString()} $currency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = "${xmrAmount.setScale(5, RoundingMode.HALF_UP).toPlainString()} XMR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (exchangeRate != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}