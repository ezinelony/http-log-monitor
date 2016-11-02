# HTTP log monitoring console program

###### Create a simple console program that monitors HTTP traffic on your machine:

Consume an actively written-to w3c-formatted HTTP access log (https://en.wikipedia.org/wiki/Common_Log_Format)
* Every 10s, display in the console the sections of the web site with the most hits (a section is defined as being what's before the second '/' in a URL. i.e. the section for "http://my.site.com/pages/create' is "http://my.site.com/pages"), as well as interesting summary statistics on the traffic as a whole.
* Make sure a user can keep the console app running and monitor traffic on their machine
* Whenever total traffic for the past 2 minutes exceeds a certain number on average, add a message saying that “High traffic generated an alert - hits = {value}, triggered at {time}”
* Whenever the total traffic drops again below that value on average for the past 2 minutes, add another message detailing when the alert recovered
* Make sure all messages showing when alerting thresholds are crossed remain visible on the page for historical reasons.
* Write a test for the alerting logic
* Explain how you’d improve on this application design


 ## Design Approach
    The application utilizes Reactive Design Ideas [http://www.reactivemanifesto.org/, https://www.lightbend.com/blog/architect-reactive-design-patterns]
    
        There are about 4 major components which are actors that communicate via messages
        
        1. The Monitor: monitors the log file changes
            When a change happens, it sends a message to every listener of that change along with the log entry
            An actor become a listener by becoming a child of a listener node which is detached from the monitor
        
        2. The Section Actor: Receives a message about log entry changes
           In order to efficiently get sections with the most hits, it utilizes a MaxHeap like datastructure called HashMapPriorityQueue (HashMap+PriorityQueue with update capability)
           It also sends out stats messages to Message Dispatcher actor; responsible for dispatching this message (In this case, printing it to the console)
        
        3. Message Dispatcher Actor: prints out messages to the console and manages how many messages a client sees again when an alert is sent
        
        4. The Section Actor: Is also a listener to log entry changes
           It utilizes a data structure that enables it to only store the necessary hits to be able to calculate last 2 hours/minutes/seconds 
           It also send out alert message to the Message Dispatcher Actor
           
 ## Potential Improvements 
    There are a lot of information that we can get from the log entries like What section has the most 400s, 404s, 500s( 499 < x < 600),
   
    What Ips have the most errors, 500s, 400s, ...
   
    Our calculation of hits is a little bit primitive; every request is a hit (assets(images, js, css,...) request, non assets requests)
   
    we could improve this either excluding the assets or at least separating them from non assets request
   
    The Message Dispatcher can be implemented to ship the stats and alerts somewhere rather than just printing
   
    We could use the log entry gather information about requests transitions i.e, does requestA always follow requestB? or this is the first time requestC is coming immediately after requestD
        These could useful in determining abnormal requests
    For the moment, this program only works on Linux. It could be good to propose a Windows version.
   
   ## ENV Variables
   
     `ENV_ACCESS_LOG_PATH` : path to the access log (Without this the application will exit)
     `ENV_ALERT_DELAY_IN_SECONDS` : [Default is 60 seconds] Delay to observe between persistent high frequency alerts
     `ENV_SECTION_TICK_DURATION_IN_SECONDS` : [Default is 10 seconds] Section with most hits interval
     `ENV_ALERT_WINDOW_IN_MINUTES`: [Default is 2 minutes] How far back should alert calculate
     `ENV_SCREEN_WINDOW`: [Default is 25] The number of past messages that gets replayed immediately following an alert
     
   ## Testing/Running Requires sbt
     
     sbt test
     "sbt" "project monitor" "run"
     
   ## Building/Running via java -jar Requires sbt
   
      build : `sbt assembly` 
      run (you can pass in your ENV too): java -jar {appRoot}/core/target/scala-2.11/http-log-monitor-core-assembly-1.0.jar 
      
     
     
     
     
    