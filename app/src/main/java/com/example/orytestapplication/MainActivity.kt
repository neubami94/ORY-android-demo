package com.example.orytestapplication

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.orytestapplication.ui.theme.ORYTestApplicationTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ORYTestApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun Content(modifier: Modifier = Modifier) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://elated-newton-tuttp6nhc3.projects.oryapis.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        val savedLoginResponse = remember { mutableStateOf<SubmitLoginFlowResponse?>(null) }
        val message = remember { mutableStateOf(String()) }

        val context = LocalContext.current
        Button(
            onClick = {
                Log.d(TAG, "Button 1 clicked")
                showCustomTab(context)
            }
        ) {
            Text(
                text = "Show custom tab"
            )
        }

        val email = savedLoginResponse.value?.session?.identity?.traits?.email ?: "Logged out"
        Text(
            text = "Email: $email"
        )
        val token = savedLoginResponse.value?.sessionToken ?: "Logged out"
        Text(
            text = "Token: $token"
        )

        Button(
            onClick = {
                Log.d(TAG, "Button 2 clicked")
                sendLoginRequestWithRetrofit(
                    apiService = apiService,
                    username = "asdf@asdf.as",
                    password = "3ubyduby",
                    showMessage = { message.value = it },
                    saveResponse = { savedLoginResponse.value = it },
                )
            }
        ) {
            Text(
                text = "Send correct login request"
            )
        }

        Button(
            onClick = {
                Log.d(TAG, "Button 3 clicked")
                sendLoginRequestWithRetrofit(
                    apiService = apiService,
                    username = "1234@1234.12",
                    password = "šubyduby",
                    showMessage = { message.value = it },
                    saveResponse = { savedLoginResponse.value = it },
                )
            }
        ) {
            Text(
                text = "Send incorrect login request"
            )
        }

        Button(
            onClick = {
                Log.d(TAG, "Button 4 clicked")
                sendLogoutRequestWithRetrofit(
                    apiService = apiService,
                    sessionToken = savedLoginResponse.value?.sessionToken,
                    showMessage = { message.value = it },
                    saveResponse = { savedLoginResponse.value = it },
                )
            }
        ) {
            Text(
                text = "Send correct logout request"
            )
        }

        Button(
            onClick = {
                Log.d(TAG, "Button 5 clicked")
                sendLogoutRequestWithRetrofit(
                    apiService = apiService,
                    sessionToken = "1234",
                    showMessage = { message.value = it },
                    saveResponse = { savedLoginResponse.value = it },
                )
            }
        ) {
            Text(
                text = "Send incorrect logout request"
            )
        }

        Text(
            text = message.value
        )
    }
}

private fun showCustomTab(context: Context) {
    var mClient: CustomTabsClient?
    var mCustomTabsSession: CustomTabsSession? = null

    CustomTabsClient.bindCustomTabsService(
        context,
        "com.android.chrome",
        object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(componentName: ComponentName, customTabsClient: CustomTabsClient) {
                // Pre-warming
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(null)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
            }
        }
    )

    CustomTabsIntent.Builder(mCustomTabsSession)
        .setShowTitle(true)
        .build()
        .launchUrl(context, Uri.parse("https://elated-newton-tuttp6nhc3.projects.oryapis.com/ui/login"))
}

private fun sendLoginRequestWithRetrofit(
    apiService: ApiService,
    username: String,
    password: String,
    showMessage: (String) -> Unit,
    saveResponse: (SubmitLoginFlowResponse?) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        Log.d(TAG, "Calling api (creating login flow)")
        showMessage("Calling api (creating login flow)")

        val createLoginFlowResponse = apiService.createNativeLoginFlow(true)

        val loginFlow = createLoginFlowResponse.body()
        if (createLoginFlowResponse.isSuccessful && loginFlow != null) {

            Log.d(TAG, "Login Flow: $loginFlow")
            Log.d(TAG, "Calling api (submitting login flow)")
            showMessage("Calling api (submitting login flow)")

            val loginResponse = apiService.submitLoginFlow(
                loginFlow.id,
                SubmitLoginFlowBody(
                    username,
                    "password",
                    password,
                )
            )

            val loginResponseBody = loginResponse.body()
            if (loginResponse.isSuccessful && loginResponseBody != null) {
                // Handle successful token exchange
                val sessionToken = loginResponseBody.sessionToken
                val receivedUsername = loginResponseBody.session.identity.traits.email

                Log.d(TAG, "Token: $sessionToken, Username: $receivedUsername")
                showMessage("Login reponse success!\nToken: $sessionToken\nUsername: $receivedUsername")

                // Store tokens securely
                saveResponse(loginResponseBody)
            } else {
                val errorCode = loginResponse.code()
                val errorMessage = loginResponse.errorBody().getErrorMessage()
                Log.e(TAG, "Status code: $errorCode")
                Log.e(TAG, "Response headers: " + loginResponse.headers())
                Log.e(TAG, "Reason: $errorMessage")
                showMessage("Login response error: $errorCode: $errorMessage")
                saveResponse(null) // TODO ?
            }
        } else {
            val errorCode = createLoginFlowResponse.code()
            val errorMessage = createLoginFlowResponse.errorBody().getErrorMessage()
            Log.e(TAG, "Status code: $errorCode")
            Log.e(TAG, "Response headers: " + createLoginFlowResponse.headers())
            Log.e(TAG, "Reason: $errorMessage")
            showMessage("Create login flow response error: $errorCode: $errorMessage")
            saveResponse(null) // TODO ?
        }
    }
}

