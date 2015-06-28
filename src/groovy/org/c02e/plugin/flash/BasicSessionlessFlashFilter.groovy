package org.c02e.plugin.flash

import java.io.IOException
import java.security.Key
import java.util.regex.Pattern
import javax.crypto.spec.SecretKeySpec
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GAA
import org.springframework.beans.factory.InitializingBean
import static org.c02e.plugin.flash.BasicSessionlessFlashScope.*

/**
 * Filter that serializes flash scope to and from a cookie.
 */
class BasicSessionlessFlashFilter implements Filter, InitializingBean {
    static final String WROTE_FLASH = 'BasicSessionlessFlashFilter.WROTE_FLASH'

    def grailsApplication

    /** Name of cookie in which to store flash. */
    String cookieName
    /** Seconds before cookie expires. */
    Integer maxAge
    /** Custom domain for which to save the flash. */
    String domain
    /** True if HTTPS-only. */
    Boolean secure
    /** True if not accessible to JavaScript. */
    Boolean httpOnly
    /** Paths to skip reading/writing cookies to/from. */
    Pattern skipPaths
    /** True to base64-encode the entire cookie value. */
    Boolean doubleEncode
    /** HMAC algorithm name. */
    String macAlgorithm
    /** Secret key used for cookie HMAC. */
    Key secret

    // Filter

    void init(FilterConfig config) throws ServletException {
    }

    void destroy() {
    }

    void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
    throws IOException, ServletException {
        readFlash rq

        rs = new BasicSessionlessFlashResponseWrapper(
        (HttpServletResponse) rs, { clear = false ->
            writeFlash rq, rs, clear
        })
        chain.doFilter rq, rs
    }

    // InitializingBean

    void afterPropertiesSet() {
        def config = this.config

        cookieName = cookieName ?: config?.cookieName ?: 'bs_flash'
        maxAge = maxAge ?: config?.maxAge ?: 5 * 60 // 5 minutes
        domain = domain ?: config?.domain ?: ''
        secure = secure != null ? secure : !!config?.secure
        httpOnly = httpOnly != null ? httpOnly : !!config?.httpOnly

        skipPaths = skipPaths ?: Pattern.compile(config?.skipPaths ?: '/(css|images|js)/.*')
        // default to true
        doubleEncode = doubleEncode != null ? doubleEncode : config?.doubleEncode != false
        macAlgorithm = macAlgorithm ?: config?.macAlgorithm ?: 'HMACSHA256'

        if (!secret) {
            def bytes = config?.secret?.bytes ?:
                config?.secret?.file ? new File(config.secret.file).bytes :
                config?.secret?.base64 ? config.secret.base64.decodeBase64() :
                config?.secret?.utf8 ? config.secret.utf8.getBytes('UTF-8') :
                null
            secret = bytes ? new SecretKeySpec(bytes, macAlgorithm) : null
        }
    }

    // impl

    def getConfig() {
        grailsApplication?.config?.grails?.plugin?.basicSessionlessFlash
    }

    /**
     * Reads the current flash cookie from the request.
     */
    void readFlash(request, long now = 0) {
        if (request.requestURI ==~ skipPaths) return

        if (!secret) {
            log.error "please set grails.plugin.basicSessionlessFlash.secret in Config.groovy"
            return
        }

        // initialize grails's flash attribute for current request
        request.setAttribute GAA.FLASH_SCOPE, new BasicSessionlessFlashScope()

        def cookie = request.cookies.find { it.name == cookieName }
        if (!cookie) return

        cookie = new BasicSessionlessFlashCookie(cookie)
        cookie.maxAge = maxAge
        cookie.doubleEncode = doubleEncode
        cookie.macAlgorithm = macAlgorithm
        cookie.secret = secret
        cookie.log = log

        // decode cookie value to map
        def flash = cookie.getFlash(now)
        if (flash)
            request.setAttribute NOW, flash
    }

    /**
     * Writes the next flash cookie to the response.
     */
    void writeFlash(request, response, clear = false, long now = 0) {
        if (request.requestURI ==~ skipPaths || !secret) return

        // avoid writing more than once per request
        if (request.getAttribute(WROTE_FLASH)) return
        request.setAttribute(WROTE_FLASH, true)

        def cookie = new BasicSessionlessFlashCookie(
            name: cookieName,
            path: request.contextPath ?: '/',
            maxAge: maxAge,
            secure: secure,
            httpOnly: httpOnly,
            // skip empty values
            flash: request.getAttribute(NEXT)?.findAll { k,v -> k && v },
            doubleEncode: doubleEncode,
            macAlgorithm: macAlgorithm,
            secret: secret,
            log: log,
        )
        // avoid setting domain unless it's actually going to be used --
        // otherwise, response impl may add empty domain to Set-Cookie header
        if (domain)
            cookie.domain = domain

        // send if there's new flash content
        if (!clear && cookie.flash)
            cookie.send response, now
        // clear if no new flash content, but was previously
        else if (request.cookies.any { it.name == cookieName })
            cookie.cancel response
    }
}
