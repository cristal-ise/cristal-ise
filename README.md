CRISTAL-iSE [![Build Status](https://img.shields.io/travis/com/cristal-ise/cristal-ise/master?label=master)](https://travis-ci.org/cristal-ise/cristal-ise)[![Build Status](https://img.shields.io/travis/com/cristal-ise/cristal-ise/develop?label=develop)](https://travis-ci.org/cristal-ise/cristal-ise)[![Javadocs](http://javadoc.io/badge/org.cristalise/cristalise.svg)](http://javadoc.io/doc/org.cristalise)
==================

[![Join the chat at https://gitter.im/cristal-ise/kernel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/cristal-ise/kernel?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Description-Driven Framework for No-Code/Low-Code Application Development 
Main repository of the [CRISTAL-iSE Description-Driven Framework](http://cristal-ise.github.io/).

CRISTAL-iSE is a description-driven software platform originally developed to track the construction of the CMS ECAL detector of the LHC at CERN. It consists of a core library, known as the kernel, which manages business objects called Items. Items are entirely configured from data, called descriptions, held in other Items. Every change of a state in an Item is a consequence of an execution of an activity in that Item's lifecycle, meaning that CRISTAL-iSE applications are completely traceable, even in their design. It also supports extensive versioning of Item description data, giving the system a high level of flexibility.


## Release process
1. **Steps on Github**
   1. review issues listed in the github Release
   1. move all open github issues to a new Release (e.g. from 6.0.0 to 6.1.0)
      - make sure all remaining/closed issues have at least the bug/enhancment flags required for the release summary
1.  **Steps on the development machine**
   1. `git checkout develop`
   1. `git pull -r`
   1. `git checkout master`
   1. `git pull -r`
   1. `git merge develop`
   1. resolve all conflicts - there should only one conflict for version number in parent pom
   1. update version tag in the parent pom (e.g. from 6.0-SNAPSHOT to 6.0.0)
   1. `mvn -N versions:update-child-modules`
   1. `mvn clean install` - for sanity check, it will be executed in travis anyways
   1. `git push`
   1. `git checkout develop`
   1. update version tag in the parent pom (e.g. from 6.0-SNAPSHOT to 6.1-SNAPSHOT)
   1. `mvn -N versions:update-child-modules`
   1. `git push`
1. **Step on Github** - TBD
   1. tag repo with the release number (e.g. v6.0.0)
   1. update Release with relase summary - check previous Releases for guidance :)
   1. close Release