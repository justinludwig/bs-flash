
import org.c02e.plugin.flash.BasicSessionlessFlashFilter
import org.springframework.web.filter.DelegatingFilterProxy

class BasicSessionlessFlashGrailsPlugin {
    // the plugin version
    def version = "0.1-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        'grails-app/views/error.gsp',
        'grails-app/*/test/*.*',
        'web-app/*/test/*.*',
    ]

    def title = "Basic Sessionless Flash Plugin" // Headline display name of the plugin
    def author = "Justin Ludwig"
    def authorEmail = "justin@codetechnology.org"
    def description = '''
Grails FlashScope implementation that stores flash in a cookie
instead of the servlet session.
'''

    // URL to the plugin's documentation
    def documentation = ''//"http://grails.org/plugin/basic-sessionless-flash"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [
        name: "CODE Technology",
        url: "http://codesurvey.org/",
    ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        log.info "installing BasicSessionlessFlashFilter"

        xml.filter[0] + {
            filter {
                'filter-name' 'basicSessionlessFlashFilter'
				'filter-class' DelegatingFilterProxy.name
            }
        }
        xml.'filter-mapping'[0] + {
            'filter-mapping' {
                'filter-name' 'basicSessionlessFlashFilter'
                'url-pattern' '/*'
            }
        }
    }

    def doWithSpring = {
		basicSessionlessFlashFilter(BasicSessionlessFlashFilter) {
			grailsApplication = ref('grailsApplication')
		}
    }

}
