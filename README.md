# gpiorestapi
REST Api for Raspberry gpio

## How to start application
java -Dspring.resources.static_locations=file:/path -Dgpio.config=OUTPUT.1.Tuta.LOW,INPUT.2.Door.PULL_DOWN -jar build/libs/gpio-1.0.0.jar

## Get state
GET /gpio/pin
Response: {"state":"LOW"}
  
## Set state
POST /gpio/{pin}/{state}
  
## Post a sequence of commands
POST /gpio/sequence/{pin}{H|L}{millis}
sequence example: 1H1000,1L500

## Subscribe to Server Sent Events
GET /gpio/{pin}/events
  
Example of event data in response: {"pin":1,"state":"LOW"}
