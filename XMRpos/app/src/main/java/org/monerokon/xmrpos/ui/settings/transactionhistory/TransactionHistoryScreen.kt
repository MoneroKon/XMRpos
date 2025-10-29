package org.monerokon.xmrpos.ui.settings.transactionhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.monerokon.xmrpos.data.remote.backend.model.BackendConfirmedTransaction
import org.monerokon.xmrpos.data.remote.backend.model.BackendPendingTransaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionHistoryScreenRoot(
    viewModel: TransactionHistoryViewModel,
    navController: NavHostController,
) {
    viewModel.setNavController(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionHistoryScreen(
        uiState = uiState,
        onBackClick = viewModel::navigateToMainSettings,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    uiState: TransactionHistoryUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go back to previous screen",
                        )
                    }
                },
                title = { Text("Transaction history") },
                actions = {
                    if (!uiState.isLoading) {
                        TextButtonAction("Refresh", onRefresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(onClick = onRefresh) {
                            Text("Try again")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    item {
                        SectionTitle("Pending transactions")
                    }
                    item {
                        if (uiState.pendingTransactions.isEmpty()) {
                            EmptyStateMessage("No pending transactions")
                        } else {
                            PendingTransactionsTable(uiState.pendingTransactions)
                        }
                    }
                    item {
                        SectionTitle("Confirmed transactions")
                    }
                    if (uiState.confirmedTransactions.isEmpty()) {
                        item {
                            EmptyStateMessage("No confirmed transactions")
                        }
                    } else {
                        items(uiState.confirmedTransactions, key = { it.transactionId }) { transaction ->
                            ConfirmedTransactionRow(transaction)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextButtonAction(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(label)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PendingTransactionsTable(transactions: List<BackendPendingTransaction>) {
    val backgroundColor = Color(0xFFFFB86C)
    val contentColor = Color(0xFF282A36)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TableHeaderCell("ID", weight = 1.5f, textColor = contentColor)
            TableHeaderCell("Amount (XMR)", weight = 2f, textColor = contentColor)
            TableHeaderCell("Accepted", weight = 1f, textColor = contentColor)
            TableHeaderCell("Confirmed", weight = 1f, textColor = contentColor)
        }
        transactions.forEach { transaction ->
            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell(transaction.id.toString(), weight = 1.5f, textColor = contentColor)
                TableCell(formatAtomicAmount(transaction.amount), weight = 2f, textColor = contentColor)
                TableCell(transaction.accepted.toYesNo(), weight = 1f, textColor = contentColor)
                TableCell(transaction.confirmed.toYesNo(), weight = 1f, textColor = contentColor)
            }
        }
    }
}

@Composable
private fun ConfirmedTransactionRow(transaction: BackendConfirmedTransaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Transaction #${transaction.transactionId}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TableHeaderCell("Tx hash", weight = 2f)
            TableCell(
                text = transaction.txHash,
                weight = 2f,
                bold = false,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DetailCell("Timestamp", formatTimestamp(transaction.timestamp))
            DetailCell("Height", transaction.height.toString())
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DetailCell("Accepted", transaction.accepted.toYesNo())
            DetailCell("Confirmed", transaction.confirmed.toYesNo())
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String, weight: Float, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = textColor,
        modifier = Modifier
            .weight(weight)
            .padding(end = 8.dp),
    )
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = if (bold) {
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = textColor,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(weight)
            .padding(end = 8.dp),
    )
}

@Composable
private fun RowScope.DetailCell(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Boolean.toYesNo(): String = if (this) "Yes" else "No"

private fun formatAtomicAmount(amount: Long): String {
    val xmrValue = amount / 1_000_000_000_000.0
    return String.format(Locale.US, "%.12f", xmrValue)
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        formatter.format(instant.atZone(ZoneId.systemDefault()))
    } catch (e: Exception) {
        timestamp
    }
}
