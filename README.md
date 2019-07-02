# gpiorestapi
REST Api for Raspberry gpio

## How to start application
java -Dgpio.config=OUTPUT.1.Tuta.LOW,INPUT.2.Door.PULL_DOWN -jar build/libs/gpio-1.0.0.jar

## Get state
GET /pin
Response: {"state":"LOW"}
  
## Set state
POST /{pin}/{state}
  
## Post a sequence of commands
POST /sequence/{pin}{H|L}{millis}
sequence example: 1H1000,1L500

## Subscribe to Server Sent Events
GET /{pin}/events
  
{pin} is an integer
{state} is HIGH or LOW
