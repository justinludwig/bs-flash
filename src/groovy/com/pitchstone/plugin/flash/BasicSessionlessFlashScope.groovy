package com.pitchstone.plugin.flash

import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.springframework.web.context.request.RequestAttributes as RA
import org.springframework.web.context.request.RequestContextHolder as RCH

/**
 * FlashScope implementation that stores flash as request attrs.
 */
class BasicSessionlessFlashScope implements FlashScope {
    static final String NOW = 'BasicSessionlessFlashScope.NOW'
    static final String NEXT = 'BasicSessionlessFlashScope.NEXT'

    // FlashScope

    void next() {
        // not needed with cookie storage
    }

    /**
     * Gets the special now map (not persisted to next request).
     */
    Map getNow() {
        def now = getAttr(NOW)
        if (now == null) {
            now = [:]
            setAttr NOW, now
        }
        return now
    }

    // Map

    void clear() {
        getAttr(NEXT)?.clear()
        getAttr(NOW)?.clear()
    }

    boolean containsKey(Object key) {
        getAttr(NEXT)?.containsKey(key) || getAttr(NOW)?.containsKey(key)
    }

    boolean containsValue(Object value) {
        entrySet().any { it.value == value }
    }

    Set entrySet() {
        def next = getAttr(NEXT)
        def now = getAttr(NOW)

        if (next && now)
            return (now + next).entrySet()
        if (next)
            return next.entrySet()
        if (now)
            return now.entrySet()

        Collections.EMPTY_SET
    }

    /**
     * Returns the flash value for the specified key or null.
     */
    Object get(Object key) {
        def next = getAttr(NEXT)
        if (next?.containsKey(key))
            return next[key]

        def now = getAttr(NOW)
        now?.get(key)
    }
    
    boolean isEmpty() {
        !getAttr(NEXT) && !getAttr(NOW)
    }

    Set keySet() {
        def next = getAttr(NEXT)
        def now = getAttr(NOW)

        if (next && now)
            return next.keySet() + now.keySet()
        if (next)
            return next.keySet()
        if (now)
            return now.keySet()

        Collections.EMPTY_SET
    }

    /**
     * Sets the specified flash value.
     */
    Object put(Object key, Object value) {
        def next = getAttr(NEXT)
        if (next == null) {
            next = [:]
            setAttr NEXT, next
        }
        next[key] = value
    }

    void putAll(Map m) {
        m?.each { k,v -> put k, v }
    }

    Object remove(Object key) {
        def next = getAttr(NEXT)
        def now = getAttr(NOW)

        def result = now?.remove(key)
        if (next?.containsKey(key))
            result = next.remove(key)

        return result
    }

    int size() {
        keySet().size()
    }

    Collection values() {
        entrySet().collect { it.value }
    }

    // impl

    /**
     * Returns the specified request attribute or null.
     */
    protected Object getAttr(key) {
        RCH.requestAttributes.getAttribute key, RA.SCOPE_REQUEST
    }

    /**
     * Sets the specified request attribute.
     */
    protected void setAttr(key, value) {
        RCH.requestAttributes.setAttribute key, value, RA.SCOPE_REQUEST
    }
}
