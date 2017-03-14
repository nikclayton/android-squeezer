#!groovy

properties(
    [
        pipelineTriggers([cron('H 2 * * *')]),
    ]
)

node
{
    currentBuild.result = "SUCCESS"
	
    try 
	{
       stage "Checkout"
            checkout scm

       stage "Clean"
            sh "./gradlew clean; exit 0"

	   stage "Build"
            sh "./gradlew build"
			
       stage "APKs"
            sh "./gradlew assembleRelease"
            archiveArtifacts artifacts: "Squeezer/build/outputs/apk/*.apk", fingerprint: true
			
       stage "Lint Report"
            step([$class: 'LintPublisher', pattern: 'Squeezer/build/outputs/lint-results*.xml'])
    }
		
    catch (err) 
	{
        currentBuild.result = "FAILURE"
        throw err
    }
}
