# RequestReply: responder

You can not interact with this application.
It is just response to request-reply messages with random values.
Please check out PingPongConfig.java for further details.

## Setup

To run this example you need to set the following environment variables:

SOLACE_MSG_VPN=yourVpn<br>
SOLACE_HOSTS=tcps://your-broker.messaging.solace.cloud:55443<br>
SOLACE_USERNAME=demo<br>
SOLACE_PASSWORD=demo<br>
HOSTNAME=[your computername]<br>

### STS
In STS/Eclipse you append the variables in the launch configuration. Once you have invoked
RequestReplyResponseApplication.java you can configure the launch configuration. To easily add all variables at once
copy the entries above and use the "Paste" button in the button bar at the right of the variable list.

![Launch Config Dialog in STS](../images/sts1.png)
