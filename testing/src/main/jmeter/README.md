## Instructions to run Cristal-CreateItem jmeter script

  - Setup Patient Descriptions
  - Retrieve the UUID of the PatientFactory using a url like this: http://localhost:8081/domain/integTest/PatientFactory
  - Choose a runid, e.g. `0001` is used in the next line
  - Execute the following command:
    - `jmeter -n -e-t Cristal-CreateItems.jmx -Jrunid=0001 -o Cristal-CreateItems-Report0001 -JfactoryUuid=3188ed62-2489-4065-8f5a-637124e51706 -JitemCount=5000 -l Cristal-CreateItems0001.jtl`
