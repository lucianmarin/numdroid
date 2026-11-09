package com.lucianmarin.num.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.num.math.EvalLineResult
import com.lucianmarin.num.math.MathEngine
import com.lucianmarin.num.math.MathScope
import com.lucianmarin.num.math.MathValue
import com.lucianmarin.num.math.SyntaxCatalog
import com.lucianmarin.num.math.SyntaxCatalogItem
import com.lucianmarin.num.math.SyntaxHighlighter
import com.lucianmarin.num.ui.theme.LocalNumColors
import com.lucianmarin.num.ui.theme.ThemeMode

private val QUICK_TOKENS = listOf(
    "ans", "(", ")", "[", "]", "+", "-", "*", "/", "^", "=", ";",
    "pi", "sqrt(", "sin(", "cos(", "in ", "deg", "i", ":"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalNumColors.current
    val context = LocalContext.current
    val syntaxVisualTransformation = remember(colors.syntax) {
        SyntaxHighlighter.visualTransformation(colors.syntax)
    }

    val listState = rememberLazyListState()
    val reversedHistory = remember(state.history) {
        state.history.mapIndexed { chronologicalIndex, item ->
            chronologicalIndex + 1 to item
        }.asReversed()
    }

    // Scroll to index 0 (newest entry right above the bottom input) when a new item is added
    LaunchedEffect(state.history.size) {
        if (state.history.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Header Bar
        TopHeaderBar(
            state = state,
            onUndo = viewModel::undo,
            onResetDemo = viewModel::resetToDemo,
            onClear = viewModel::clearHistory,
            onCycleTheme = viewModel::cycleThemeMode,
            onOpenSyntax = { viewModel.toggleSyntaxSheet(true) },
            onToggleScope = viewModel::toggleScopeBar
        )

        // Scope & Stats Bar (Sum, Avg, Variables & Functions)
        AnimatedVisibility(visible = state.showScopeBar) {
            ScopeAndStatsBar(
                summary = state.scopeSummary,
                onInsertToken = { token -> viewModel.insertIntoCurrentInput(token) }
            )
        }

        // Reverse History List (editable items, newest nearest the bottom input bar)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (reversedHistory.isEmpty()) {
                EmptyHistoryPlaceholder(
                    onLoadDemo = viewModel::resetToDemo,
                    onOpenSyntax = { viewModel.toggleSyntaxSheet(true) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = reversedHistory,
                        key = { _, pair -> pair.second.id }
                    ) { _, (lineNumber, item) ->
                        HistoryEntryCard(
                            lineNumber = lineNumber,
                            item = item,
                            visualTransformation = syntaxVisualTransformation,
                            onExpressionChange = { newExpr ->
                                viewModel.updateHistoryItem(item.id, newExpr)
                            },
                            onDelete = { viewModel.deleteHistoryItem(item.id) },
                            onCopyResult = { textToCopy ->
                                copyToClipboard(context, textToCopy)
                            },
                            onInsertResult = { resultText ->
                                viewModel.insertIntoCurrentInput(resultText)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Single Input Panel (with Quick Token Bar + Input Formula Row + Separate Result Row Below)
        BottomCalculatorInputPanel(
            currentInput = state.currentInput,
            livePreview = state.livePreview,
            visualTransformation = syntaxVisualTransformation,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::submitCurrentInput,
            onQuickToken = { token -> viewModel.insertIntoCurrentInput(token) },
            onCopyPreview = { text -> copyToClipboard(context, text) }
        )
    }

    if (state.showSyntaxSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSyntaxSheet(false) },
            sheetState = sheetState,
            containerColor = colors.bgSurface,
            contentColor = colors.textPrimary
        ) {
            SyntaxCatalogBottomSheet(
                onInsertExpression = { expr, asNewItem ->
                    if (asNewItem) {
                        viewModel.insertIntoCurrentInput(expr, addAsNewHistoryItem = true)
                    } else {
                        viewModel.setCurrentInputText(expr)
                    }
                    viewModel.toggleSyntaxSheet(false)
                },
                onClose = { viewModel.toggleSyntaxSheet(false) }
            )
        }
    }
}

@Composable
private fun TopHeaderBar(
    state: CalculatorUiState,
    onUndo: () -> Unit,
    onResetDemo: () -> Unit,
    onClear: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenSyntax: () -> Unit,
    onToggleScope: () -> Unit
) {
    val colors = LocalNumColors.current

    Surface(
        color = colors.bgSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Title + Accent Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleScope() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.accentOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Num",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${state.history.size} lines",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) colors.textSecondary else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onResetDemo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Load Demo Formulas",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCycleTheme,
                        modifier = Modifier.size(36.dp)
                    ) {
                        val icon = when (state.themeMode) {
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toggle Theme (${state.themeMode.name})",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Syntax Guide Pill Button
                    Surface(
                        color = colors.accentOrangeSoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { onOpenSyntax() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Syntax Reference",
                                tint = colors.accentOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Syntax",
                                color = colors.accentOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.borderLight, thickness = 1.dp)
        }
    }
}

@Composable
private fun ScopeAndStatsBar(
    summary: ScopeSummary,
    onInsertToken: (String) -> Unit
) {
    val colors = LocalNumColors.current
    val hasContent = summary.numericCount > 0 ||
        summary.variables.isNotEmpty() ||
        summary.functions.isNotEmpty()

    if (!hasContent) return

    Surface(
        color = colors.bgSubtle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (summary.numericSum != null && summary.numericAvg != null) {
                    StatBadge(
                        label = "SUM",
                        value = MathValue.formatNumber(summary.numericSum),
                        onClick = { onInsertToken(MathValue.formatNumber(summary.numericSum)) }
                    )
                    StatBadge(
                        label = "AVG",
                        value = MathValue.formatNumber(summary.numericAvg),
                        onClick = { onInsertToken(MathValue.formatNumber(summary.numericAvg)) }
                    )
                }

                for ((name, value) in summary.variables) {
                    Surface(
                        color = colors.bgSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .border(1.dp, colors.borderLight, RoundedCornerShape(6.dp))
                            .clickable { onInsertToken(name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = colors.syntax.variable,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = " = $value",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                for (fnSig in summary.functions) {
                    val fnName = fnSig.substringBefore("(") + "("
                    Surface(
                        color = colors.accentBlueSoft,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onInsertToken(fnName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                tint = colors.accentBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = fnSig,
                                color = colors.accentBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.borderLight, thickness = 1.dp)
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = LocalNumColors.current
    Surface(
        color = colors.bgSurface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .border(1.dp, colors.borderLight, RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label ",
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    lineNumber: Int,
    item: HistoryItem,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation,
    onExpressionChange: (String) -> Unit,
    onDelete: () -> Unit,
    onCopyResult: (String) -> Unit,
    onInsertResult: (String) -> Unit
) {
    val colors = LocalNumColors.current
    val result = item.result

    Surface(
        color = colors.bgSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderLight, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Editable Input Formula Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Line number badge
                Text(
                    text = "#$lineNumber",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(28.dp)
                )

                // Editable syntax-highlighted formula field
                BasicTextField(
                    value = item.expression,
                    onValueChange = onExpressionChange,
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(colors.accentOrange),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Line",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Row 2: Separate Result Row below the formula
            if (!result.isComment) {
                HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

                val isError = result.error != null
                val rowBg = if (isError) colors.errorBg else colors.bgSubtle
                val displayText = if (isError) {
                    result.error ?: "Error"
                } else {
                    result.formatted ?: ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable(enabled = !isError && displayText.isNotEmpty()) {
                            onInsertResult(displayText)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isError) "!" else "=",
                            color = if (isError) colors.errorText else colors.accentOrange,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = displayText,
                            color = if (isError) colors.errorText else colors.accentGreen,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!isError && displayText.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onCopyResult(displayText) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Result",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomCalculatorInputPanel(
    currentInput: androidx.compose.ui.text.input.TextFieldValue,
    livePreview: EvalLineResult,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation,
    onInputChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onQuickToken: (String) -> Unit,
    onCopyPreview: (String) -> Unit
) {
    val colors = LocalNumColors.current

    Surface(
        color = colors.bgSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = colors.borderStrong, thickness = 1.dp)

            // Quick Math Symbol & Function Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (token in QUICK_TOKENS) {
                    Surface(
                        color = colors.bgSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .border(1.dp, colors.borderLight, RoundedCornerShape(6.dp))
                            .clickable { onQuickToken(token) }
                    ) {
                        Text(
                            text = token.trim(),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

            // Row 1: Single Formula Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "›",
                    color = colors.accentOrange,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 10.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (currentInput.text.isEmpty()) {
                        Text(
                            text = "Enter math formula (e.g. 25 cm in inch, det([1,2;3,4]))…",
                            color = colors.textMuted,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = currentInput,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(colors.accentOrange),
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { onSubmit() },
                            onDone = { onSubmit() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (currentInput.text.isNotEmpty()) {
                    IconButton(
                        onClick = { onInputChange(androidx.compose.ui.text.input.TextFieldValue("")) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Input",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Evaluate / Add to History Button
                Surface(
                    color = if (currentInput.text.isNotBlank()) colors.accentOrange else colors.bgSubtle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(enabled = currentInput.text.isNotBlank()) { onSubmit() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Evaluate Formula",
                            tint = if (currentInput.text.isNotBlank()) {
                                androidx.compose.ui.graphics.Color.White
                            } else {
                                colors.textMuted
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Separate Evaluated Result Preview Row below the input formula
            HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

            val hasInput = currentInput.text.isNotBlank()
            val hasError = livePreview.error != null
            val formattedPreview = livePreview.formatted

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle)
                    .clickable(enabled = !formattedPreview.isNullOrEmpty()) {
                        formattedPreview?.let(onCopyPreview)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "=",
                        color = if (hasInput && !hasError && formattedPreview != null) {
                            colors.accentOrange
                        } else {
                            colors.textMuted
                        },
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    val resultText = when {
                        !hasInput -> "Result preview appears here"
                        livePreview.isComment -> "Comment"
                        hasError -> livePreview.error ?: "Invalid expression"
                        formattedPreview != null -> formattedPreview
                        else -> "…"
                    }

                    val resultColor = when {
                        !hasInput || livePreview.isComment -> colors.textMuted
                        hasError -> colors.errorText
                        else -> colors.accentGreen
                    }

                    Text(
                        text = resultText,
                        color = resultColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (formattedPreview != null) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!formattedPreview.isNullOrEmpty()) {
                    Text(
                        text = "Tap to copy",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder(
    onLoadDemo: () -> Unit,
    onOpenSyntax: () -> Unit
) {
    val colors = LocalNumColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No history items yet",
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Type a formula in the bottom input bar. Every entry in history stays editable and reactive.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = colors.accentOrange,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onLoadDemo() }
            ) {
                Text(
                    text = "Load Demo Sheet",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Surface(
                color = colors.bgSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, colors.borderStrong, RoundedCornerShape(8.dp))
                    .clickable { onOpenSyntax() }
            ) {
                Text(
                    text = "Browse Syntax",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyntaxCatalogBottomSheet(
    onInsertExpression: (expr: String, asNewItem: Boolean) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalNumColors.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf("All") + SyntaxCatalog.groups.map { it.category }
    }

    val filteredGroups = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        SyntaxCatalog.groups
            .filter { selectedCategory == "All" || it.category == selectedCategory }
            .mapNotNull { group ->
                val matching = if (q.isEmpty()) {
                    group.items
                } else {
                    group.items.filter { item ->
                        item.name.lowercase().contains(q) ||
                            item.syntax.lowercase().contains(q) ||
                            item.desc.lowercase().contains(q) ||
                            item.subcategory.lowercase().contains(q)
                    }
                }
                if (matching.isEmpty()) null else group.copy(items = matching)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(640.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "MathJS Syntax Reference",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${SyntaxCatalog.totalCount} built-in operators, functions, units & constants",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search box
        Surface(
            color = colors.bgSubtle,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.borderLight, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search functions, units, constants (e.g. lusolve, degC, matrix)…",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(colors.accentOrange),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Tabs
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (cat in categories) {
                val active = cat == selectedCategory
                Surface(
                    color = if (active) colors.accentOrange else colors.bgSubtle,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (active) androidx.compose.ui.graphics.Color.White else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.borderLight)

        // Catalog Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (group in filteredGroups) {
                item(key = "header_${group.category}") {
                    Text(
                        text = group.category.uppercase(),
                        color = colors.accentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                itemsIndexed(
                    items = group.items,
                    key = { idx, it -> "${group.category}_${it.name}_$idx" }
                ) { _, item ->
                    SyntaxCatalogItemCard(
                        item = item,
                        onTryInInput = { onInsertExpression(item.expr, false) },
                        onAddToHistory = { onInsertExpression(item.expr, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SyntaxCatalogItemCard(
    item: SyntaxCatalogItem,
    onTryInInput: () -> Unit,
    onAddToHistory: () -> Unit
) {
    val colors = LocalNumColors.current
    val highlightedExpr = remember(item.expr, colors.syntax) {
        SyntaxHighlighter.highlight(item.expr, colors.syntax)
    }
    val evaluatedExample = remember(item.expr) {
        MathEngine.evaluateSingleLine(item.expr, MathScope()).formatted ?: ""
    }

    Surface(
        color = colors.bgSubtle,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderLight, RoundedCornerShape(10.dp))
            .clickable { onTryInInput() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = colors.bgSurface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.subcategory,
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = colors.accentOrangeSoft,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { onAddToHistory() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to History",
                            tint = colors.accentOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Add",
                            color = colors.accentOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.desc,
                color = colors.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Example box showing input formula and result row below
            Surface(
                color = colors.bgSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.borderLight, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = highlightedExpr,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (evaluatedExample.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "= $evaluatedExample",
                            color = colors.accentGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Num Result", text))
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}
