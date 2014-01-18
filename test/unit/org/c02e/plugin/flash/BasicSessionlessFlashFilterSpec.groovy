package org.c02e.plugin.flash

import grails.test.MockUtils
import grails.test.mixin.support.GrailsUnitTestMixin
import org.codehaus.groovy.grails.plugins.codecs.Base64Codec
import spock.lang.Specification
import static org.c02e.plugin.flash.BasicSessionlessFlashScope.*

@Mixin(GrailsUnitTestMixin)
class BasicSessionlessFlashFilterSpec extends Specification {
    static final long JAN_1_2000 = 946684800000
    static final long SEP_9_2001 = 1000000000000

    def config = new ConfigObject()
    def filter = new BasicSessionlessFlashFilter(
        grailsApplication: new ConfigObject(),
    )

    def setup() {
        mockCodec Base64Codec
        MockUtils.mockLogging BasicSessionlessFlashFilter

        filter.grailsApplication.config.grails.plugin.basicSessionlessFlash = config
    }


    def "initialized with defaults"() {
        when:
            filter.afterPropertiesSet()
        then:
            filter.cookieName == 'bs_flash'
            filter.maxAge == 300
            filter.domain == ''
            filter.secure == false
            filter.httpOnly == false
            filter.skipPaths as String == '/(css|images|js)/.*'
            filter.doubleEncode == true
            filter.macAlgorithm == 'HMACSHA256'
            filter.secret == null
    }

    def "config overrides defaults"() {
        when:
            config.cookieName = 'bsf'
            config.maxAge = 60
            config.domain = 'example.com'
            config.secure = true
            config.httpOnly = true
            config.skipPaths = '/static/.*'
            filter.doubleEncode = false
            config.macAlgorithm = 'HMACSHA1'
            filter.afterPropertiesSet()
        then:
            filter.cookieName == 'bsf'
            filter.maxAge == 60
            filter.domain == 'example.com'
            filter.secure == true
            filter.httpOnly == true
            filter.skipPaths as String == '/static/.*'
            filter.doubleEncode == false
            filter.macAlgorithm == 'HMACSHA1'
            filter.secret == null
    }


    def "secret can be specified as raw bytes"() {
        when:
            config.secret.bytes = [1, 2, 3] as byte[]
            filter.afterPropertiesSet()
        then:
            filter.secret.encoded == [1, 2, 3] as byte[]
    }

    def "secret can be specified as path to file"() {
        setup:
            def file = File.createTempFile('bsf-test', '.txt')
            file.deleteOnExit()
            file.text = 'secret'
        when:
            config.secret.file = file.path
            filter.afterPropertiesSet()
        then:
            filter.secret.encoded == 'secret'.bytes
    }

    def "secret can be specified as base64 string"() {
        when:
            config.secret.base64 = 'c2VjcmV0'
            filter.afterPropertiesSet()
        then:
            filter.secret.encoded == 'secret'.bytes
    }

    def "secret can be specified as plain text string"() {
        when:
            config.secret.utf8 = 'secret'
            filter.afterPropertiesSet()
        then:
            filter.secret.encoded == 'secret'.bytes
    }


    def "when no now and no next, flash not sent"() {
        setup:
            def request = stubRequest()
            def response = stubResponse()
            standardConfig()
        when:
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers == [:]
            response.cookies == []
    }

