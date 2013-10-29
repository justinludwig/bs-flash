package com.pitchstone.plugin.flash

import grails.test.mixin.support.GrailsUnitTestMixin
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.Cookie
import org.apache.commons.logging.impl.SLF4JLogFactory
import org.codehaus.groovy.grails.plugins.codecs.Base64Codec
import spock.lang.Specification

@Mixin(GrailsUnitTestMixin)
class BasicSessionlessFlashCookieSpec extends Specification {
    static final long JAN_1_2000 = 946684800000
    static final long SEP_9_2001 = 1000000000000

    def macAlgorithm = 'HMACSHA256'
    def secret = new SecretKeySpec('secret'.bytes, macAlgorithm)
    def cookie = new BasicSessionlessFlashCookie(
        name: 'bs_flash',
        maxAge: 300, // 5 minutes
        macAlgorithm: macAlgorithm,
        secret: secret,
        log: new SLF4JLogFactory().getInstance(BasicSessionlessFlashCookie.class),
    )

    def setup() {
        mockCodec Base64Codec
    }

    def "empty cookie can be created"() {
        when: def cookie = new BasicSessionlessFlashCookie()
        then:
            cookie.name == ''
            cookie.value == ''
            cookie.path == null
            cookie.domain == null
            cookie.maxAge == -1
            !cookie.secure
            !cookie.httpOnly
            cookie.macAlgorithm == null
            cookie.secret == null
            cookie.log == null
            cookie.flash == [:]
    }

    def "cookie can be created from another cookie"() {
        when:
            def original = new Cookie('name', 'value')
            original.path = 'path'
            original.domain = 'domain'
            original.maxAge = 1
            original.secure = true
            def cookie = new BasicSessionlessFlashCookie(original)
        then:
            cookie.name == 'name'
            cookie.value == 'value'
            cookie.path == 'path'
            cookie.domain == 'domain'
            cookie.maxAge == 1
            cookie.secure
            !cookie.httpOnly
    }


    def "format maxAge of 0 is Jan 1 1970"() {
        expect: cookie.formatMaxAge(0) == 'Thu, 01-Jan-1970 00:00:00 GMT'
    }

    def "format maxAge of 300 is 5 minutes from now"() {
        expect: cookie.formatMaxAge(300, JAN_1_2000) == 'Sat, 01-Jan-2000 00:05:00 GMT'
    }


    def "format RFC 3339 for null"() {
        expect: cookie.formatRfc3339(null) == null
    }

    def "format RFC 3339 for Jan 1 2000 with milliseconds"() {
        expect: cookie.formatRfc3339(new Date(JAN_1_2000)) == '2000-01-01T00:00:00.000Z'
    }

    def "format RFC 3339 for Jan 1 2000 without milliseconds"() {
        expect: cookie.formatRfc3339(new Date(JAN_1_2000), false) == '2000-01-01T00:00:00Z'
    }


    def "parse RFC 3339 for non date"() {
        expect: cookie.parseRfc3339('foo') == null
    }

    def "parse RFC 3339 for Jan 1 2000 with milliseconds"() {
        expect: cookie.parseRfc3339('2000-01-01T00:00:00.000Z') == new Date(JAN_1_2000)
    }

    def "parse RFC 3339 for Jan 1 2000 without milliseconds"() {
        expect: cookie.parseRfc3339('2000-01-01T00:00:00Z') == new Date(JAN_1_2000)
    }


    def "null flash converts to empty value"() {
        when: cookie.flash = null
        then: cookie.value == ''
    }

    def "empty flash converts to empty value"() {
        when: cookie.flash = [:]
        then: cookie.value == ''
    }

    def "one simple flash entry converts to one-entry value"() {
        when: cookie.flash = [foo: 'bar']
        then: cookie.getValue(JAN_1_2000) ==
            'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY'
    }

    def "three simple flash entries convert to three-entry value"() {
        when: cookie.flash = [foo: 'bar', x: 'y', AC: 'DC']
        then: cookie.getValue(JAN_1_2000) ==
            'foo:bar|x:y|AC:DC|2000-01-01T00:00:00.000Z|UAAxTVW6flm0B5gwGZFBsKF6fV49k+ko+v9oWxV5SmM'
    }

    def "flash entries with non-alpha chars convert to encoded value"() {
        when: cookie.flash = ['yes?': 'we\'re #1!*', 'CPT\u00ae': '$4 & 75%']
        then: cookie.getValue(JAN_1_2000) ==
            'yes%3F:we%27re+%231%21*|CPT%C2%AE:%244+%26+75%25|2000-01-01T00:00:00.000Z|X/DvQY3JBxZC+pFDMouHXXXR2X3Cg3Qr1OWHgqxIIUI'
    }

