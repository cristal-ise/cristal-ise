## Guide to deploy to Maven Central (sonatype) using Travis
_Our guide is based on this existing template, we reused the deploy.sh script, maven settings.xml and the travis.yml file entirely:_

_[https://github.com/making/travis-ci-maven-deploy-skelton](https://github.com/making/travis-ci-maven-deploy-skelton)_

### Steps to follow
1. Create a JIRA Account at sonatype.org and create a new OOSRH ticket
  
    These steps are explained in this guide: [http://central.sonatype.org/pages/ossrh-guide.html](http://central.sonatype.org/pages/ossrh-guide.html).
    In the ticket explain that you want to be able to administrate the existing `org.cristalise` groupdId.

1. Generate Pretty Good Privacy (PGP) keys

    These steps are explained in this guide: [http://central.sonatype.org/pages/working-with-pgp-signatures.html](http://central.sonatype.org/pages/working-with-pgp-signatures.html).
    Use `gpg --list-secret-keys` to find the keyid or keyname. In the guide such keyid is `C6EED57A` and this 
    is what you need to find in your gpg database, and use for `GPG_KEYNAME` environment variable bellow.

1. Distribute your public key

    `gpg --keyserver hkp://pool.sks-keyservers.net --recv-keys <keyid>`

1. Copy the `deploy` directory from one of our existing project

1. Encrypt the gpg files to secure them in github

    This will overwrite the existing `secring.gpg.enc` and `pubring.gpg.enc` file you have copied in the previous step.
    ```
    $ cd <location of the project>
    $ export ENCRYPTION_PASSWORD=<password to encrypt>
    $ openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ~/.gnupg/secring.gpg -out deploy/secring.gpg.enc
    $ openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ~/.gnupg/pubring.gpg -out deploy/pubring.gpg.enc
    ```

1. Edit the `pom.xml` file of the project

   Check this guide: [http://central.sonatype.org/pages/requirements.html](http://central.sonatype.org/pages/requirements.html). The best is to copy it from one of our 
   existing project. Check also the **maven pom example** provided in the original template we 
   [used](https://github.com/making/travis-ci-maven-deploy-skelton). 

1. Copy the `.travis.yml` file from one of our existing project

1. Install travis cli on your computer

    Use this command: `gem install travis`. On windows use cygwin. In order to use gem command 
    install ruby development tools first together with make.

1. Create the secure environment variables in Travis

    It can be done using the settings pages of travis, e.g. [https://travis-ci.org/cristal-ise/jooqdb/settings](https://travis-ci.org/cristal-ise/jooqdb/settings), or
    use the following commands bellow:
    ```
    $ travis login
    $ travis enable -r cristal-ise/<repository>

    $ travis env set SONATYPE_USERNAME <uid> 
    $ travis env set SONATYPE_PASSWORD <pwd1>
    $ travis env set ENCRYPTION_PASSWORD $ENCRYPTION_PASSWORD
    $ travis env set GPG_KEYNAME <the key looks like:B56B4CE4>
    $ travis env set GPG_PASSPHRASE <passphase>
    ```

1. Make sure that `publish.sh` has executable rights

    Use this command if you already committed publish.sh : `git update-index --chmod=+x publish.sh`

1. Add, commit and push all changes to github. 

   Check travis run here: [https://travis-ci.org/cristal-ise/](https://travis-ci.org/cristal-ise/)