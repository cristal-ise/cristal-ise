Specifications
--------------
The functionalities of the system is described in the form of one long scenario. This has the advantage to define all major actions in the system, and it also provides the data which is required to setup our final testing. 

### Main Scenario

1. Joe the Patient reserves an appointment with the Physician using a self-service interface
2. Jean the Physician examines Joe, checks his blood pressure and updates his medical records
3. Jean prescribes the following medical check-ups and Specialist analysis:
   * ECG
   * Blood Biochemical Analysis
   * Urinalysis
   * Comprehensive eye examination
4. Joe reserves an appointment with the different Specialists
5. Joe visits the laboratory to give blood for the Blood Biochemical analysis. The results are automatically added to his medical records.
6. Joe visits the laboratory to give urine for the Urinalysis. The results are automatically added to his medical records.
7. Joe visits Sam the Cardiologist who carries out an ECG, adding the results and his diagnosis to Joe's records.
8. Joe visits Steve the Optometrist who examines his eye, adding the results and his diagnosis to Joe's records.
9. Joe reserves an appointment with his Physician using a self-service interface.
10. During Joe's appointment Jean accesses and reads his medical records and discusses the results, diagnoses and medical options with him.


Class Diagram
-------------
The diagram below should be considered as an analysis diagram that could for the blueprint to create the full design. It does not show all the details, but rather it represents the major domain objects and their relationships within a Patient Tracking application.

![Patient Record](https://api.genmymodel.com/projects/_Tz-G0C9QEeSxYsF3O4XI1g/diagrams/_Tz-t4i9QEeSxYsF3O4XI1g/svg)


### Patient
Patient is the core element of the system. It has attributes like Name, NI Number, Sex, Weigth, but it is important to analisy which data can change.

- *MedicalHistory* - Maintains the entire medical history of a person
- *CheckList* - The current list of checkups prescbed by the Physician
- *Checkup* - One examination done by a Specialist

### Resource
Resource is a entity which time needs to be managed

- *Physician* - Medical doctor who carries out basic checkups and can prescibe specific one done by a Specialist
- *Specialist* - Mdical Doctor who carries out scpecial medical examinations, tests, analisys
- *Equipment* - A piece of hardware/software used by a Specialist
- *Calendar* - A pleace which holds the ordered list of Appointments for one Resource
- *Appointment* - An agreed timeslot to carry out a Chekup for a Patient

### Protocol
Protocol is full description of the collection of Checkups that the given organisation, e.g. hospital, is aligable to carry out. Such list can change, and it can include complex interdependent steps.

- *SpecialistType* - A defintions to describe who can carry out a given step in the Protocol
- *EquipmentType* - A defintions to describe the piece of hardware/software which is used during carry a step in the Protocol

Next: [Cristalised](Patient-Tracking-Cristalised)