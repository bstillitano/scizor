package com.scizor.sample

import com.scizor.Scizor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Shared OkHttp client wired to Scizor's interceptor, plus demo request helpers. */
object SampleNetwork {

    private const val GRAPHQL_ENDPOINT = "https://graphqlzero.almansi.me/api"

    val restUrls = listOf(
        "https://jsonplaceholder.typicode.com/posts/1",
        "https://jsonplaceholder.typicode.com/users/1",
        "https://jsonplaceholder.typicode.com/comments?postId=1",
        "https://jsonplaceholder.typicode.com/todos/1",
        "https://httpbin.org/get",
        "https://httpbin.org/json",
    )

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().addInterceptor(Scizor.network.interceptor()).build()
    }

    /** Runs a blocking GET. Call from a background dispatcher. */
    fun get(url: String) {
        runCatching { client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() } }
    }

    /** Posts a named GraphQL operation to the GraphQLZero demo endpoint. */
    fun graphQL(operationName: String, query: String, variables: JSONObject) {
        val payload = JSONObject()
            .put("operationName", operationName)
            .put("query", query)
            .put("variables", variables)
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(GRAPHQL_ENDPOINT).post(body).build()
        runCatching { client.newCall(request).execute().use { it.body?.string() } }
    }

    fun graphQLQuery() = graphQL(
        operationName = "GetUser",
        query = "query GetUser(\$id: ID!) { user(id: \$id) { id name email } }",
        variables = JSONObject().put("id", 1),
    )

    fun graphQLMutation() = graphQL(
        operationName = "CreatePost",
        query = "mutation CreatePost(\$input: CreatePostInput!) { createPost(input: \$input) { id title body } }",
        variables = JSONObject().put(
            "input",
            JSONObject().put("title", "Scizor").put("body", "Testing GraphQL logging"),
        ),
    )
}