    def "flash can be double encoded"() {
        when:
            cookie.doubleEncode = true
            cookie.flash = [foo: 'bar']
        then: cookie.getValue(JAN_1_2000) ==
            'Zm9vOmJhcnwyMDAwLTAxLTAxVDAwOjAwOjAwLjAwMFp8VFpreGJSdHN0WTlzNTVYZ2hlQjBXSTFLY0N2ckJhUXFqWUVMUmVLaVJ3WQ'
    }

    
    def "null value converts to empty flash"() {
        when: cookie.value = null
        then: cookie.flash == [:]
    }
    
    def "empty value converts to empty flash"() {
        when: cookie.value = ''
        then: cookie.flash == [:]
    }
    
    def "value with not enough parts converts to empty flash"() {
        when: cookie.value = 'foo'
        then: cookie.flash == [:]

        when: cookie.value = 'foo|bar'
        then: cookie.flash == [:]

        when: cookie.value = '|'
        then: cookie.flash == [:]
    }
    
    def "value with invalid mac converts to empty flash"() {
        when: cookie.value = 'foo|bar|baz'
        then: cookie.flash == [:]

        when: cookie.value = '||'
        then: cookie.flash == [:]

        when: cookie.value = 'foo|bar|baz|xyz'
        then: cookie.flash == [:]

        when: cookie.value = '|||'
        then: cookie.flash == [:]
    }
    
    def "value with expired date converts to empty flash"() {
        when: cookie.value =
            'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY'
        then: cookie.getFlash(SEP_9_2001) == [:]
    }
    
    def "one-entry value converts to one simple flash entry"() {
        when: cookie.value =
            'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY'
        then: cookie.getFlash(JAN_1_2000) == [foo: 'bar']
    }
    
    def "three-entry value converts to three simple flash entries"() {
        when: cookie.value =
            'foo:bar|x:y|AC:DC|2000-01-01T00:00:00.000Z|UAAxTVW6flm0B5gwGZFBsKF6fV49k+ko+v9oWxV5SmM'
        then: cookie.getFlash(JAN_1_2000) == [foo: 'bar', x: 'y', AC: 'DC']
    }
    
    def "encoded value converts to flash entries with non-alpha chars"() {
        when: cookie.value =
            'yes%3F:we%27re+%231%21*|CPT%C2%AE:%244+%26+75%25|2000-01-01T00:00:00.000Z|X/DvQY3JBxZC+pFDMouHXXXR2X3Cg3Qr1OWHgqxIIUI'
        then: cookie.getFlash(JAN_1_2000) == ['yes?': 'we\'re #1!*', 'CPT\u00ae': '$4 & 75%']
    }

    def "flash can be double unencoded"() {
        when:
            cookie.doubleEncode = true
            cookie.value = 
                'Zm9vOmJhcnwyMDAwLTAxLTAxVDAwOjAwOjAwLjAwMFp8VFpreGJSdHN0WTlzNTVYZ2hlQjBXSTFLY0N2ckJhUXFqWUVMUmVLaVJ3WQ'
        then: cookie.getFlash(JAN_1_2000) == [foo: 'bar']
    }


    def "cancel cookie"() {
        setup:
            def added = false
            def response = [ addCookie: { added = true } ]
        when: cookie.cancel response
        then:
            added
            cookie.maxAge == 0
            cookie.path == '/'
    }

    
    def "send basic cookie"() {
        setup:
            def headers = [:]
            def response = [ addHeader: { k,v -> headers[k] = v } ]
        when:
            cookie.flash = [foo: 'bar']
            cookie.send response, JAN_1_2000
        then:
            headers.size() == 1
            headers.'Set-Cookie' ==
                'bs_flash=foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY; expires=Sat, 01-Jan-2000 00:05:00 GMT'
    }
    
    def "send custom cookie"() {
        setup:
            def headers = [:]
            def response = [ addHeader: { k,v -> headers[k] = v } ]
        when:
            cookie.name = 'bsf'
            cookie.path = '/app'
            cookie.domain = 'example.com'
            cookie.maxAge = 60
            cookie.secure = true
            cookie.httpOnly = true
            cookie.flash = [foo: 'bar']
            cookie.send response, JAN_1_2000
        then:
            headers.size() == 1
            headers.'Set-Cookie' ==
                'bsf=foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY; expires=Sat, 01-Jan-2000 00:01:00 GMT; domain=example.com; path=/app; secure; httponly'
    }
}
