## Guide to deploy to Maven Central (sonatype) using Travis
_This guide uses GPG2 encryption.  Please see [https://www.gnupg.org/documentation/index.html](https://www.gnupg.org/documentation/index.html)._

### Steps to follow
1. Create a JIRA Account at sonatype.org and create a new OOSRH ticket

    These steps are explained in this guide: [http://central.sonatype.org/pages/ossrh-guide.html](http://central.sonatype.org/pages/ossrh-guide.html).
    In the ticket explain that you want to be able to administrate the existing `org.cristalise` groupdId.

1. Generate Pretty Good Privacy (PGP) keys

    These steps are explained in this guide: [http://central.sonatype.org/pages/working-with-pgp-signatures.html](http://central.sonatype.org/pages/working-with-pgp-signatures.html). Unfortunatelly this document is a bit outdated, gpg keyid formats has changed.

    Use `gpg --list-secret-keys  --keyid-format LONG` to find the keyid or keyname. This will have the following output:
    ```
    /home/vagrant/.gnupg/pubring.kbx
    --------------------------------
    pub   rsa3072/3615D3466393F7AA 2020-09-07 [SC] [expires: 2022-09-07]
      B15E8AD67E833F28615B33F13615D3466393F7AA
    uid                 [ultimate] Zsolt Kovacs <zs.myth@gmail.com>
    sub   rsa3072/B525228546FD535F 2020-09-07 [E] [expires: 2022-09-07]
    ```
    In this example the keyid is `B525228546FD535F` (in the guide such keyid is `C6EED57A`) and this 
    is what you need to find in your gpg database, and use for `GPG_KEYNAME` environment variable bellow.

    *Note: GPG2 version uses keybox (.kbx) file which contains both public and secret keys.*

1. Distribute your public key

    - `gpg2 --keyserver hkp://keys.openpgp.org --send-keys <keyid>`
    - check key: `gpg --keyserver hkp://keys.openpgp.org --search-key 'zs.myth@gmail.com'`

1. Install Travis Client

    [Ruby](https://www.ruby-lang.org/en/downloads/) installed on your system is required to use the Travis client.
    Guide and packages are available here [https://rubygems.org/pages/download](https://rubygems.org/pages/download).

    * `gem install travis` to install the client.
    * `travis version` and `travis env list` to check if the client is correctly installed.

1. Prepare `pom.xml` and `.maven.xml` files.

    In order for the maven to publish the artifact to Sonatype OSS, the following plugins needs to be in the **`pom.xml`**:

    * maven-javadoc-plugin - To generate javadoc.
    * maven-source-plugin - To attach the source.
    * nexus-staging-maven-plugin - To release to Maven central.
    * maven-gpg-plugin - To sign the artifacts.

    See [https://github.com/cristal-ise/in-memory-lookup/blob/develop/pom.xml](https://github.com/cristal-ise/in-memory-lookup/blob/develop/pom.xml) for complete details.

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

    Use tracis cleint to set 6 environment variables: **GPG_EXECUTABLE**, **GPG_SECRET_KEYS**, **GPG_OWNERTRUST**, **GPG_PASSPHRASE**,
    **SONATYPE_USERNAME**, **SONATYPE_PASSWORD**

    1. change directory to cristalise git repository
    1. `travis login`
    1. `gpg -a --export-secret-keys B525228546FD535F | base64 > secret-keys`
    1. ``export GPG_SECRET_KEYS=`cat secret-keys` ``
    1. `travis env unset GPG_SECRET_KEYS`
    1. `travis env copy GPG_SECRET_KEYS`
    1. `travis env set GPG_EXECUTABLE gpg`
    1. ``travis env set GPG_OWNERTRUST `gpg --export-ownertrust | base64` ``
    1. `travis env set GPG_PASSPHRASE "your passphrase"` - double qoute is neede if passphrese contains space
    1. `travis env set SONATYPE_USERNAME <yourusername>`
    1. `travis env set SONATYPE_PASSWORD <youruserpwd>`

1. Modify travis file (`.travis.yml`) content.

    *Project version: This will set the project version from the `pom.xml` to an environment variable `project.version`*

       before_deploy:
          - mvn help:evaluate -N -Dexpression=project.version|grep -v '\['
          - export project_version=$(mvn help:evaluate -N -Dexpression=project.version|grep -v '\[')

     Additional details here [http://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html](http://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html)

    *Deploy part: Execute `travis setup releases` on the project's root directory. It will ask for GitHub
     username and password.  This will create the deploy part in `.yml` file.  For details about api_key see
     https://docs.travis-ci.com/user/deployment/releases/*

       deploy:
          provider: releases
          api_key:
             secure: **** (some long encrypted key)
          file:
             - <'project'>/target/<'name'>-$project_version.jar
          skip_cleanup: true
          on:
             repo: *** repository
          name: $project_version

    *GPG details*: This will make the GPG details that we added as env variables available in the build. Travis will replace
     GPG_SECRET_KEYS and GPG_OWNERTRUST with the correct values. The `tr " " "\n"` is needed becuase the `travis env copy ...` will replace the newline with spaces.

       before_install:
          - echo $GPG_SECRET_KEYS | tr " " "\n" | base64 --decode | $GPG_EXECUTABLE --import
          - echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust

    *Build details*: Define the build commands that Travis use, so that it can pass the settings file (`.maven.xml`) and define the profile to use.

        install:
            mvn --settings .maven.xml install -DskipTests=true -Dgpg.skip -Dmaven.javadoc.skip=true -B -V

        script:
            mvn clean deploy --settings .maven.xml -DskipTests=true -B -U -Prelease

    *Others: You may also need to limit the deployment to Maven Centeral depending on the specified branch(es).  Add the 
    following or similar under the `deploy` tree*

         on:
           all_branches: true
           condition: $TRAVIS_BRANCH =~ ^master|release|develop$

    For complete file example see [https://github.com/cristal-ise/in-memory-lookup/blob/develop/.travis.yml](https://github.com/cristal-ise/in-memory-lookup/blob/develop/.travis.yml).

1. Add, commit and push all changes to github.

    Check travis run here: [https://travis-ci.org/cristal-ise/](https://travis-ci.org/cristal-ise/)

    Check the uploaded artifact here: [https://oss.sonatype.org/#nexus-search;quick~cristalise](https://oss.sonatype.org/#nexus-search;quick~cristalise)
