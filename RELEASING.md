mvn -DautoVersionSubmodules=true -Darguments=-Dgpg.keyname="opennms@opennms.org" -Dgpg.keyname="opennms@opennms.org" -Prelease release:clean release:prepare
mvn -DautoVersionSubmodules=true -Darguments=-Dgpg.keyname="opennms@opennms.org" -Dgpg.keyname="opennms@opennms.org" -Prelease release:perform
mvn -DautoVersionSubmodules=true -Darguments=-Dgpg.keyname="opennms@opennms.org" -Dgpg.keyname="opennms@opennms.org" -Prelease nexus-staging:release -DstagingRepositoryId=xxxx
