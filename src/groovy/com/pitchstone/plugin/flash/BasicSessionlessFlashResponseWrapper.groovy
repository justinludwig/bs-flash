package com.pitchstone.plugin.flash

import java.io.IOException
import java.io.PrintWriter
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

/**
 * HttpServletResponseWrapper implementation that writes the next flash
 * to the cookie right before content/redirects/errors are written to the response.
 */
class BasicSessionlessFlashResponseWrapper extends HttpServletResponseWrapper {

    Closure writeFlash

    BasicSessionlessFlashResponseWrapper(HttpServletResponse rs, Closure writeFlash) {
        super(rs)
        this.writeFlash = writeFlash
    }
    
    ServletOutputStream getOutputStream() throws IOException {
        writeFlash true
        super.outputStream
    }

    PrintWriter getWriter() throws IOException {
        writeFlash true
        super.writer
    }

    void sendError(int code) throws IOException {
        writeFlash()
        super.sendError code
    }

    void sendError(int code, String msg) throws IOException {
        writeFlash()
        super.sendError code, msg
    }

    void sendRedirect(String location) throws IOException {
        writeFlash()
        super.sendRedirect location
    }

    void setStatus(int code) {
        writeFlash()
        super.setStatus code
    }

    void setStatus(int code, String msg) {
        writeFlash()
        super.setStatus code, msg
    }

}
