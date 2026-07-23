package com.scizor.feature.network

import org.json.JSONArray
import org.json.JSONObject

/** Parsed GraphQL details of a request. */
internal data class GraphQLInfo(
    val operationName: String?,
    val operationType: String?,
    val variables: String?,
)

/** Detects and parses GraphQL request bodies, mirroring Scyther's GraphQLOperation. */
internal object GraphQL {

    fun parse(url: String, body: String?): GraphQLInfo? {
        if (body.isNullOrBlank()) return null
        val looksGraphQL = url.contains("graphql", true) || url.contains("/gql", true) ||
            body.contains("\"query\"")
        if (!looksGraphQL) return null

        // Batched requests are a top-level JSON array of operations (Scyther: "Batch (N operations)").
        if (body.trimStart().startsWith("[")) {
            return runCatching {
                val arr = JSONArray(body)
                val first = arr.optJSONObject(0)
                if (arr.length() == 0 || first == null || !first.has("query")) return@runCatching null
                GraphQLInfo("Batch (${arr.length()} operations)", "Batch", body)
            }.getOrNull()
        }

        return runCatching {
            val obj = JSONObject(body)
            val query = obj.optString("query")
            val explicitName = obj.optString("operationName").takeIf { it.isNotBlank() }
            val name = explicitName ?: parseOperationName(query)
            val type = parseOperationType(query)
            val variables = if (obj.has("variables") && !obj.isNull("variables")) {
                obj.get("variables").toString()
            } else {
                null
            }
            if (name == null && type == null) null else GraphQLInfo(name, type, variables)
        }.getOrNull()
    }

    private fun parseOperationType(query: String): String? {
        val trimmed = query.trimStart()
        return when {
            trimmed.startsWith("mutation") -> "Mutation"
            trimmed.startsWith("subscription") -> "Subscription"
            trimmed.startsWith("query") || trimmed.startsWith("{") -> "Query"
            else -> null
        }
    }

    private fun parseOperationName(query: String): String? =
        Regex("(query|mutation|subscription)\\s+(\\w+)").find(query)?.groupValues?.getOrNull(2)
}
