package com.jeongum.springlogging.filter

import com.jeongum.springlogging.config.HeaderKeys
import com.sun.org.apache.bcel.internal.Const
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.*

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class HttpLoggingFilter : OncePerRequestFilter() {
    private val httpLogger: Logger = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.currentTimeMillis()
        val reqWrapper = ContentCachingRequestWrapper(request)
        val resWrapper = ContentCachingResponseWrapper(response)

        val requestId = "noun-" + UUID.randomUUID().toString().replace("-", "")
        MDC.put(HeaderKeys.TRACE_ID, requestId)
        resWrapper.setHeader(HeaderKeys.TRACE_ID, requestId)

        filterChain.doFilter(reqWrapper, resWrapper)

        val endTime = System.currentTimeMillis()
        logRequestInfo(reqWrapper, resWrapper, endTime - startTime)

        resWrapper.copyBodyToResponse()
    }

    private fun logRequestInfo(
        request: ContentCachingRequestWrapper, response: ContentCachingResponseWrapper, elapsed: Long
    ) {
        if (listOf("/v3/api-docs", "/swagger-ui").any { request.requestURI.contains(it) }) return

        fun makeHeaders(headerNames: List<String>, getHeaders: (String) -> String) =
            headerNames.associateWith { getHeaders(it) }
                .filterKeys { key -> listOf("token", "Token").none { key.contains(it) } }

        val requestData = """
            |
            |[HTTP] ${request.method} ${request.requestURI} - ${response.status} (elapsed: ${elapsed})
            |REQUEST_HEADERS >> ${makeHeaders(request.headerNames.toList()) { name -> request.getHeader(name) }}
            |REQUEST_BODY >> ${String(request.contentAsByteArray)}
            |RESPONSE_HEADERS >>  ${makeHeaders(response.headerNames.toList()) { name -> response.getHeader(name) ?: "" }}
            |RESPONSE_BODY >> ${String(response.contentAsByteArray)}
            |
        """.trimMargin()

        httpLogger.info(requestData)
    }
}
