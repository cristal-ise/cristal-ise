## Instructions to run Cristal-CreateItem jmeter script

1. `cd testing/src/main/jmeter`
1. Retrieve the UUID of the PatientFactory using the browser :
    1. Login: `http://localhost:8081/login?user=user&pass=test`
    1. Open PatientFactory: `http://localhost:8081/domain/integTest/PatientFactory`
1. Choose a runid, e.g. `0001` is used in the next line
1. Execute the following command:
    * `jmeter -n -e -t Cristal-CreateItems.jmx -Jrunid=0001 -o Cristal-CreateItems-Report0001 -JfactoryUuid=3188ed62-2489-4065-8f5a-637124e51706 -JitemCount=5000 -l Cristal-CreateItems0001.jtl`
    * Patient items are created in the `/integTest/Patients/jmeter0001` domain folder
    * Check documentation: https://jmeter.apache.org/usermanual/get-started.html#non_gui
1. To read the report use a browser to open `Cristal-CreateItems-Report0001/index.html`