#!/bin/bash

export GPG_SECRET_KEYS=`gpg -a --export-secret-keys <yourkeyid> | base64`
export GPG_EXECUTABLE=gpg
export GPG_OWNERTRUST=`gpg --export-ownertrust | base64`
# double qoute is needed if passphrase contains space
export GPG_PASSPHRASE="<yourpassphrase>"
export SONATYPE_USERNAME=<youruser>
export SONATYPE_PASSWORD=<yourpwd>

echo $GPG_SECRET_KEYS | tr " " "\n" | base64 --decode | $GPG_EXECUTABLE --import --no-tty --batch --yes
echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust --no-tty --batch --yes
mvn -e clean deploy --settings .maven.xml -B -V -Prelease;
