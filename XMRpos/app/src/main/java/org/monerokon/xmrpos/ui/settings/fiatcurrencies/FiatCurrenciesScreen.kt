// FiatCurrenciesScreen.kt
package org.monerokon.xmrpos.ui.settings.fiatcurrencies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.settings.companyinformation.CompanyInformationViewModel
import kotlin.math.exp

@Composable
fun FiatCurrenciesScreenRoot(viewModel: FiatCurrenciesViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    FiatCurrenciesScreen(
        onBackClick = viewModel::navigateToMainSettings,
        fiatOptions = viewModel.fiatOptions,
        primaryFiatCurrency = viewModel.primaryFiatCurrency,
        updatePrimaryFiatCurrency = viewModel::updatePrimaryFiatCurrency,
        referenceFiatCurrencies = viewModel.referenceFiatCurrencies,
        addReferenceFiatCurrency = viewModel::addReferenceFiatCurrency,
        removeReferenceFiatCurrency = viewModel::removeReferenceFiatCurrency,
        moveReferenceFiatCurrencyUp = viewModel::moveReferenceFiatCurrencyUp,
        moveReferenceFiatCurrencyDown = viewModel::moveReferenceFiatCurrencyDown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiatCurrenciesScreen(
    onBackClick: () -> Unit,
    fiatOptions: List<String>,
    primaryFiatCurrency: String,
    updatePrimaryFiatCurrency: (String) -> Unit,
    referenceFiatCurrencies: List<String>,
    addReferenceFiatCurrency: (String) -> Unit,
    removeReferenceFiatCurrency: (Int) -> Unit,
    moveReferenceFiatCurrencyUp: (Int) -> Unit,
    moveReferenceFiatCurrencyDown: (Int) -> Unit,
) {
    var primaryFiatSelectExpanded by remember { mutableStateOf(false) }
    val primaryFiatSelectTextFieldState = rememberTextFieldState(fiatOptions[0])
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = {onBackClick()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go back to previous screen"
                        )
                    }
                },
                title = {
                    Text("Fiat currencies")
                }
            )
        },
    ) { innerPadding ->
        Column (
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Text("Primary fiat currency", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This is the currency that will be entered to take an order. It will also be displayed on the receipt along with the exchange rate.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            CurrencySelector(primaryFiatCurrency, "Primary fiat currency", fiatOptions, onCurrencySelected = {updatePrimaryFiatCurrency(it)})
            Spacer(modifier = Modifier.height(24.dp))
            Text("Reference fiat currencies", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            ReferenceFiatCurrenciesCard(referenceFiatCurrencies, removeReferenceFiatCurrency, moveReferenceFiatCurrencyUp, moveReferenceFiatCurrencyDown)
            Spacer(modifier = Modifier.height(8.dp))
            ReferenceCurrencySelector(fiatOptions, addReferenceFiatCurrency)
    }}
}


@Composable
fun ReferenceFiatCurrenciesCard(
    referenceFiatCurrencies: List<String>,
    removeReferenceFiatCurrency: (Int) -> Unit,
    moveReferenceFiatCurrencyUp: (Int) -> Unit,
    moveReferenceFiatCurrencyDown: (Int) -> Unit,
) {
    OutlinedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, Color(0xff52443c)),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(referenceFiatCurrencies.size) { index ->
                    ReferenceFiatCurrencyRow(referenceFiatCurrencies[index], onRemoveClick = {removeReferenceFiatCurrency(index)}, onMoveReferenceFiatCurrencyUpClick = {moveReferenceFiatCurrencyUp(index)}, onMoveReferenceFiatCurrencyDownClick = {moveReferenceFiatCurrencyDown(index)})
                }
            }
        }
    }
}

@Composable
fun ReferenceFiatCurrencyRow(
    fiatCurrency: String,
    onRemoveClick: () -> Unit,
    onMoveReferenceFiatCurrencyUpClick: () -> Unit,
    onMoveReferenceFiatCurrencyDownClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Text(fiatCurrency)
        Row (
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { onMoveReferenceFiatCurrencyUpClick() }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Remove reference fiat currency",
                )
            }
            IconButton(onClick = { onMoveReferenceFiatCurrencyDownClick() }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Remove reference fiat currency",
                )
            }
            IconButton(onClick = { onRemoveClick() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove reference fiat currency",
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(value: String, label: String, currencies: List<String>, onCurrencySelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }


    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {expanded = !expanded}, modifier = modifier) {
            TextField(
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                value = value,
                enabled = true,
                label = { Text(label) },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                    )
                }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = {expanded = false}) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            expanded = false
                            onCurrencySelected(currency)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
            }
        }
    }
}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceCurrencySelector(fiatOptions: List<String>, addReferenceFiatCurrency: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }


    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {expanded = !expanded}, modifier = modifier) {
            FilledTonalButton(
                modifier = Modifier.menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ).fillMaxWidth(),
                onClick = {},
            ) {
                Text("Add reference fiat currency")
            }
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = {expanded = false}) {
                fiatOptions.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            expanded = false
                            addReferenceFiatCurrency(currency)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }}




