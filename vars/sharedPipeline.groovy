import org.yaml.snakeyaml.Yaml


def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node {
	    // Clean workspace before doing anything
	    deleteDir()

	    try {
	        stage ('Clone & Env') {
	        	checkout scm
			def yamlconfig = readYaml file: "config.yml"
			env.buildProjectFolder = yamlconfig.build.projectFolder
			env.buildCommand = yamlconfig.build.buildCommand
			env.databaseFolder = yamlconfig.database.databaseFolder
			env.databaseCommand = yamlconfig.database.databaseCommand
			env.deployCommand = yamlconfig.deploy.deployCommand
			env.ptest = yamlconfig.test[0].testCommand
			env.rtest = yamlconfig.test[1].testCommand
			env.itest = yamlconfig.test[2].testCommand
			env.testFolder = yamlconfig.test[0].testFolder
			env.emailRecipients = yamlconfig.notifications.email.recipients
			
	        }
	        stage ('Build') {
	        	sh "echo 'building ${config.projectName} ...'"
			sh "${buildCommand} -f ${buildProjectFolder}/pom.xml"
			
	        }
	        stage ('Database') {
	        	sh "echo 'Database action on project: ${config.projectName} ...'"
			sh "${databaseCommand} -f ${databaseFolder}/pom.xml"
			
	        }
	      	stage ('Deploy') {
	            sh "echo 'deploying to server ${config.serverDomain}...'"
		    sh "${deployCommand} -f ${buildProjectFolder}/pom.xml"
	      	}
		stage ('Test') {
		        parallel 'performance': {
		            sh "echo 'shell scripts to run performance tests...'"
				dir(testFolder) {
			    		sh "${ptest}"
				}
		        },
		        'regression': {
		            sh "echo 'shell scripts to run regression tests...'"
		            dir(testFolder) {
			    		sh "${rtest}"
				}
		        },
		        'integration': {
		            sh "echo 'shell scripts to run integration tests...'"
			    dir(testFolder) {
			    		sh "${itest}"
				}
		        }
	        }
	    } catch (err) {
	        currentBuild.result = 'FAILED'
		emailext (
          		subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
          		body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
            		<p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
			to: emailRecipients
        		)
	        throw err
	    }
    }
}

