package org.publicvalue.multiplatform.oidc.appsupport

import kotlinx.coroutines.CancellableContinuation
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.flows.AuthCodeResponse
import org.publicvalue.multiplatform.oidc.flows.AuthCodeResult
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlow
import org.publicvalue.multiplatform.oidc.flows.EndSessionFlow
import org.publicvalue.multiplatform.oidc.flows.EndSessionResponse
import org.publicvalue.multiplatform.oidc.types.AuthCodeRequest
import org.publicvalue.multiplatform.oidc.types.EndSessionRequest
import org.publicvalue.multiplatform.oidc.wrapExceptions
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalObjCName

/**
 * Implements the OAuth 2.0 Code Authorization Flow.
 * See: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
 *
 * Implementations have to provide their own method to get the authorization code,
 * as this requires user interaction (e.g. via browser).
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "CodeAuthFlow", name = "CodeAuthFlow", exact = true)
actual class PlatformCodeAuthFlow(
    actual override val client: OpenIdConnectClient,
    ephemeralBrowserSession: Boolean = false
): CodeAuthFlow, EndSessionFlow {

    private val webFlow = WebSessionFlow(
        ephemeralBrowserSession
    )

    // required for swift (no default argument support)
    constructor(client: OpenIdConnectClient) : this(client = client, ephemeralBrowserSession = false)

    actual override suspend fun getAuthorizationCode(request: AuthCodeRequest): AuthCodeResponse = wrapExceptions {
        val resultUrl = webFlow.startWebFlow(request.url, request.url.parameters.get("redirect_uri").orEmpty())
        return if (resultUrl != null) {
            val code = resultUrl.parameters["code"] ?: ""
            val state = resultUrl.parameters["state"] ?: ""
            AuthCodeResponse.success(AuthCodeResult(code = code, state = state))
        } else {
            AuthCodeResponse.failure(OpenIdConnectException.AuthenticationCancelled("Authentication cancelled"))
        }
    }

    actual override suspend fun endSession(request: EndSessionRequest): EndSessionResponse = wrapExceptions {
        val resultUrl = webFlow.startWebFlow(request.url, request.url.parameters.get("post_logout_redirect_uri").orEmpty())
        return if (resultUrl != null) {
            EndSessionResponse.success(Unit)
        } else {
            EndSessionResponse.failure(OpenIdConnectException.AuthenticationCancelled("Logout cancelled"))
        }
    }
}

class PresentationContext: NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
        return ASPresentationAnchor()
    }
}

/** fix for multiple callbacks from ASWebAuthenticationSession (https://github.com/kalinjul/kotlin-multiplatform-oidc/issues/89) **/
fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}

/** fix for multiple callbacks from ASWebAuthenticationSession (https://github.com/kalinjul/kotlin-multiplatform-oidc/issues/89) **/
fun <T> CancellableContinuation<T>.resumeWithExceptionIfActive(value: Exception) {
    if (isActive) {
        resumeWithException(value)
    }
}