    def "when now and no next, flash canceled"() {
        setup:
            def request = stubRequest('/', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            standardConfig()
        when:
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers == [:]
            response.cookies.size() == 1
            response.cookies[0].name == 'bs_flash'
            response.cookies[0].value == ''
            response.cookies[0].path == '/'
            response.cookies[0].maxAge == 0
    }

    def "when no now but when next, flash sent"() {
        setup:
            def request = stubRequest()
            def response = stubResponse()
            standardConfig()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers.size() == 1
            response.headers.'Set-Cookie' ==
                'bs_flash=foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY; expires=Sat, 01-Jan-2000 00:05:00 GMT; path=/'
            response.cookies == []
    }

    def "when now and next, flash sent"() {
        setup:
            def request = stubRequest('/', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            standardConfig()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers.size() == 1
            response.headers.'Set-Cookie' ==
                'bs_flash=foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY; expires=Sat, 01-Jan-2000 00:05:00 GMT; path=/'
            response.cookies == []
    }

    def "when now and next, flash sent just once"() {
        setup:
            def request = stubRequest('/', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            standardConfig()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, false, JAN_1_2000
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers.size() == 1
            response.headers.'Set-Cookie' ==
                'bs_flash=foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY; expires=Sat, 01-Jan-2000 00:05:00 GMT; path=/'
            response.cookies == []
    }

    def "when now and next but skipped path, flash not sent"() {
        setup:
            def request = stubRequest('/css/app.css', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            standardConfig()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers == [:]
            response.cookies == []
    }

    def "when now and next but no secret, flash not sent"() {
        setup:
            def request = stubRequest('/', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            filter.afterPropertiesSet()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, false, JAN_1_2000
        then:
            response.headers == [:]
            response.cookies == []
    }

    def "when now and next but explicit cancel, flash canceled"() {
        setup:
            def request = stubRequest('/', [[name: 'bs_flash', value: 'foo']])
            def response = stubResponse()
            standardConfig()
        when:
            request[NEXT] = [foo: 'bar']
            filter.writeFlash request, response, true, JAN_1_2000
        then:
            response.headers == [:]
            response.cookies.size() == 1
            response.cookies[0].name == 'bs_flash'
            response.cookies[0].value == ''
            response.cookies[0].path == '/'
            response.cookies[0].maxAge == 0
    }


    def "when no cookies, flash not read"() {
        setup:
            def request = stubRequest()
            def response = stubResponse()
            standardConfig()
        when:
            filter.readFlash request, JAN_1_2000
        then:
            request[NOW] == null
    }

    def "when cookies other than bs, flash not read"() {
        setup:
            def request = stubRequest('/', [
                [ name: 'foo', value: 'bar' ],
                [ name: 'x', value: 'y' ],
            ])
            def response = stubResponse()
            standardConfig()
        when:
            filter.readFlash request, JAN_1_2000
        then:
            request[NOW] == null
    }

    def "when bs cookie, flash read"() {
        setup:
            def request = stubRequest('/', [
                [ name: 'bs_flash', value: 'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY' ],
            ])
            def response = stubResponse()
            standardConfig()
        when:
            filter.readFlash request, JAN_1_2000
        then:
            request[NOW].foo == 'bar'
    }

    def "when bs cookie but skipped path, flash not read"() {
        setup:
            def request = stubRequest('/css/app.css', [
                [ name: 'bs_flash', value: 'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY' ],
            ])
            def response = stubResponse()
            standardConfig()
        when:
            filter.readFlash request, JAN_1_2000
        then:
            request[NOW] == null
    }

    def "when bs cookie but too old, flash not read"() {
        setup:
            def request = stubRequest('/', [
                [ name: 'bs_flash', value: 'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY' ],
            ])
            def response = stubResponse()
            standardConfig()
        when:
            filter.readFlash request, SEP_9_2001
        then:
            request[NOW] == null
    }

    def "when bs cookie but no secret, flash not read"() {
        setup:
            def request = stubRequest('/', [
                [ name: 'bs_flash', value: 'foo:bar|2000-01-01T00:00:00.000Z|TZkxbRtstY9s55XgheB0WI1KcCvrBaQqjYELReKiRwY' ],
            ])
            def response = stubResponse()
            filter.afterPropertiesSet()
        when:
            filter.readFlash request, JAN_1_2000
        then:
            request[NOW] == null
    }


    protected stubRequest(String requestURI = '/', List cookies = []) {
        def request; request = [
            requestURI: requestURI,
            cookies: cookies,
            getAttribute: { request[it] },
            removeAttribute: { request.remove(it) },
            setAttribute: { n,v -> request[n] = v },
        ]
        return request
    }

    protected stubResponse() {
        def headers = [:]
        def cookies = []
        [
            // HttpServletResponse api
            addCookie: { cookies << it },
            addHeader: { k,v ->
                headers[k] = headers.containsKey(k) ? "${headers[k]}\n${v}" : v
            },
            // impl
            headers: headers,
            cookies: cookies,
        ]
    }

    protected standardConfig() {
        config.doubleEncode = false
        config.secret.utf8 = 'secret'
        filter.afterPropertiesSet()
    }

}
