package com.pitchstone.plugin.flash

import spock.lang.Specification
import static com.pitchstone.plugin.flash.BasicSessionlessFlashScope.*

class BasicSessionlessFlashScopeSpec extends Specification {

    def flash = new BasicSessionlessFlashScope()
    def request = [:]

    def setup() {
        flash.getMetaClass().getAttr = { request[it] }
        flash.getMetaClass().setAttr = { k,v -> request[k] = v }
    }


    def "when now is null, now lazy inits"() {
        expect: flash.getNow() == [:]
    }

    def "when now is not null, now does not re-init"() {
        when: request[NOW] = [foo: 1]
        then: flash.getNow() == [foo: 1]
    }

    def "now is stored as request attr"() {
        when: flash.getNow()
        then: request[NOW] == [:]
    }


    def "when no next or now, gets nothing"() {
        expect: flash.foo == null
    }

    def "when next only, gets next"() {
        when: request[NEXT] = [foo: 1]
        then: flash.foo == 1
    }

    def "when now only, gets now"() {
        when: request[NOW] = [foo: 1]
        then: flash.foo == 1
    }

    def "when both next and now, gets next"() {
        when:
            request[NEXT] = [foo: 1]
            request[NOW] = [foo: 2]
        then: flash.foo == 1
    }


    def "put is stored as next request attr"() {
        when: flash.foo = 1
        then:
            request[NEXT] == [foo: 1]
            request[NOW] == null
    }

    def "put multiple into next"() {
        when:
            flash.foo = 1
            flash.bar = 2
        then: request[NEXT] == [foo: 1, bar: 2]
    }


    def "put all into next"() {
        when: flash.putAll foo: 1, bar: 2
        then: request[NEXT] == [foo: 1, bar: 2]
    }


    def "when no next or now, removes nothing"() {
        expect: flash.remove('foo') == null
    }

    def "when next only, removes next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then:
            flash.remove('foo') == 1
            request[NEXT] == [bar: 2]
    }

    def "when now only, removes now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then:
            flash.remove('foo') == 1
            request[NOW] == [bar: 2]
    }

    def "when both next and now, removes both, returns next"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then:
            flash.remove('foo') == 1
            request[NEXT] == [bar: 2]
            request[NOW] == [baz: 3]
    }


    def "when no next or now, entrySet is nothing"() {
        expect: flash.entrySet() as List == []
    }

    def "when next only, entrySet is next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then:
            flash.entrySet()*.key == ['foo', 'bar']
            flash.entrySet()*.value == [1, 2]
    }

    def "when now only, entrySet is now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then:
            flash.entrySet()*.key == ['foo', 'bar']
            flash.entrySet()*.value == [1, 2]
    }

    def "when both next and now, entrySet is merged, preferring next"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then:
            flash.entrySet()*.key.sort() == ['bar', 'baz', 'foo']
            flash.entrySet()*.value.sort() == [1, 2, 3]
    }


    def "when no next or now, keySet is nothing"() {
        expect: flash.keySet() as List == []
    }

    def "when next only, keySet is next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then: flash.keySet() as List == ['foo', 'bar']
    }

    def "when now only, keySet is now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then: flash.keySet() as List == ['foo', 'bar']
    }

    def "when both next and now, keySet is merged"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then: flash.keySet().sort() == ['bar', 'baz', 'foo']
    }


    def "when no next or now, values is nothing"() {
        expect: flash.values() as List == []
    }

    def "when next only, values is next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then: flash.values() as List == [1, 2]
    }

    def "when now only, values is now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then: flash.values() as List == [1, 2]
    }

    def "when both next and now, values is values of merged entries, preferring next"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then: flash.values().sort() == [1, 2, 3]
    }


    def "when no next or now, size is 0"() {
        expect: flash.size() == 0
    }

    def "when next only, size is of next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then: flash.size() == 2
    }

    def "when now only, size is of now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then: flash.size() == 2
    }

    def "when both next and now, size is of merged keys"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then: flash.size() == 3
    }


    def "when no next or now, is empty"() {
        expect: flash.isEmpty()
    }

    def "when next only, is not empty"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then: !flash.isEmpty()
    }

    def "when now only, is not empty"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then: !flash.isEmpty()
    }

    def "when both next and now, is not empty"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then: !flash.isEmpty()
    }


    def "when no next or now, does not contain key"() {
        expect: !flash.containsKey('foo')
    }

    def "when next only, contains key from next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then:
            flash.containsKey('foo')
            !flash.containsKey('baz')
    }

    def "when now only, contains key from now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then:
            flash.containsKey('foo')
            !flash.containsKey('baz')
    }

    def "when both next and now, contains keys from both next and now"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then:
            flash.containsKey('foo')
            flash.containsKey('bar')
            flash.containsKey('baz')
            !flash.containsKey(1)
    }


    def "when no next or now, does not contain value"() {
        expect: !flash.containsValue(1)
    }

    def "when next only, contains value from next"() {
        when: request[NEXT] = [foo: 1, bar: 2]
        then:
            flash.containsValue(1)
            !flash.containsValue(10)
    }

    def "when now only, contains value from now"() {
        when: request[NOW] = [foo: 1, bar: 2]
        then:
            flash.containsValue(1)
            !flash.containsValue(10)
    }

    def "when both next and now, contains values from merged entries, preferring next"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
        then:
            flash.containsValue(1)
            flash.containsValue(2)
            flash.containsValue(3)
            !flash.containsValue(10)
    }


    def "when no next or now, clears nothing"() {
        when: flash.clear()
        then:
            request[NEXT] == null
            request[NOW] == null
    }

    def "when next only, clears next"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            flash.clear()
        then:
            request[NEXT] == [:]
            request[NOW] == null
    }

    def "when now only, clears now"() {
        when:
            request[NOW] = [foo: 1, bar: 2]
            flash.clear()
        then:
            request[NEXT] == null
            request[NOW] == [:]
    }

    def "when both next and now, clears both"() {
        when:
            request[NEXT] = [foo: 1, bar: 2]
            request[NOW] = [foo: 10, baz: 3]
            flash.clear()
        then:
            request[NEXT] == [:]
            request[NOW] == [:]
    }

}
