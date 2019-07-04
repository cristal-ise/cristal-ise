## Guide to deploy to Maven Central (sonatype) using Travis
_This guide uses GPG2 encryption.  Please see https://www.gnupg.org/documentation/index.html._

### Steps to follow
1. Create a JIRA Account at sonatype.org and create a new OOSRH ticket
  
    These steps are explained in this guide: http://central.sonatype.org/pages/ossrh-guide.html.
    In the ticket explain that you want to be able to administrate the existing `org.cristalise` groupdId.

1. Generate Pretty Good Privacy (PGP) keys

    These steps are explained in this guide: http://central.sonatype.org/pages/working-with-pgp-signatures.html.
    Use `gpg --list-secret-keys` to find the keyid or keyname. In the guide such keyid is `C6EED57A` and this 
    is what you need to find in your gpg database, and use for `GPG_KEYNAME` environment variable bellow.
    
    *Note: GPG2 version uses keybox (.kbx) file which contains both public and secret keys.*

1. Distribute your public key

    `gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys <keyid>`

1. Export secret and owner trust keys.  These will be needed for the travis' environment variables (GPG_SECRET_KEYS and 
   GPG_OWNERTRUST).

    `gpg -a --export-secret-keys <keyid> | base64`
    
    `gpg --export-ownertrust | base64`

1. Install Travis Client

    [Ruby](https://www.ruby-lang.org/en/downloads/) installed on your system is required to use the Travis client.
    Guide and packages are available here https://rubygems.org/pages/download.

    `gem install travis` to install the client.

    `travis version` to check if the client is correctly installed.

1. Prepare `pom.xml` and `.maven.xml` files.

    In order for the maven to publish the artifact to Sonatype OSS, the following plugins needs to be in the **`pom.xml`**:

    * maven-javadoc-plugin - To generate javadoc.
    * maven-source-plugin - To attach the source.
    * nexus-staging-maven-plugin - To release to Maven central.
    * maven-gpg-plugin - To sign the artifacts.

    See https://github.com/cristal-ise/in-memory-lookup/blob/develop/pom.xml for complete details.

    Modify the maven settings file (**`.maven.xml`**) that will be distributed together with the project.  This file will 
    also be used by Travis CI.

    ```
    <settings xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/SETTINGS/1.0.0"
              xsi:schemalocation="http://maven.apache.org/SETTINGS/1.0.0
                          http://maven.apache.org/xsd/settings-1.0.0.xsd">
        <servers>
            <server>
                <id>ossrh</id>
                <username>${env.SONATYPE_USERNAME}</username>
                <password>${env.SONATYPE_PASSWORD}</password>
            </server>
        </servers>
        <profiles>
            <profile>
                <id>ossrh</id>
                <activation>
                    <activeByDefault>true</activeByDefault>
                    <property>
                        <name>performRelease</name>
                        <value>true</value>
                    </property>
                </activation>
                <properties>
                    <gpg.executable>${env.GPG_EXECUTABLE}</gpg.executable>
                    <gpg.passphrase>${env.GPG_PASSPHRASE}</gpg.passphrase>
                </properties>
            </profile>
        </profiles>
    </settings>
    ```
      
   The environment variables above will be created and set on the next step.

1. Add the environment variables on Travis CI project.
    
    Go the settings of the Travis CI project e.g. https://travis-ci.org/<repository>/<project>/settings and the following 
    environment variables:

    * **GPG_EXECUTABLE** - the value must be `gpg`.
    * **GPG_SECRET_KEYS** - the value from the `gpg` export secret keys.
    * **GPG_OWNERTRUST** - the value from the `gpg` export owner true.
    * **GPG_PASSPHRASE** - the passphrase used in exporting export secret keys.
    * **SONATYPE_USERNAME** - the username used in creating OSSRH ticket.
    * **SONATYPE_PASSWORD** - the password used in creating OSSRH ticket.

    *Alternatively, you can generate access token from your profile on Nexus Repository Manager https://oss.sonatype.org and 
    use it for SONATYPE_USERNAME and SONATYPE_PASSWORD. To access your profile on Nexus just use the username and password 
    from OSSRH Jira account then go to profile.*

1. Modify travis file (`.travis.yml`) content.

    *Project version: This will set the project version from the `pom.xml` to an environment variable 
     `project.version`*

       before_deploy:
          - mvn help:evaluate -N -Dexpression=project.version|grep -v '\['
          - export project_version=$(mvn help:evaluate -N -Dexpression=project.version|grep -v '\[')

     Additional details here http://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html

    *Deploy part: Execute `travis setup releases` on the project's root directory. It will ask for GitHub 
     username and password.  This will create the deploy part in `.yml` file.  For details about api_key see 
     https://docs.travis-ci.com/user/deployment/releases/*

       deploy:
          provider: releases
          api_key:
             secure: **** (some long encrypted key)
          file:
             - <project>/target/<name>-$project_version.jar
          skip_cleanup: true
          on:
             repo: *** repository
          name: $project_version

    *GPG details*: This will make the GPG details that we added as env variables available in the build. Travis will replace 
     GPG_SECRET_KEYS and GPG_OWNERTRUST with the correct values.* 

       before_install:
          - echo $GPG_SECRET_KEYS | base64 --decode | $GPG_EXECUTABLE --import
          - echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust

    *Build details: Define the build commands that Travis use, so that it can pass the settings file (`.maven.xml`) and 
     define the profile to use.* 

        install:
            mvn --settings .maven.xml install -DskipTests=true -Dgpg.skip -Dmaven.javadoc.skip=true -B -V

        script:
            mvn clean deploy --settings .maven.xml -DskipTests=true -B -U -Prelease
     
    *Others: You may also need to limit the deployment to Maven Centeral depending on the specified branch(es).  Add the 
    following or similar under the `deploy` tree*

         on:
           all_branches: true
           condition: $TRAVIS_BRANCH =~ ^master|release|develop$

    For complete file example see https://github.com/cristal-ise/in-memory-lookup/blob/develop/.travis.yml.

1. Add, commit and push all changes to github. 
    
    Check travis run here: https://travis-ci.org/cristal-ise/
    
    Check the uploaded artifact here: https://oss.sonatype.org/#nexus-search;quick~cristalise