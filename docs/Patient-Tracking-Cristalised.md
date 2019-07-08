Class Diagram
-------------

![Pateint Tracking Cristalised](https://api.genmymodel.com/projects/cf074a44-3210-4172-8955-94e567e19e43/diagrams/_Tz-t4i9QEeSxYsF3O4XI1g/svg)

### Patient
Patient is an *Item* so it entire life cycle can be managed and changes can be captured. Item has two major concepts to capture data. The data elements of Patient Item can be based on *Property* and *Outcome*. Property is the set of very basic data which can be changed but its changes are not tracked. It shall be used to describe identification and typing. Outcome is a versioned structure of data which can be as complex as any XML document can be and captured by an Activity.

- MedicalHistory - Each Item in cristalise has a *History* to store the complete audit tail of event happened with it
- CheckList - Mapped to the *Workflow* of the Item which is instantiated from the description (see Protocol below) 
- Checkup - Mapped to the *Activity* of the Workflow.

### Resource
Each Resource is mapped to an *Agent* which is a special Item with *Roles*

- Physician - Mapped to Role and Agent Definition, so instances like 'Jean the Physician' become an Agent. The Physician Role specifies who can prescribe a Checkup
- Specialist - Mapped to a SubRole to group all specialist like Cardiologist
- Equipment -  Mapped to a SubRole to group all the different type of Equipments
- Calendar - This is described in the Calendar Tutorial, and it will not be included here
- Appointment - This is described in the Calendar Tutorial, and it will not be included here

### Protocol
Protocol is mapped to a *Workflow Definition*. Each Checkup is described by an *Activity Definition* and each Activity Definition is associated with an Outcome Definition (XML Schema document) to define what data is captured by that Checkup

- SpecialistType - Mapped to an *Agent Definition*, which capture the Name Property like Opthometrist, can be instantiated as user Agent with a Role like Opthometrist
- EquipmentType - Mapped to an *Agent Definition*

Next: [Implementation](Patient-Tracking-Implementation)