private fun sendLogoutRequestWithRetrofit(
    apiService: ApiService,
    sessionToken: String?,
    showMessage: (String) -> Unit,
    saveResponse: (SubmitLoginFlowResponse?) -> Unit,
) {
    if (sessionToken != null) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Calling api (perform native logout)")
            showMessage("Calling api (perform native logout)")

            val logoutResponse = apiService.performNativeLogout(
                PerformNativeLogoutBody(
                    sessionToken = sessionToken
                )
            )

            val responseCode = logoutResponse.code()
            val responseHeaders = logoutResponse.headers()

            if (logoutResponse.isSuccessful) {
                Log.d(TAG, "Status code: $responseCode")
                Log.d(TAG, "Response headers: $responseHeaders")
                Log.d(TAG, "Logout successful")
                showMessage("Logout successful")
                saveResponse(null)
            } else {
                val errorMessage = logoutResponse.errorBody().getErrorMessage()
                Log.e(TAG, "Status code: $responseCode")
                Log.e(TAG, "Response headers: $responseHeaders")
                Log.e(TAG, "Reason: $errorMessage")
                showMessage("Perform native logout response error: $responseCode: $errorMessage")
            }
        }
    } else {
        Log.e(TAG, "Cannot perform a logout, already logged out")
        showMessage("Cannot perform a logout, already logged out")
    }
}

private fun ResponseBody?.getErrorMessage(): String {
    val errorBody = this?.let {
        Gson().fromJson<GenericErrorResponseBody?>(
            it.charStream(),
            object : TypeToken<GenericErrorResponseBody>() {}.type
        )
    }
    return errorBody?.ui?.messages?.firstOrNull { it.text.isNotEmpty() }?.text ?: "empty message"
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ORYTestApplicationTheme {
        Content()
    }
}

private const val TAG = "MainActivity"

// Define your API interface
interface ApiService {
    @GET("self-service/login/api")
    suspend fun createNativeLoginFlow(
        @Query("refresh") refresh: Boolean,
    ): Response<CreateNativeLoginFlowResponse>

    @POST("self-service/login")
    suspend fun submitLoginFlow(
        @Query("flow") flowId: String,
        @Body submitLoginFlowBody: SubmitLoginFlowBody,
    ): Response<SubmitLoginFlowResponse>

    // Cannot use @DELETE annotation due to java.lang.IllegalArgumentException: Non-body HTTP method cannot contain @Body.
    @HTTP(method = "DELETE", path = "self-service/logout/api", hasBody = true)
    suspend fun performNativeLogout(
        @Body performNativeLogoutBody: PerformNativeLogoutBody,
    ): Response<Void>
}

// Define your API response model
data class CreateNativeLoginFlowResponse(
    val id: String,
)

data class SubmitLoginFlowBody(
    val identifier: String,
    val method: String,
    val password: String,
)

data class SubmitLoginFlowResponse(
    @SerializedName("session_token") val sessionToken: String,
    val session: Session,
)

data class Session(
    val identity: Identity,
)

data class Identity(
    val traits: Traits,
)

data class Traits(
    val email: String
)

data class PerformNativeLogoutBody(
    @SerializedName("session_token") val sessionToken: String,
)

data class GenericErrorResponseBody(
    val ui: Ui,
)

data class Ui(
    val messages: List<Message>
)

data class Message(
    val text: String
)
