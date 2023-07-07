# RequestReply: Sender

## Description

You got 4 rest controller.
- http://localhost:9011/temperature/last_value/bn
  Dynamic sending topi via solace PubSub+
- http://localhost:9011/temperature_last_value
  Static sending topi via solace PubSub+


## Setup

To run this example you need to set the following environment variables:

SOLACE_MSG_VPN=yourVpn<br>
SOLACE_HOSTS=tcps://your-broker.messaging.solace.cloud:55443<br>
SOLACE_USERNAME=demo<br>
SOLACE_PASSWORD=demo<br>
HOSTNAME=[your computername]<br>

### STS
In STS/Eclipse you append the variables in the launch configuration. Once you have invoked
RequestReplySendingApplication.java you can configure the launch configuration. To easily add all variables at once
copy the entries above and use the "Paste" button in the button bar at the right of the variable list.

![Launch Config Dialog in STS](../images/sts1.png)
