mvn -DautoVersionSubmodules=true -Darguments=-Dgpg.keyname="701E145FE26283F8C073BAAE697677243260D071" -Dgpg.keyname="701E145FE26283F8C073BAAE697677243260D071" -Prelease release:clean release:prepare
mvn -DautoVersionSubmodules=true -Darguments=-Dgpg.keyname="701E145FE26283F8C073BAAE697677243260D071" -Dgpg.keyname="701E145FE26283F8C073BAAE697677243260D071" -Prelease release:perform

# release:perform uploads the bundle to Central Portal (https://central.sonatype.com) as a pending deployment.
# Log in to Central Portal and click "Publish" on the pending deployment to release it to Maven Central.
