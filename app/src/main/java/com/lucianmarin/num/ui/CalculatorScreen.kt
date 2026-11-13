package com.lucianmarin.num.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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

private val LEFT_GUTTER_WIDTH = 40.dp
private val RIGHT_GUTTER_WIDTH = 40.dp
private val LIST_VERTICAL_PADDING = 10.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalNumColors.current
    val context = LocalContext.current
    val syntaxVisualTransformation = remember(colors.syntax) {
        SyntaxHighlighter.visualTransformation(colors.syntax)
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.history.size
    )

    // Scroll to the bottom input card when history size changes
    LaunchedEffect(state.history.size) {
        listState.animateScrollToItem(state.history.size)
    }

    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.getBottom(density)
    val imeBottom = WindowInsets.ime.getBottom(density)
    val bottomInsetPadding = with(density) { (navBarBottom - imeBottom).coerceAtLeast(0).toDp() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
            .imePadding()
    ) {
        // Top Header Bar
        TopHeaderBar(
            state = state,
            onUndo = viewModel::undo,
            onClear = viewModel::clearHistory,
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

        // History List aligned to bottom, followed by the bottom input card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = LIST_VERTICAL_PADDING,
                    bottom = LIST_VERTICAL_PADDING + bottomInsetPadding
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                itemsIndexed(
                    items = state.history,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val lineNumber = index + 1
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
                        onInsertResult = {
                            viewModel.insertIntoCurrentInput("\$$lineNumber")
                        }
                    )
                }

                item(key = "bottom_input_card") {
                    BottomCalculatorInputPanel(
                        lineNumber = state.history.size + 1,
                        currentInput = state.currentInput,
                        livePreview = state.livePreview,
                        visualTransformation = syntaxVisualTransformation,
                        onInputChange = viewModel::onInputChange,
                        onSubmit = viewModel::submitCurrentInput,
                        onCopyPreview = { text -> copyToClipboard(context, text) }
                    )
                }
            }
        }
    }

    if (state.showSyntaxSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSyntaxSheet(false) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = colors.bgSurface,
            contentColor = colors.textPrimary,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
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
    onClear: () -> Unit,
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
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val undoColor = if (state.canUndo) colors.textSecondary else colors.textMuted.copy(alpha = 0.4f)
                    Surface(
                        color = Color.Transparent,
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = state.canUndo) { onUndo() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = undoColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Undo",
                                color = undoColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = Color.Transparent,
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onClear() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Clear",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Syntax Guide Button
                Surface(
                    color = Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenSyntax() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, colors.borderLight, CircleShape)
                            .clickable { onInsertToken(name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onInsertToken(fnName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
        shape = CircleShape,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, colors.borderLight, CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label ",
                color = colors.accentPink,
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
    onInsertResult: () -> Unit
) {
    val colors = LocalNumColors.current
    val result = item.result
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            withFrameNanos { }
            val padPx = with(density) { LIST_VERTICAL_PADDING.toPx() }
            val rect = if (cardSize != IntSize.Zero) {
                Rect(
                    left = 0f,
                    top = -padPx,
                    right = cardSize.width.toFloat(),
                    bottom = cardSize.height.toFloat() + padPx
                )
            } else {
                null
            }
            bringIntoViewRequester.bringIntoView(rect)
        }
    }

    val cardShape = RoundedCornerShape(12.dp)
    Surface(
        color = colors.bgSurface,
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .bringIntoViewRequester(bringIntoViewRequester)
            .clip(cardShape)
            .border(1.dp, colors.borderLight, cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Editable Input Formula Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertically highlighted left gutter: Line number
                Box(
                    modifier = Modifier
                        .width(LEFT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(colors.bgHover),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\$$lineNumber",
                        color = colors.accentBlue,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                // Editable syntax-highlighted formula field
                BasicTextField(
                    value = item.expression,
                    onValueChange = onExpressionChange,
                    singleLine = true,
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
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                // Vertically highlighted right gutter: Clear / Remove (X) icon or Referenced icon
                val isReferenced = item.isReferenced
                Box(
                    modifier = Modifier
                        .width(RIGHT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(colors.bgHover)
                        .clickable(enabled = !isReferenced) { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReferenced) Icons.Default.Link else Icons.Default.Close,
                        contentDescription = if (isReferenced) "Line Referenced" else "Remove Line",
                        tint = if (isReferenced) colors.textMuted else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Row 2: Separate Result Row below the formula
            if (!result.isComment) {
                HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

                val isError = result.error != null
                val rowBg = if (isError) colors.errorBg else colors.bgSubtle
                val gutterBg = if (isError) colors.errorBg else colors.bgHover
                val displayText = if (isError) {
                    result.error ?: "Error"
                } else {
                    result.formatted ?: ""
                }
                val canCopy = !isError && displayText.isNotEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(rowBg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertically highlighted left gutter: Equal sign (=) or (!)
                    Box(
                        modifier = Modifier
                            .width(LEFT_GUTTER_WIDTH)
                            .fillMaxHeight()
                            .background(gutterBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isError) "!" else "=",
                            color = if (isError) colors.errorText else colors.accentOrange,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                    // Evaluated Result
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(enabled = canCopy) {
                                onInsertResult()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = displayText.ifEmpty { " " },
                            color = if (isError) colors.errorText else colors.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                    // Vertically highlighted right gutter: Copy icon
                    Box(
                        modifier = Modifier
                            .width(RIGHT_GUTTER_WIDTH)
                            .fillMaxHeight()
                            .background(gutterBg)
                            .clickable(enabled = canCopy) {
                                onCopyResult(displayText)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (canCopy) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Result",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomCalculatorInputPanel(
    lineNumber: Int,
    currentInput: androidx.compose.ui.text.input.TextFieldValue,
    livePreview: EvalLineResult,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation,
    onInputChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onCopyPreview: (String) -> Unit
) {
    val colors = LocalNumColors.current
    val cardShape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            withFrameNanos { }
            val padPx = with(density) { LIST_VERTICAL_PADDING.toPx() }
            val rect = if (cardSize != IntSize.Zero) {
                Rect(
                    left = 0f,
                    top = -padPx,
                    right = cardSize.width.toFloat(),
                    bottom = cardSize.height.toFloat() + padPx
                )
            } else {
                null
            }
            bringIntoViewRequester.bringIntoView(rect)
        }
    }

    Surface(
        color = colors.bgSurface,
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .bringIntoViewRequester(bringIntoViewRequester)
            .clip(cardShape)
            .border(1.dp, colors.borderLight, cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Single Formula Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertically highlighted left gutter: Line number
                Box(
                    modifier = Modifier
                        .width(LEFT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(colors.bgHover),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\$$lineNumber",
                        color = colors.accentBlue,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                BasicTextField(
                    value = currentInput,
                    onValueChange = onInputChange,
                    singleLine = true,
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
                    keyboardActions = KeyboardActions(
                        onSend = { onSubmit() },
                        onDone = { onSubmit() }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                // Vertically highlighted right gutter: Clear (X) icon
                val hasText = currentInput.text.isNotEmpty()
                Box(
                    modifier = Modifier
                        .width(RIGHT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(colors.bgHover)
                        .clickable(enabled = hasText) {
                            onInputChange(androidx.compose.ui.text.input.TextFieldValue(""))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Input",
                        tint = if (hasText) colors.textSecondary else colors.textMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Row 2: Separate Evaluated Result Row below the input formula
            HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

            val hasInput = currentInput.text.isNotBlank()
            val isError = hasInput && livePreview.error != null
            val formattedPreview = livePreview.formatted
            val rowBg = if (isError) colors.errorBg else colors.bgSubtle
            val gutterBg = if (isError) colors.errorBg else colors.bgHover
            val displayText = when {
                !hasInput || livePreview.isComment -> ""
                isError -> livePreview.error ?: "Error"
                else -> formattedPreview ?: ""
            }
            val canCopyPreview = !isError && displayText.isNotEmpty()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(rowBg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertically highlighted left gutter: Equal sign (=) or (!)
                Box(
                    modifier = Modifier
                        .width(LEFT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(gutterBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isError) "!" else "=",
                        color = if (isError) colors.errorText else colors.accentOrange,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                // Evaluated Result
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = canCopyPreview) {
                            onCopyPreview(displayText)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = displayText.ifEmpty { " " },
                        color = if (isError) colors.errorText else colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                VerticalDivider(color = colors.borderLight, thickness = 1.dp)

                // Vertically highlighted right gutter: Copy icon
                Box(
                    modifier = Modifier
                        .width(RIGHT_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .background(gutterBg)
                        .clickable(enabled = canCopyPreview) {
                            onCopyPreview(displayText)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (canCopyPreview) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Result Preview",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
                    text = "Syntax Reference",
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

        Spacer(modifier = Modifier.height(12.dp))

        // Search box
        Surface(
            color = colors.bgSubtle,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .border(1.dp, colors.borderLight, CircleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            text = "Search functions, units, constants…",
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (cat in categories) {
                val active = cat == selectedCategory
                Surface(
                    color = if (active) colors.accentOrange else colors.bgSubtle,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (active) colors.accentOrange else colors.borderLight,
                            shape = CircleShape
                        )
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (active) androidx.compose.ui.graphics.Color.White else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.borderLight)

        // Catalog Items List
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (group in filteredGroups) {
                item(key = "header_${group.category}") {
                    Text(
                        text = group.category.uppercase(),
                        color = colors.accentBlue,
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

    val cardShape = RoundedCornerShape(14.dp)
    val innerBoxShape = RoundedCornerShape(10.dp)

    Surface(
        color = colors.bgSubtle,
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, colors.borderLight, cardShape)
            .clickable { onTryInInput() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = colors.accentPinkSoft,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onAddToHistory() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to History",
                            tint = colors.accentPink,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Add",
                            color = colors.accentPink,
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
                shape = innerBoxShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(innerBoxShape)
                    .border(1.dp, colors.borderLight, innerBoxShape)
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
                            color = colors.textPrimary,
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
    clipboard.setPrimaryClip(ClipData.newPlainText("Numeric Result", text))
}
