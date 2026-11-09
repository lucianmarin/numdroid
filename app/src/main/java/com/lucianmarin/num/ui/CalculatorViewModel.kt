package com.lucianmarin.num.ui

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.lucianmarin.num.math.EvalLineResult
import com.lucianmarin.num.math.MathEngine
import com.lucianmarin.num.math.MathValue
import com.lucianmarin.num.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: EvalLineResult = EvalLineResult(null, null, null, isComment = true),
    val isReferenced: Boolean = false
)

data class ScopeSummary(
    val variables: Map<String, String> = emptyMap(),
    val functions: List<String> = emptyList(),
    val numericSum: Double? = null,
    val numericAvg: Double? = null,
    val numericCount: Int = 0
)

data class CalculatorUiState(
    val history: List<HistoryItem> = emptyList(),
    val currentInput: TextFieldValue = TextFieldValue(""),
    val livePreview: EvalLineResult = EvalLineResult(null, null, null, isComment = true),
    val scopeSummary: ScopeSummary = ScopeSummary(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showSyntaxSheet: Boolean = false,
    val showScopeBar: Boolean = true,
    val canUndo: Boolean = false
)

class CalculatorViewModel : ViewModel() {

    companion object {
        private const val PREFS_NAME = "num_prefs"
        private const val KEY_HISTORY = "history_json_v1"
        private const val KEY_INPUT = "current_input_v1"
        private const val KEY_THEME = "theme_mode_v1"
        const val MAX_LINES = 99
        private const val MAX_HISTORY_ITEMS = MAX_LINES - 1

        val DEFAULT_EXPRESSIONS = listOf(
            "# Welcome to Numeric — a smart math calculator",
            "radius = 12.5 cm",
            "area = pi * radius^2 in cm2",
            "speed = 110 km/h in m/s",
            "hypot2(a, b) = sqrt(a^2 + b^2)",
            "hypot2(3, 4) + (2 + 3i) * (1 - i)",
            "A = [1, 2; 3, 4]; det(A)",
            "ans * 10"
        )
    }

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<List<String>>()
    private var appContext: Context? = null

    init {
        val initialItems = DEFAULT_EXPRESSIONS.map { HistoryItem(expression = it) }
        recalculateAll(initialItems, TextFieldValue(""))
    }

    fun attachContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        loadPersistedState()
    }

    private fun loadPersistedState() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeStr = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val savedInput = prefs.getString(KEY_INPUT, "") ?: ""
        val historyJson = prefs.getString(KEY_HISTORY, null)

        val loadedItems = if (historyJson != null) {
            runCatching {
                val arr = JSONArray(historyJson)
                val list = mutableListOf<HistoryItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            expression = obj.optString("expression", "")
                        )
                    )
                }
                list
            }.getOrNull()
        } else {
            null
        }

        val itemsToUse = loadedItems ?: DEFAULT_EXPRESSIONS.map { HistoryItem(expression = it) }
        _uiState.update { it.copy(themeMode = themeMode) }
        recalculateAll(
            items = itemsToUse,
            input = TextFieldValue(savedInput, TextRange(savedInput.length))
        )
    }

    private fun persistState(state: CalculatorUiState) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (item in state.history) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("expression", item.expression)
            arr.put(obj)
        }
        prefs.edit()
            .putString(KEY_HISTORY, arr.toString())
            .putString(KEY_INPUT, state.currentInput.text)
            .putString(KEY_THEME, state.themeMode.name)
            .apply()
    }

    private fun pushUndoSnapshot(items: List<HistoryItem>) {
        val snapshot = items.map { it.expression }
        if (undoStack.lastOrNull() != snapshot) {
            if (undoStack.size >= 30) {
                undoStack.removeFirst()
            }
            undoStack.addLast(snapshot)
        }
    }

    fun onInputChange(newValue: TextFieldValue) {
        val currentHistory = _uiState.value.history
        recalculateAll(currentHistory, newValue)
    }

    fun submitCurrentInput() {
        val current = _uiState.value
        if (current.history.size >= MAX_HISTORY_ITEMS) return
        val text = current.currentInput.text.trim()
        if (text.isEmpty()) return

        pushUndoSnapshot(current.history)
        val newHistory = current.history + HistoryItem(expression = text)
        recalculateAll(newHistory, TextFieldValue(""))
    }

    fun updateHistoryItem(id: String, newExpression: String) {
        val current = _uiState.value
        val idx = current.history.indexOfFirst { it.id == id }
        if (idx == -1) return
        if (current.history[idx].expression == newExpression) return

        pushUndoSnapshot(current.history)
        val updated = current.history.toMutableList()
        updated[idx] = updated[idx].copy(expression = newExpression)
        recalculateAll(updated, current.currentInput)
    }

    fun deleteHistoryItem(id: String) {
        val current = _uiState.value
        val targetIdx = current.history.indexOfFirst { it.id == id }
        if (targetIdx == -1) return
        val target = current.history[targetIdx]
        if (target.isReferenced) return

        val deletedLineNumber = targetIdx + 1
        val updated = current.history
            .filterIndexed { idx, _ -> idx != targetIdx }
            .map { item ->
                item.copy(
                    expression = MathEngine.shiftReferencedLineNumbersAfterDeletion(
                        item.expression,
                        deletedLineNumber
                    )
                )
            }

        val updatedInputText = MathEngine.shiftReferencedLineNumbersAfterDeletion(
            current.currentInput.text,
            deletedLineNumber
        )
        val updatedInput = if (updatedInputText != current.currentInput.text) {
            current.currentInput.copy(
                text = updatedInputText,
                selection = TextRange(
                    current.currentInput.selection.start.coerceIn(0, updatedInputText.length),
                    current.currentInput.selection.end.coerceIn(0, updatedInputText.length)
                )
            )
        } else {
            current.currentInput
        }

        pushUndoSnapshot(current.history)
        recalculateAll(updated, updatedInput)
    }

    fun clearHistory() {
        val current = _uiState.value
        if (current.history.isEmpty() && current.currentInput.text.isEmpty()) return
        pushUndoSnapshot(current.history)
        recalculateAll(emptyList(), TextFieldValue(""))
    }

    fun resetToDemo() {
        val current = _uiState.value
        pushUndoSnapshot(current.history)
        val demoItems = DEFAULT_EXPRESSIONS.map { HistoryItem(expression = it) }
        recalculateAll(demoItems, TextFieldValue(""))
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        val restored = previous.map { HistoryItem(expression = it) }
        recalculateAll(restored, _uiState.value.currentInput)
    }

    fun insertIntoCurrentInput(snippet: String, addAsNewHistoryItem: Boolean = false) {
        val current = _uiState.value
        if (addAsNewHistoryItem) {
            if (current.history.size >= MAX_HISTORY_ITEMS) return
            pushUndoSnapshot(current.history)
            val newHistory = current.history + HistoryItem(expression = snippet)
            recalculateAll(newHistory, current.currentInput)
            return
        }

        val tf = current.currentInput
        val start = tf.selection.min.coerceIn(0, tf.text.length)
        val end = tf.selection.max.coerceIn(0, tf.text.length)
        val newText = tf.text.substring(0, start) + snippet + tf.text.substring(end)
        val newCursor = start + snippet.length
        recalculateAll(
            current.history,
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        )
    }

    fun setCurrentInputText(expression: String) {
        recalculateAll(
            _uiState.value.history,
            TextFieldValue(text = expression, selection = TextRange(expression.length))
        )
    }

    fun cycleThemeMode() {
        val next = when (_uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        _uiState.update { it.copy(themeMode = next) }
        persistState(_uiState.value)
    }

    fun toggleSyntaxSheet(show: Boolean) {
        _uiState.update { it.copy(showSyntaxSheet = show) }
    }

    fun toggleScopeBar() {
        _uiState.update { it.copy(showScopeBar = !it.showScopeBar) }
    }

    private fun recalculateAll(items: List<HistoryItem>, input: TextFieldValue) {
        val cappedItems = if (items.size > MAX_HISTORY_ITEMS) items.take(MAX_HISTORY_ITEMS) else items
        val expressions = cappedItems.map { it.expression }
        val (evaluatedList, scope) = MathEngine.evaluateLines(expressions)

        val refsByLine = expressions.map { MathEngine.extractReferencedLineNumbers(it) }
        val inputRefs = MathEngine.extractReferencedLineNumbers(input.text)

        val updatedHistory = cappedItems.mapIndexed { idx, item ->
            val lineNumber = idx + 1
            val isRef = inputRefs.contains(lineNumber) ||
                refsByLine.withIndex().any { (otherIdx, refs) -> otherIdx != idx && refs.contains(lineNumber) }
            item.copy(
                result = evaluatedList.getOrElse(idx) { EvalLineResult(null, null, null, true) },
                isReferenced = isRef
            )
        }

        // Evaluate live preview for the bottom input bar using a snapshot of the scope
        val previewScope = scope.snapshot()
        val livePreview = MathEngine.evaluateSingleLine(input.text, previewScope)

        val numericVals = evaluatedList.mapNotNull { res ->
            val v = res.value
            if (v is MathValue.NumberVal && v.value.isFinite()) v.value else null
        }

        val sum = if (numericVals.isNotEmpty()) numericVals.sum() else null
        val avg = if (numericVals.isNotEmpty()) sum!! / numericVals.size else null

        val varsMap = scope.variables
            .filterKeys { it != "ans" && !MathEngine.LINE_VAR_REGEX.matches(it) }
            .filterValues { it !is MathValue.FunctionVal }
            .mapValues { (_, v) -> v.format() }

        val fnList = scope.functions.values.map { fn ->
            "${fn.name}(${fn.params.joinToString(", ")})"
        }

        val summary = ScopeSummary(
            variables = varsMap,
            functions = fnList,
            numericSum = sum,
            numericAvg = avg,
            numericCount = numericVals.size
        )

        _uiState.update { state ->
            state.copy(
                history = updatedHistory,
                currentInput = input,
                livePreview = livePreview,
                scopeSummary = summary,
                canUndo = undoStack.isNotEmpty()
            )
        }
        persistState(_uiState.value)
    }
}
