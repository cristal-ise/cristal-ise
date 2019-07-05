Create Patient - The scenario 
-----------------------------

We are building a healthcare application, and one of the first element to create is a Patient with some vital information like name, insurance number, gender and date of birth. 

## Purpose 

This tutorial helps you to start using CRISTAL-iSE as quickly as possible. It will guide you through the steps of creating your first Items. 

## Prerequisites

To get started first you need to follow the [Getting Started](/cristal-ise/kernel/wiki/Getting-Started) document to install a CRISTAL-iSE server with these 5 modules: kernel, ldap, xmldb, gui and dev

## Description-Driven building blocks

In a development environment like Rails, which is based on the Model-View-Controller paradigm, you create a class to store your model data, which will create the corresponding database table, view and controller. In CRISTAL-iSE this is done rather differently, because it is based on the [Description-driven approach](Description-driven+Systems). 

In CRISTAL-iSE everything is orchestrated by a [[Workflow]] which defines the business process (also called as LifeCycle), which is the list of operations of your business object, in this case the Patient. A single operations or command on that object (Patient), is represented by an [[Activity]]. That Activity provides the so called [[Outcome]], an XML document, which is the (sub)set of business data to be collected for the business object (Patient). All these are grouped together by the [[Item]] to manage Activity execution and data collection consistently. 

There are 2 groups of Items you need to manage for a working system: Description and Instance Items. First of all you need to create the Desciption Items to manage the definition of your business processes and objects. For this tutorial it will be called as PatientDescription Item. The PatientDescription Item will hold the references to Items of WorkflowDescription, ActivityDescription, Schema (also known as OutcomeDescription) and PropertyDescription and using those descriptions it will be able to create new Patient (an Instance Item). You will use and manage these Patient Items to execute operations and monitor their status.

If you want to read more about these concepts, please read the [[Technical Introduction]] and the [[The CRISTAL-iSE API|API]] documents.

## Steps to follow for this tutorial

These are the steps to create all the Items required for this tutorial

1. [Create `PatientDetails` OutcomeDesc Item](../Create-PatientDetails)
1. [Create `SetPatientDetails` Activity Description Item](../Create-SetPatientDetails)
1. [Create `PatientLifecycle` CompositeActivity Description Item](../Create-PatientLifecycle)
1. [Create `PatientDescription` Description Item](../Create-PatientDescription)
1. [Create `Patient` Item](../Create-Patient)
