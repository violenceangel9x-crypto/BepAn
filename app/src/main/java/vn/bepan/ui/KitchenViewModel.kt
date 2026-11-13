package vn.bepan.ui

import android.app.Application
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import vn.bepan.*
import vn.bepan.assistant.*
import vn.bepan.data.*
import vn.bepan.diagnostics.*
import vn.bepan.domain.*
import java.time.Instant

data class PersonChoice(val call: Call, val date: String, val context: vn.bepan.domain.Context, val request: String, val people: List<Person>, val scope: AssistantScope = AssistantScope())
data class UiState(
    val date: String = today(), val snapshot: DaySnapshot? = null, val busy: Boolean = false,
    val error: String = "", val roomFailed: Boolean = false, val notice: String = "",
    val chat: List<ChatMessage> = emptyList(), val actions: List<AssistantAction> = emptyList(),
    val audits: List<Audit> = emptyList(), val notes: List<Note> = emptyList(),
    val choice: PersonChoice? = null, val manualPreview: Command? = null, val dayFinanceImpacts: List<DayFinanceImpact> = emptyList(),
    val draft: String = "", val mock: Boolean = BuildConfig.DEBUG, val endpoint: String = "",
    val connection: GeminiConnection = GeminiConnection.DIRECT, val hasApiKey: Boolean = false,
    val credentialBusy: Boolean = false, val credentialsReady: Boolean = false, val credentialError: String = "",
    val events: List<SafeEvent> = emptyList(), val selectedTrace: String = "", val evidence: RoomEvidence = RoomEvidence("UNKNOWN"),
    val finance: List<ClosedMeal> = emptyList(), val purchases: List<Purchase> = emptyList(),
    val people: List<Person> = emptyList(), val memberships: List<MembershipPeriod> = emptyList(), val rates: List<Rate> = emptyList(),
    val configurationPreview: ConfigurationCommand? = null, val financePeriod: String = today().take(7),
    val ledger: FinanceMonth? = null, val financePreview: FinanceCommand? = null, val lastFinanceReceipt: Receipt? = null,
    val exportingFinance: Boolean = false, val financeGroup: String = Group.OFFICER.name, val financeFilter: String = "ALL", val chatRoute: String = "TODAY"
)
class KitchenViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as BepApplication
    val log = app.log
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    private val repo get() = app.repository
    private val configuration get() = ConfigurationRepository(repo.db, log)
    private val financial get() = FinanceRepository(repo.db, log)
    private val mockKey = booleanPreferencesKey("mock")
    private val endpointKey = stringPreferencesKey("endpoint")
    private val connectionKey = stringPreferencesKey("gemini_connection")
    private val draftKey = stringPreferencesKey("draft")
    private val periodKey = stringPreferencesKey("finance_period")
    private val groupKey = stringPreferencesKey("finance_group")
    private val filterKey = stringPreferencesKey("finance_filter")
    private var draftJob: Job? = null
    init {
        viewModelScope.launch {
            app.settings.data.catch { emit(emptyPreferences()) }.collect { prefs ->
                val endpoint = prefs[endpointKey].orEmpty()
                val connection = runCatching { GeminiConnection.valueOf(prefs[connectionKey].orEmpty()) }.getOrDefault(if (endpoint.isNotBlank()) GeminiConnection.GATEWAY else GeminiConnection.DIRECT)
                val previousPeriod = state.value.financePeriod
                mutable.update { it.copy(mock = BuildConfig.DEBUG && (prefs[mockKey] ?: true), endpoint = endpoint, connection = connection, draft = prefs[draftKey] ?: "",
                    financePeriod = prefs[periodKey] ?: it.financePeriod, financeGroup = prefs[groupKey] ?: it.financeGroup, financeFilter = prefs[filterKey] ?: it.financeFilter) }
                if (previousPeriod != state.value.financePeriod && state.value.snapshot != null) refresh()
            }
        }
        viewModelScope.launch {
            try { val configured = app.apiKeyStore.read() != null; mutable.update { it.copy(hasApiKey = configured) } }
            catch (_: Exception) { mutable.update { it.copy(credentialError = "Không đọc được khóa đã lưu. Hãy nhập và lưu lại API key.") } }
            finally { mutable.update { it.copy(credentialsReady = true) } }
        }
        launch {
            repo.initialize(BuildConfig.DEBUG)
            val prefs = app.settings.data.first()
            mutable.update { it.copy(financePeriod = prefs[periodKey] ?: it.financePeriod, financeGroup = prefs[groupKey] ?: it.financeGroup, financeFilter = prefs[filterKey] ?: it.financeFilter) }
            refresh()
            launch { repo.dao.observeChat().collect { rows -> mutable.update { it.copy(chat = rows) } } }
            launch { repo.dao.observeActions().collect { rows -> mutable.update { it.copy(actions = rows) } } }
            launch { repo.dao.observeAudits().collect { rows -> mutable.update { it.copy(audits = rows) }; refresh() } }
            launch { repo.dao.observeNotes().collect { rows -> mutable.update { it.copy(notes = rows) } } }
        }
    }
    private fun launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch {
        try { block() } catch (e: Exception) {
            if (e is CancellationException) throw e
            val code = (e as? RuleViolation)?.code ?: "DATABASE_UNAVAILABLE"
            val trace = vn.bepan.domain.Context(source = Source.SYSTEM)
            if (code == "DATABASE_UNAVAILABLE") log.record(trace, EventCode.ROOM_OPEN_FAILED, reason = Reason.DATABASE_UNAVAILABLE)
            mutable.update { it.copy(error = errorText(code), busy = false, roomFailed = code == "DATABASE_UNAVAILABLE") }
        }
    }
    suspend fun refresh() {
        val date = state.value.date
        val period = state.value.financePeriod
        val snap = repo.snapshot(date)
        val people = repo.dao.people()
        val memberships = repo.dao.memberships(); val rates = repo.dao.rates()
        val ledger = financial.month(period)
        val draft = repo.dao.financeDrafts().firstOrNull { repo.dao.action(it.id) == null }?.let { FinanceCommand.decode(it.command) }
        mutable.update { if (it.date == date && it.financePeriod == period) it.copy(snapshot = snap, people = people, memberships = memberships, rates = rates, ledger = ledger, financePreview = draft) else it }
    }
    fun changeFinancePeriod(period: String) = launch {
        java.time.YearMonth.parse(period)
        mutable.update { it.copy(financePeriod = period, ledger = null) }
        app.settings.edit { it[periodKey] = period }
        refresh()
    }
    fun financeFilters(group: String = state.value.financeGroup, filter: String = state.value.financeFilter) = launch {
        Group.valueOf(group); requireRule(filter in setOf("ALL", "CUT", "DUE", "EXCESS"), "INVALID_ARGUMENT")
        mutable.update { it.copy(financeGroup = group, financeFilter = filter) }
        app.settings.edit { it[groupKey] = group; it[filterKey] = filter }
    }
    fun chatRoute(route: String) { if (route in setOf("TODAY", "FINANCE", "NOTES", "SETTINGS")) mutable.update { it.copy(chatRoute = route) } }
    fun previewFinance(kind: Kind, amount: Long = 0, people: List<String> = emptyList(), targetPeriod: String = "", settlementId: String = "", text: String = "", cashDate: String = today(), group: String = "", prepared: () -> Unit = {}) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = "") }
        launch {
            val command = financial.preview(kind, state.value.financePeriod, amount, people, targetPeriod, settlementId, text, cashDate, group)
            mutable.update { it.copy(financePreview = command, busy = false) }
            prepared()
        }
    }
    fun cancelFinance() = launch {
        state.value.financePreview?.let { financial.cancel(it) }
        mutable.update { it.copy(financePreview = null) }
    }
    fun confirmFinance() {
        val command = state.value.financePreview ?: return
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            val receipt = financial.commit(command)
            refresh()
            mutable.update { it.copy(busy = false, financePreview = null, lastFinanceReceipt = receipt, notice = "Đã lưu · ${command.operationId.take(8)}") }
        }
    }
    fun exportFinance(uri: android.net.Uri, period: String) {
        if (state.value.exportingFinance) return
        mutable.update { it.copy(exportingFinance = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val ledger = financial.month(period)
                    app.contentResolver.openOutputStream(uri, "w")?.use { FinanceExcel.write(ledger, it) }
                        ?: throw RuleViolation("EXPORT_FAILED")
                }
                mutable.update { it.copy(notice = "Đã xuất Excel đối soát · kỳ $period") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(error = "Chưa xuất được Excel. Dữ liệu sổ vẫn giữ nguyên; chọn tệp khác để thử lại.") }
            } finally { mutable.update { it.copy(exportingFinance = false) } }
        }
    }
    fun previewProfile(personId: String, name: String, effective: String, group: String, active: Boolean) = launch {
        mutable.update { it.copy(configurationPreview = configuration.profile(personId, name, effective, group, active), error = "") }
    }
    fun previewRate(rate: Rate) = launch { mutable.update { it.copy(configurationPreview = configuration.rate(rate), error = "") } }
    fun cancelConfiguration() { mutable.update { it.copy(configurationPreview = null) } }
    fun confirmConfiguration() {
        val command = state.value.configurationPreview ?: return
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            configuration.commit(command)
            refresh()
            mutable.update { it.copy(busy = false, configurationPreview = null, notice = "Đã lưu cấu hình có hiệu lực") }
        }
    }
    fun changeDate(date: String) = launch { java.time.LocalDate.parse(date); mutable.update { it.copy(date = date) }; refresh() }
    fun draft(text: String) {
        mutable.update { it.copy(draft = text) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch { delay(200); app.settings.edit { it[draftKey] = text } }
    }
    fun configure(mock: Boolean, endpoint: String) = launch { app.settings.edit { it[mockKey] = mock && BuildConfig.DEBUG; it[endpointKey] = endpoint.trim(); it[connectionKey] = GeminiConnection.GATEWAY.name } }
    fun setMock(mock: Boolean) = launch { app.settings.edit { it[mockKey] = mock && BuildConfig.DEBUG } }
    fun setConnection(connection: GeminiConnection) = launch { app.settings.edit { it[connectionKey] = connection.name } }
    fun saveApiKey(value: String, saved: () -> Unit) {
        if (!state.value.credentialsReady || state.value.credentialBusy || state.value.busy) return
        if (!ApiKeyStore.validInput(value.trim())) {
            mutable.update { it.copy(credentialError = "API key cần từ 16 đến 4096 ký tự và không có khoảng trắng ở giữa.") }; return
        }
        mutable.update { it.copy(credentialBusy = true, credentialError = "") }
        viewModelScope.launch {
            try {
                app.apiKeyStore.save(value)
                mutable.update { it.copy(hasApiKey = true) }
                app.settings.edit { it[connectionKey] = GeminiConnection.DIRECT.name; it[mockKey] = false }
                saved()
                mutable.update { it.copy(notice = "Đã lưu API key. Gemini dùng khóa trên điện thoại.") }
            } catch (_: Exception) { mutable.update { it.copy(credentialError = "Chưa lưu được API key. Vui lòng thử lại trên thiết bị này.") } }
            finally { mutable.update { it.copy(credentialBusy = false) } }
        }
    }
    fun deleteApiKey() {
        if (state.value.credentialBusy || state.value.busy) return
        mutable.update { it.copy(credentialBusy = true, credentialError = "") }
        viewModelScope.launch {
            try { app.apiKeyStore.clear(); mutable.update { it.copy(hasApiKey = false, notice = "Đã xóa API key trên điện thoại.") } }
            catch (_: Exception) { mutable.update { it.copy(credentialError = "Chưa xóa được API key. Vui lòng thử lại.") } }
            finally { mutable.update { it.copy(credentialBusy = false) } }
        }
    }
    fun clearError() { mutable.update { it.copy(error = "", notice = "") } }
    fun feedback(text: String, error: Boolean = false) { mutable.update { if(error) it.copy(error = text) else it.copy(notice = text) } }
    fun attendance(person: String, meal: Meal, enabled: Boolean) = launch {
        val snap = repo.snapshot(state.value.date)
        repo.commit(Command(kind = Kind.ATTENDANCE, date = snap.day.date, expectedVersion = snap.day.version, personId = person, meal = meal.name, enabled = enabled), vn.bepan.domain.Context())
        refresh()
    }
    fun preview(kind: Kind, amount: Long = 0, text: String = "", person: String = "", meal: String = "", end: String = "", enabled: Boolean = false) = launch {
        val snap = repo.snapshot(state.value.date)
        val preview = repo.prepare(Command(kind = kind, date = snap.day.date, expectedVersion = snap.day.version, amount = amount, text = text, personId = person, meal = meal, endDate = end, enabled = enabled))
        mutable.update { it.copy(manualPreview = preview.command, dayFinanceImpacts = preview.impacts) }
    }
    fun cancelManual() { mutable.update { it.copy(manualPreview = null, dayFinanceImpacts = emptyList()) } }
    fun confirmManual() {
        val command = state.value.manualPreview ?: return
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            repo.commit(command, vn.bepan.domain.Context())
            mutable.update { it.copy(busy = false, manualPreview = null, notice = "Đã lưu") }
            if (command.kind == Kind.CLOSE_DAY) changeDate(java.time.LocalDate.parse(command.date).plusDays(1).toString()) else refresh()
        }
    }
    fun saveMenu(date: String, meal: Meal, text: String, expectedVersion: Long, saved: () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            repo.commit(Command(kind = Kind.MENU, date = date, expectedVersion = expectedVersion, meal = meal.name, text = text), vn.bepan.domain.Context())
            refresh()
            mutable.update { it.copy(busy = false) }
            saved()
        }
    }
    fun note(text: String, note: Note? = null, pinned: Boolean = note?.pinned ?: false) = launch {
        val snap = repo.snapshot(state.value.date)
        repo.commit(Command(kind = Kind.NOTE, date = snap.day.date, expectedVersion = snap.day.version, personId = note?.id ?: "", text = text, enabled = pinned), vn.bepan.domain.Context())
    }
    fun send() {
        val message = state.value.draft.trim()
        if (message.isEmpty() || state.value.busy) return
        val frozen = state.value
        mutable.update { it.copy(busy = true, choice = null) }
        launch {
            val request = id()
            val reference = today()
            val ctx = vn.bepan.domain.Context(source = Source.ASSISTANT, provider = if (frozen.mock) Provider.MOCK else Provider.GEMINI, requestId = request)
            val user = ChatMessage(request, Instant.now().toString(), "USER", message, ctx.traceId, provider = ctx.provider.name, selectedDate = frozen.date, referenceDate = reference,
                route = frozen.chatRoute, selectedPeriod = if (frozen.chatRoute == "FINANCE") frozen.financePeriod else frozen.date.take(7), selectedGroup = frozen.financeGroup, selectedFilter = frozen.financeFilter)
            repo.dao.chat(user)
            interpret(user, ctx, frozen.endpoint, frozen.connection)
        }
    }
    private suspend fun interpret(user: ChatMessage, ctx: vn.bepan.domain.Context, endpoint: String, connection: GeminiConnection = state.value.connection) {
        log.record(ctx, EventCode.REQUEST_STARTED)
        try {
                val existing = repo.dao.actionForRequest(user.id)
                if (existing != null) {
                    reply(ctx, if (repo.dao.receipt(existing.operationId) != null) "Thao tác đã lưu. Mở kết quả bên dưới." else "Dùng lại bản xem trước của lượt này.", existing.operationId)
                    return
                }
                val provider: Gateway = when {
                    ctx.provider == Provider.MOCK -> MockGateway()
                    connection == GeminiConnection.GATEWAY -> HttpGateway(endpoint)
                    else -> DirectGeminiGateway(app.apiKeyStore::read,
                        { app.assets.open("gemini-system.vi.txt").bufferedReader(Charsets.UTF_8).use { it.readText() } },
                        { app.assets.open("gemini-functions.v1.json").bufferedReader(Charsets.UTF_8).use { it.readText() } })
                }
                val scope = AssistantScope(user.route, user.selectedPeriod, user.selectedGroup, user.selectedFilter)
                val result = provider.interpret(user.text, user.selectedDate, user.referenceDate, ctx.traceId, user.id, ctx.attempt, scope)
                log.record(ctx, EventCode.GATEWAY_RETURNED)
                requireRule(result.calls.size <= 1, "MULTI_STEP_UNSUPPORTED")
                if (result.calls.isEmpty()) reply(ctx, "Gợi ý từ Gemini · chưa ghi dữ liệu:\n" + result.text.ifEmpty { "Bạn vui lòng bổ sung yêu cầu." })
                else handle(result.calls.single(), user.selectedDate, user.referenceDate, ctx, user.id, scope = scope)
                draft("")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val code = (e as? RuleViolation)?.code ?: "GATEWAY_UNAVAILABLE"
                log.record(ctx, when(code) { "GATEWAY_TIMEOUT" -> EventCode.GATEWAY_TIMEOUT; "GATEWAY_UNAVAILABLE", "CONFIGURATION" -> EventCode.GATEWAY_UNAVAILABLE; else -> EventCode.VALIDATION_REJECTED }, reason = when(code) { "GATEWAY_TIMEOUT" -> Reason.TIMEOUT; "CONFIGURATION" -> Reason.CONFIGURATION; else -> Reason.PROTOCOL })
                reply(ctx, errorText(code), error = code)
            } finally { mutable.update { it.copy(busy = false) } }
    }
    fun retry(trace: String) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            val user = repo.dao.userMessage(trace) ?: throw RuleViolation("REQUEST_MISSING")
            requireRule(user.selectedDate.isNotEmpty() && user.referenceDate.isNotEmpty() && user.attempt < 1000, "REQUEST_MISSING")
            repo.dao.chatAttempt(user.id, user.attempt + 1)
            interpret(user, vn.bepan.domain.Context(trace,Source.ASSISTANT,Provider.valueOf(user.provider),user.id,user.attempt+1), state.value.endpoint)
        }
    }
    private suspend fun reply(ctx: vn.bepan.domain.Context, text: String, operation: String = "", error: String = "") {
        repo.dao.chat(ChatMessage(id(), Instant.now().toString(), "ASSISTANT", text, ctx.traceId, operation, error, provider = ctx.provider.name, attempt = ctx.attempt))
    }
    private suspend fun handle(call: Call, selected: String, reference: String, ctx: vn.bepan.domain.Context, request: String, selectedPerson: String = "", scope: AssistantScope = AssistantScope()) {
        if (call.name in FinanceAssistant.functions) {
            val result = repo.db.withTransaction {
                val result = FinanceAssistant(financial).propose(call, scope, selected, selectedPerson)
                result.command?.let { command ->
                    repo.dao.action(AssistantAction(command.operationId, ctx.traceId, request, ctx.provider.name, command.encode(), result.text))
                    reply(ctx, result.text, command.operationId)
                }
                result
            }
            if (result.choices.isNotEmpty()) mutable.update { it.copy(choice = PersonChoice(call, selected, ctx, request, result.choices, scope)) }
            if (result.command == null) reply(ctx, result.text)
            log.record(ctx, when { result.command != null -> EventCode.PREVIEW_READY; result.choices.isNotEmpty() -> EventCode.CLARIFICATION_REQUIRED; else -> EventCode.READ_COMPLETED }, result.command?.kind, result.command?.operationId.orEmpty())
            return
        }
        val date = CommandCodec.resolveDate(call.args, selected, reference)
        val snap = repo.snapshot(date)
        val args = call.args
        when(call.name) {
            "get_meal_counts" -> { reply(ctx, "$date · ${if (snap.day.closed) "Đã chốt" else "Nháp"}\n" + Meal.entries.joinToString(" · ") { "${it.label}: ${snap.count(it)} suất" }); log.record(ctx, EventCode.READ_COMPLETED) }
            "get_shopping_budget" -> { reply(ctx, "$date · Ngân sách ${money(snap.budget)}\nĐã mua ${money(snap.bought)} · Chênh lệch ${money(snap.budget - snap.bought)}"); log.record(ctx, EventCode.READ_COMPLETED) }
            "set_attendance" -> {
                requireRule(args.has("enabled"), "INVALID_ARGUMENT")
                val meal = Meal.valueOf(args.getString("meal"))
                val rawQuery = normalized(args.optString("personName")).trim()
                val query = rawQuery.replace(Regex("^(chien si|can bo|khach)\\s+"), "")
                val group = args.optString("group").ifEmpty { if (rawQuery.startsWith("chien si ")) "SOLDIER" else if (rawQuery.startsWith("can bo ")) "OFFICER" else "" }
                requireRule(selectedPerson.isNotEmpty() || query.isNotEmpty() || args.optString("personId").isNotEmpty(), "INVALID_ARGUMENT")
                val people = snap.servings.map { it.person }.distinctBy { it.id }.filter { group.isEmpty() || it.group == group }.filter { p ->
                    if (selectedPerson.isNotEmpty()) p.id == selectedPerson else if (args.optString("personId").isNotEmpty()) p.id == args.getString("personId") else normalized(p.name).split(' ').takeLast(query.split(' ').size).joinToString(" ") == query
                }
                if (people.size != 1) {
                    log.record(ctx, EventCode.CLARIFICATION_REQUIRED)
                    if (people.isEmpty()) reply(ctx, "Chưa tìm thấy người phù hợp. Bạn nhập tên đầy đủ nhé.")
                    else { mutable.update { it.copy(choice = PersonChoice(call, date, ctx, request, people)) }; reply(ctx, "Có nhiều người trùng tên. Chọn đúng người bên dưới.") }
                    return
                }
                val enabled = args.getBoolean("enabled")
                val person = people.single()
                val existing = snap.servings.first { it.person.id == person.id && it.meal == meal }
                if (existing.enabled == enabled) { reply(ctx, "${person.name} · $date · ${meal.label} đã ở trạng thái yêu cầu."); return }
                val command = Command(kind = Kind.ATTENDANCE, date = date, expectedVersion = snap.day.version, personId = person.id, meal = meal.name, enabled = enabled)
                val summary = "${person.name} · $date · ${meal.label}: ${if (enabled) "Có ăn" else "Cắt cơm"}"
                storeAction(command, ctx, request, summary)
                repo.commit(command, ctx)
                refresh()
            }
            "record_purchase", "close_day" -> {
                val kind = if (call.name == "record_purchase") Kind.PURCHASE else Kind.CLOSE_DAY
                val amount = if (kind == Kind.PURCHASE) {
                    val number = args.get("amountVnd")
                    requireRule(number is Number && number.toString().toBigDecimal().stripTrailingZeros().scale() <= 0, "INVALID_ARGUMENT")
                    number.toString().toBigDecimal().longValueExact().also { requireRule(it in 1..1_000_000_000L, "INVALID_ARGUMENT") }
                } else 0L
                requireRule(!snap.day.closed, "DAY_CLOSED")
                val preview = repo.prepare(Command(kind = kind, date = date, expectedVersion = snap.day.version, amount = amount, text = args.optString("description")))
                val command = preview.command
                val summary = if (kind == Kind.PURCHASE) "$date · ${command.text} · ${money(amount)}" else "$date · Sáng ${snap.count(Meal.BREAKFAST)} / Trưa ${snap.count(Meal.LUNCH)} / Tối ${snap.count(Meal.DINNER)}\nTiền ăn ${money(snap.charge)} · Đã mua ${money(snap.bought)}"
                storeAction(command, ctx, request, summary + if (preview.impacts.isEmpty()) "" else "\n" + preview.impacts.joinToString("\n") { it.summary() })
                log.record(ctx, EventCode.PREVIEW_READY, kind, command.operationId)
            }
            else -> throw RuleViolation("COMMAND_UNSUPPORTED")
        }
    }
    private suspend fun storeAction(command: Command, ctx: vn.bepan.domain.Context, request: String, summary: String) {
        repo.dao.action(AssistantAction(command.operationId, ctx.traceId, request, ctx.provider.name, CommandCodec.encode(command), summary))
        reply(ctx, summary, command.operationId)
    }
    fun choose(person: String) {
        val choice = state.value.choice ?: return
        mutable.update { it.copy(choice = null) }
        launch { handle(choice.call, choice.date, choice.date, choice.context, choice.request, person, choice.scope) }
    }
    fun confirm(action: AssistantAction) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        launch {
            val ctx = vn.bepan.domain.Context(action.traceId, Source.ASSISTANT, Provider.valueOf(action.provider), action.requestId)
            try {
                val latest = repo.dao.action(action.operationId) ?: throw RuleViolation("ACTION_MISSING")
                requireRule(latest.status in listOf("PENDING", "SAVED"), "ACTION_EXPIRED")
                log.record(ctx, EventCode.CONFIRMATION_RECEIVED, operationId = action.operationId)
                if (org.json.JSONObject(action.command).has("expected")) {
                    val receipt = financial.commit(FinanceCommand.decode(action.command), ctx)
                    mutable.update { it.copy(lastFinanceReceipt = receipt) }
                } else repo.commit(CommandCodec.decode(action.command), ctx)
                refresh()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val code = (e as? RuleViolation)?.code ?: "DATABASE_UNAVAILABLE"
                if (code in listOf("DAY_CLOSED", "STALE_VERSION")) repo.dao.actionStatus(action.operationId, "EXPIRED")
                reply(ctx, errorText(code), error = code)
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun cancel(action: AssistantAction) = launch {
        // Commit and cancel are serialized by Room. Cancellation cannot overwrite a saved card.
        repo.db.withTransaction {
            if (repo.dao.receipt(action.operationId) == null && repo.dao.action(action.operationId)?.status == "PENDING") {
                repo.dao.actionStatus(action.operationId, "CANCELLED")
                repo.dao.deleteFinanceDraft(action.operationId)
            }
        }
        log.record(vn.bepan.domain.Context(action.traceId, Source.ASSISTANT, Provider.valueOf(action.provider)), EventCode.USER_CANCELLED, operationId = action.operationId)
    }
    fun undo(action: AssistantAction) = launch {
        repo.undo(CommandCodec.decode(action.command), vn.bepan.domain.Context(source = Source.ASSISTANT, provider = Provider.valueOf(action.provider)))
        refresh()
    }
    fun diagnostics(trace: String = "") = launch {
        val events = log.snapshot()
        val selected = trace.ifEmpty { events.lastOrNull()?.traceId ?: "" }
        val evidence = if (selected.isEmpty()) RoomEvidence("UNKNOWN") else runCatching { repo.evidence(selected) }.getOrDefault(RoomEvidence("UNKNOWN"))
        mutable.update { it.copy(events = events, selectedTrace = selected, evidence = evidence) }
    }
    fun clearLog() = launch { log.clear(); diagnostics() }
    companion object {
        fun errorText(code: String): String = when(code) {
            "SETTLEMENT_MISSING" -> "Không tìm thấy khoản chờ hoàn. Mở lại sổ để xem trạng thái mới."
            "SURPLUS_UNAVAILABLE" -> "Phần dư không còn đủ hoặc cần đối soát sau điều chỉnh. Hủy khoản chờ hoàn hoặc bổ sung thu rồi xem trước lại."
            "OPENING_EXISTS" -> "Số dư đầu quỹ đã được xác lập. Không thể tạo thêm lần nữa."
            "EFFECTIVE_DATE_EXISTS" -> "Ngày hiệu lực này đã có cấu hình. Chọn một ngày hiệu lực khác để giữ lịch sử."
            "NOT_REGISTERED" -> "Bữa này chưa đăng ký hoặc sổ cũ chưa đủ bằng chứng. Không thể bật suất bằng điều chỉnh."
            "DAY_CLOSED" -> "Ngày đã chốt. Cần luồng điều chỉnh có lý do."
            "STALE_VERSION", "ACTION_EXPIRED" -> "Bản xem trước đã cũ. Hãy tạo lại từ dữ liệu mới."
            "GATEWAY_TIMEOUT" -> "Kết nối Gemini quá thời gian. Bản nháp được giữ để gửi lại."
            "CONFIGURATION" -> "Chưa có kết nối Gemini hợp lệ. Kiểm tra Cài đặt."
            "API_KEY_MISSING" -> "Chưa có API key. Mở Cài đặt → Gemini để nhập và lưu khóa."
            "CREDENTIAL_UNAVAILABLE" -> "Không đọc được API key đã lưu. Mở Cài đặt để nhập lại khóa."
            "API_KEY_REJECTED" -> "Gemini từ chối API key hoặc quyền truy cập. Kiểm tra khóa trong Cài đặt."
            "MODEL_UNAVAILABLE" -> "Model Gemini hiện không khả dụng. Bạn vẫn có thể thao tác bằng tay."
            "RATE_LIMITED" -> "Gemini đang giới hạn lượt gọi. Chờ một lúc rồi thử lại; bản nháp được giữ."
            "GATEWAY_UNAVAILABLE" -> "Chưa kết nối được Gemini. Bạn vẫn có thể thao tác bằng tay."
            "SCHEDULE_CONFLICT" -> "Lịch đang chồng nhau với lựa chọn trái ngược. Hãy kiểm tra phạm vi."
            "DATABASE_UNAVAILABLE" -> "Không đọc được dữ liệu. Mở Chẩn đoán để xem log còn có."
            "AUDIT_FAILED" -> "Chưa lưu được lịch sử; thay đổi đã được hoàn nguyên."
            "MULTI_STEP_UNSUPPORTED" -> "Bản này hỗ trợ từng thao tác. Hãy gửi riêng từng yêu cầu."
            else -> "Yêu cầu chưa được lưu. Kiểm tra thông tin và thử lại."
        }
    }
}
