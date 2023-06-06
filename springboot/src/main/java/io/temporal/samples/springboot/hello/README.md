# SpringBoot Hello Sample

1. Start SpringBoot from main samples repo directory:
   
       ./gradlew bootRun

2. In your browser navigate to:
 
       http://localhost:3030/hello/Temporal%20User

You should see "Hello Temporal User!" show on the page which is the result of our 
Hello workflow execution.

You can try changing the language setting in [application.yaml](../../../../../../resources/application.yaml) file
from "english" to "spanish" to get the greeting result in Spanish.