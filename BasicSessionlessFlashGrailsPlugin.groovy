
import com.pitchstone.plugin.flash.BasicSessionlessFlashFilter
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
    def authorEmail = "justin@pitchstonetechnology.com"
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
        name: "PitchStone Technology",
        url: "http://www.pitchstonetechnology.com/",
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

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
