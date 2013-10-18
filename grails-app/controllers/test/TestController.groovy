package test

class TestController {

    def index() { }

    def flashGet() {
        flash.message = '<em>GET</em> flash'
        forward action: 'index'
    }

    def flashPost() {
        flash.message = '<em>POST</em>-redirect-<em>GET</em> flash'
        redirect action: 'index'
    }

    def noFlashGet() {
        forward action: 'index'
    }

    def noFlashPost() {
        redirect action: 'index'
    }
}
