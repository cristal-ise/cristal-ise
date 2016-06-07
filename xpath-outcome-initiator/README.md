Empty and XPath OutcomeInitiator implementation of CRISTAL-iSE [![Build Status](https://travis-ci.org/cristal-ise/xpath-outcome-initiator.svg?branch=master)](https://travis-ci.org/cristal-ise/xpath-outcome-initiator)
=============================================================

This module contains 2 implementation of OutcomeInitiator of CRISTAL-iSE kernel

- EmptyOutcomeInitiator generates empty Outcome(XML) from XML Schema. It is based on SampleXmlUtil of Apache XMLBeans
- XPapthOutcomeInitiator extends EmptyOutcomeInitiator by updating the generated XML based on XPath expression found in the Job