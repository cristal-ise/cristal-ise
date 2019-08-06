```
@startuml
[*] -> Waiting
Waiting --> Started: Start
Waiting --> Finished : Done
Started --> Finished : Finish
Started --> Suspended : Suspend
Suspended --> Started : Resume

note as N1
  List of Transition can have Outcome
    - Done
    - Finish
    - Suspend

  Resume may be performed by any Admin role
  but it preservers Agent reservation.
end note
@enduml
```