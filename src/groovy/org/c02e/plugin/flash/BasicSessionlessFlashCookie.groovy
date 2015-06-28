package org.c02e.plugin.flash

import java.security.Key
import java.text.SimpleDateFormat
import javax.crypto.Mac
import javax.servlet.http.Cookie
import org.apache.commons.logging.Log

class BasicSessionlessFlashCookie extends Cookie {

    BasicSessionlessFlashCookie() {
        super('null', null)
    }

    BasicSessionlessFlashCookie(Cookie cookie) {
        this()

        if (!cookie) return
        '''
            comment domain maxAge name path secure value version
            httpOnly flash macAlgorithm secret log
        '''.trim().split(/\s+/).findAll {
            // don't try to access if not available (eg httpOnly)
            // or if null (eg domain)
            cookie.hasProperty(it) && cookie[it]
        }.each {
            this[it] = cookie[it]
        }
    }

    // Cookie

    Object clone() {
        new BasicSessionlessFlashCookie(this)
    }

    /** Add setter to name. */
    String name = ''

    String getValue(long now = 0) {
        if (!super.value && flash)
            value = encodeFlash(flash, now)
        return super.value ?: ''
    }

    // impl

    /** Pre-servlet-3.0 compatability. */
    boolean httpOnly

    Map<String,String> flash = [:]
    Map<String,String> getFlash(long now = 0) {
        if (!flash && super.value)
            flash = decodeValue(super.value, maxAge, now)
        return flash ?: [:]
    }

    String macAlgorithm
    Key secret
    Log log
    boolean doubleEncode

    /**
     * Sets this cookie on the specified response.
     */
    void send(response, long now = 0) {
        // write header manually for pre-servlet-3.0 compatability
        def parts = [ "${name}=${getValue(now)?:''}" ]
        if (maxAge >= 0)
            parts << "expires=${getFormattedMaxAge(now)}"
        if (domain)
            parts << "domain=${domain}"
        if (path)
            parts << "path=${path}"
        if (secure)
            parts << 'secure'
        if (httpOnly)
            parts << 'httponly'
        response.addHeader 'Set-Cookie', parts.join('; ')
    }

    /**
     * Sets this cookie on the specified response in a way that will clear it.
     */
    void cancel(response) {
        maxAge = 0
        if (!path)
            path = '/'
        flash = [:]
        response.addCookie this
    }

    /**
     * Returns a string formatted as this cookie's 'expires' date.
     */
    String getFormattedMaxAge(long now = 0) {
        formatMaxAge maxAge, now
    }

    /**
     * Returns a string formatted as a cookie 'expires' date
     * the specified seconds in the future.
     */
    String formatMaxAge(Integer maxAge, long now = 0) {
        def ms = maxAge ? (now ?: System.currentTimeMillis()) + maxAge * 1000l : 0
        formatDate new Date(ms)
    }

    /**
     * Returns a string formatted as a cookie 'expires' date
     * with the specified date.
     */
    String formatDate(Date date) {
        def pattern = "EEE, dd-MMM-yyyy HH:mm:ss 'GMT'"
        def formatter = new SimpleDateFormat(pattern, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone('GMT')
        formatter.format date
    }

    /**
     * Serializes the specified flash-map into the cookie value.
     * Returns a empty string if the map is null or empty.
     */
    String encodeFlash(Map<String,String> flash, long now = 0) {
        if (!flash) return ''

        def encoder = new URLEncoder()
        def parts = []
        flash.each { k,v ->
            parts << encoder.encode(k as String, 'UTF-8')
            parts << ':'
            parts << encoder.encode(v as String, 'UTF-8')
            parts << '|'
        }
        parts << formatRfc3339(new Date(now ?: System.currentTimeMillis()))
        def mac = calculateMac(parts.join('').getBytes('ASCII'))
        parts << '|' << mac

        def value = parts.join('')
        // optionally encode entire cookie with base64
        // to protect against faulty cookie handling
        doubleEncode ?
            value.getBytes('ASCII').encodeAsBase64().replaceAll(/=+$/, '') : value
    }

    /**
     * Deserializes the specified cookie value into a flash-map.
     * Returns an empty map if the cookie is null, empty, or invalid.
     */
    Map<String,String> decodeValue(String value, maxAge = 0, long now = 0) {
        if (!value) return [:]

        // reverse extra base64 encoding
        if (doubleEncode)
            value = new String(value.decodeBase64(), 'ASCII')

        def parts = value.split(/\|/) as List
        if (parts.size < 3) {
            log.warn "not enough parts for flash cookie: $value"
            return [:]
        }

        def submittedMac = parts.pop()
        def calculatedMac = calculateMac(parts.join('|').getBytes('ASCII'))
        if (submittedMac != calculatedMac) {
            log.warn "invalid mac for flash cookie: $value"
            return [:]
        }

        def ms = parseRfc3339(parts.pop())?.time ?: 0
        if (ms < (now ?: System.currentTimeMillis()) - maxAge * 1000) {
            log.warn "too-old flash cookie: $value"
            return [:]
        }

        def decoder = new URLDecoder()
        parts.findAll { it ==~ /[^:]+:[^:]+/ }.inject([:]) { map, part ->
            def (k, v) = part.split(/:/)
            k = decoder.decode(k, 'UTF-8')
            v = decoder.decode(v, 'UTF-8')
            map[k] = v
            return map
        }
    }

    /**
     * Generates base64-encoded MAC for the specified message.
     */
    String calculateMac(byte[] message) {
        Mac mac = Mac.getInstance(macAlgorithm)
        mac.init(secret)
        mac.doFinal(message).encodeAsBase64().replaceAll(/=+$/, '')
    }

    /**
     * Formats the specified date in RFC 3339 style.
     * Returns null if the specified date is null.
     */
    String formatRfc3339(Date date, boolean ms = true) {
        if (!date) return null

        def pattern = "yyyy-MM-dd'T'HH:mm:ss${ms?'.SSS':''}'Z'"
        def formatter = new SimpleDateFormat(pattern, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone('GMT')
        formatter.format date
    }

    /**
     * Parses the specified date in RFC 3339 style.
     * Returns null if the specified string cannot be parsed.
     */
    Date parseRfc3339(String s) {
        def m = s =~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z/
        if (!m) return null

        def ms = m[0][1]
        def pattern = "yyyy-MM-dd'T'HH:mm:ss${ms?'.SSS':''}'Z'"
        def formatter = new SimpleDateFormat(pattern, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone('GMT')
        formatter.parse s
    }

}
