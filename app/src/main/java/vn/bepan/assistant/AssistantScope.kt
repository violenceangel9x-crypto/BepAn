package vn.bepan.assistant

import org.json.JSONObject
import vn.bepan.domain.*
import java.time.YearMonth

data class AssistantScope(val route: String = "TODAY", val period: String = "", val group: String = "", val filter: String = "ALL") {
    fun json(selected: String, reference: String): JSONObject {
        requireRule(route in setOf("TODAY", "FINANCE", "NOTES", "SETTINGS") && filter in setOf("ALL", "CUT", "DUE", "EXCESS"), "INVALID_ARGUMENT")
        if (group.isNotEmpty()) Group.valueOf(group)
        val month = period.ifEmpty { selected.take(7) }; YearMonth.parse(month)
        return JSONObject().put("selectedDate", selected).put("referenceDate", reference).put("timeZone", "Asia/Ho_Chi_Minh")
            .put("selectedPeriod", month).put("route", route).put("selectedGroup", group).put("selectedFilter", filter)
    }
    fun resolvePeriod(args: JSONObject, selected: String): String {
        val value = args.optString("period").takeUnless { it.isBlank() || it == "VIEW_PERIOD" } ?: period.ifEmpty { selected.take(7) }
        return YearMonth.parse(value).toString()
    }
}
