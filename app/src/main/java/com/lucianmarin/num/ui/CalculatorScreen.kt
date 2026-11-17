package com.lucianmarin.num.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.num.math.EvalLineResult
import com.lucianmarin.num.math.MathValue
import com.lucianmarin.num.math.SyntaxCatalog
import com.lucianmarin.num.math.SyntaxCatalogItem
import com.lucianmarin.num.math.SyntaxHighlighter
import com.lucianmarin.num.ui.theme.LocalNumColors

private val LEFT_GUTTER_WIDTH = 40.dp
private val RIGHT_GUTTER_WIDTH = 40.dp
private val LIST_VERTICAL_PADDING = 10.dp
private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val FORMULA_KEYBOARD_OPTIONS = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Text,
    imeAction = ImeAction.Done
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
    val formulaTextStyle = remember(colors.syntax.plain) {
        TextStyle(
            color = colors.syntax.plain,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp
        )
    }
    val cursorBrush = remember(colors.accentOrange) { SolidColor(colors.accentOrange) }
    val cardBorder = remember(colors.borderLight) { BorderStroke(1.dp, colors.borderLight) }
    val onCopyText = remember(context) { { text: String -> copyToClipboard(context, text) } }
    val bottomInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var focusBottomInputTrigger by remember { mutableIntStateOf(0) }
    val onInsertLineRef = remember<(Int) -> Unit>(viewModel) {
        { lineNumber ->
            viewModel.insertIntoCurrentInput("\$$lineNumber")
            focusBottomInputTrigger++
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.history.size
    )

    // Scroll to the bottom input card when history size changes
    LaunchedEffect(state.history.size) {
        listState.animateScrollToItem(state.history.size)
    }

    LaunchedEffect(focusBottomInputTrigger) {
        if (focusBottomInputTrigger > 0) {
            val isBottomVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == "bottom_input_card" }
            if (!isBottomVisible) {
                listState.animateScrollToItem(state.history.size)
            }
            withFrameNanos { }
            runCatching {
                bottomInputFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
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
                onInsertToken = { token ->
                    viewModel.insertIntoCurrentInput(token)
                    focusBottomInputTrigger++
                }
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
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "history_card" }
                ) { index, item ->
                    val lineNumber = index + 1
                    val result = item.result
                    HistoryEntryCard(
                        id = item.id,
                        lineNumber = lineNumber,
                        expression = item.expression,
                        formattedResult = result.formatted,
                        errorMessage = result.error,
                        isComment = result.isComment,
                        isReferenced = item.isReferenced,
                        formulaTextStyle = formulaTextStyle,
                        cursorBrush = cursorBrush,
                        cardBorder = cardBorder,
                        visualTransformation = syntaxVisualTransformation,
                        onExpressionChange = viewModel::updateHistoryItem,
                        onDelete = viewModel::deleteHistoryItem,
                        onCopyResult = onCopyText,
                        onInsertResult = onInsertLineRef
                    )
                }

                item(key = "bottom_input_card", contentType = "bottom_input") {
                    BottomCalculatorInputPanel(
                        lineNumber = state.history.size + 1,
                        currentInput = state.currentInput,
                        livePreview = state.livePreview,
                        formulaTextStyle = formulaTextStyle,
                        cursorBrush = cursorBrush,
                        cardBorder = cardBorder,
                        visualTransformation = syntaxVisualTransformation,
                        focusRequester = bottomInputFocusRequester,
                        onInputChange = viewModel::onInputChange,
                        onSubmit = viewModel::submitCurrentInput,
                        onCopyPreview = onCopyText
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
private fun ThreeColumnRow(
    dividerColor: Color,
    leftGutter: @Composable () -> Unit,
    center: @Composable () -> Unit,
    rightGutter: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val leftWidthPx = with(density) { LEFT_GUTTER_WIDTH.roundToPx() }
    val rightWidthPx = with(density) { RIGHT_GUTTER_WIDTH.roundToPx() }
    val dividerWidthPx = with(density) { 1.dp.roundToPx().coerceAtLeast(1) }

    Layout(
        contents = listOf(leftGutter, center, rightGutter),
        modifier = modifier.drawBehind {
            val divW = dividerWidthPx.toFloat()
            drawRect(
                color = dividerColor,
                topLeft = Offset(leftWidthPx.toFloat(), 0f),
                size = Size(divW, size.height)
            )
            drawRect(
                color = dividerColor,
                topLeft = Offset(size.width - rightWidthPx - divW, 0f),
                size = Size(divW, size.height)
            )
        }
    ) { (leftMeasurables, centerMeasurables, rightMeasurables), constraints ->
        val totalWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            leftWidthPx + rightWidthPx + dividerWidthPx * 2
        }
        val centerWidth = (totalWidth - leftWidthPx - rightWidthPx - dividerWidthPx * 2).coerceAtLeast(0)
        val centerPlaceable = centerMeasurables.firstOrNull()?.measure(
            Constraints(
                minWidth = centerWidth,
                maxWidth = centerWidth,
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight
            )
        )
        val rowHeight = centerPlaceable?.height ?: 0
        val leftPlaceable = leftMeasurables.firstOrNull()?.measure(
            Constraints.fixed(leftWidthPx, rowHeight)
        )
        val rightPlaceable = rightMeasurables.firstOrNull()?.measure(
            Constraints.fixed(rightWidthPx, rowHeight)
        )

        layout(totalWidth, rowHeight) {
            leftPlaceable?.placeRelative(0, 0)
            centerPlaceable?.placeRelative(leftWidthPx + dividerWidthPx, 0)
            rightPlaceable?.placeRelative(totalWidth - rightWidthPx, 0)
        }
    }
}

@Composable
private fun HistoryEntryCard(
    id: String,
    lineNumber: Int,
    expression: String,
    formattedResult: String?,
    errorMessage: String?,
    isComment: Boolean,
    isReferenced: Boolean,
    formulaTextStyle: TextStyle,
    cursorBrush: Brush,
    cardBorder: BorderStroke,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation,
    onExpressionChange: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    onInsertResult: (Int) -> Unit
) {
    val colors = LocalNumColors.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    if (isFocused) {
        val density = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(density)
        LaunchedEffect(imeBottom) {
            if (imeBottom > 0) {
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
    }

    Surface(
        color = colors.bgSurface,
        shape = CARD_SHAPE,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Editable Input Formula Row
            ThreeColumnRow(
                dividerColor = colors.borderLight,
                leftGutter = {
                    Box(
                        modifier = Modifier.background(colors.bgHover),
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
                },
                center = {
                    BasicTextField(
                        value = expression,
                        onValueChange = { newExpr -> onExpressionChange(id, newExpr) },
                        singleLine = true,
                        textStyle = formulaTextStyle,
                        cursorBrush = cursorBrush,
                        visualTransformation = visualTransformation,
                        keyboardOptions = FORMULA_KEYBOARD_OPTIONS,
                        keyboardActions = remember(focusManager) {
                            KeyboardActions(onDone = { focusManager.clearFocus() })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                },
                rightGutter = {
                    Box(
                        modifier = Modifier
                            .background(colors.bgHover)
                            .clickable(enabled = !isReferenced) { onDelete(id) },
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
            )

            // Row 2: Separate Result Row below the formula
            if (!isComment) {
                HorizontalDivider(color = colors.borderLight, thickness = 1.dp)

                val isError = errorMessage != null
                val rowBg = if (isError) colors.errorBg else colors.bgSubtle
                val gutterBg = if (isError) colors.errorBg else colors.bgHover
                val displayText = if (isError) errorMessage else (formattedResult ?: "")
                val canCopy = !isError && displayText.isNotEmpty()

                ThreeColumnRow(
                    dividerColor = colors.borderLight,
                    modifier = Modifier.background(rowBg),
                    leftGutter = {
                        Box(
                            modifier = Modifier.background(gutterBg),
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
                    },
                    center = {
                        Box(
                            modifier = Modifier
                                .clickable(enabled = canCopy) {
                                    onInsertResult(lineNumber)
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
                    },
                    rightGutter = {
                        Box(
                            modifier = Modifier
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
                )
            }
        }
    }
}

@Composable
private fun BottomCalculatorInputPanel(
    lineNumber: Int,
    currentInput: TextFieldValue,
    livePreview: EvalLineResult,
    formulaTextStyle: TextStyle,
    cursorBrush: Brush,
    cardBorder: BorderStroke,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation,
    focusRequester: FocusRequester,
    onInputChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onCopyPreview: (String) -> Unit
) {
    val colors = LocalNumColors.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    if (isFocused) {
        val density = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(density)
        LaunchedEffect(imeBottom) {
            if (imeBottom > 0) {
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
    }

    Surface(
        color = colors.bgSurface,
        shape = CARD_SHAPE,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Single Formula Input Field
            ThreeColumnRow(
                dividerColor = colors.borderLight,
                leftGutter = {
                    Box(
                        modifier = Modifier.background(colors.bgHover),
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
                },
                center = {
                    BasicTextField(
                        value = currentInput,
                        onValueChange = onInputChange,
                        singleLine = true,
                        textStyle = formulaTextStyle,
                        cursorBrush = cursorBrush,
                        visualTransformation = visualTransformation,
                        keyboardOptions = FORMULA_KEYBOARD_OPTIONS,
                        keyboardActions = remember(onSubmit) {
                            KeyboardActions(
                                onSend = { onSubmit() },
                                onDone = { onSubmit() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                },
                rightGutter = {
                    val hasText = currentInput.text.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .background(colors.bgHover)
                            .clickable(enabled = hasText) {
                                onInputChange(TextFieldValue(""))
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
            )

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

            ThreeColumnRow(
                dividerColor = colors.borderLight,
                modifier = Modifier.background(rowBg),
                leftGutter = {
                    Box(
                        modifier = Modifier.background(gutterBg),
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
                },
                center = {
                    Box(
                        modifier = Modifier
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
                },
                rightGutter = {
                    Box(
                        modifier = Modifier
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
            )
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
        SyntaxCatalog.evaluatedExampleFor(item.expr)
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
                        color = colors.syntax.plain,
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
