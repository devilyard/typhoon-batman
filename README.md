# typhoon-batman

typhoon-batman is a java mqtt client for v3.1 and v3.1.1, supports socket, socket with ssl, web scoket and secure web socket protocol.

## installation
integrate with maven
```xml
<dependency>
  <groupId>io.github.devilyard</groupId>
  <artifactId>typhoon-mqtt-batman</artifactId>
  <version>3.0</version>
</dependency>
```

with gradle
```
implementation 'io.github.devilyard:typhoon-batman:3.0'
```

with gradle kotlin dsl
```
implementation("io.github.devilyard:typhoon-batman:3.0")
```

and with ivy
```
<dependency org="io.github.devilyard" name="typhoon-batman" rev="3.0" />
```

## how to begin?

to get connect

```java
@Test
public void test() throws MqttException, InterruptedException {
    MqttClient client = new MqttClient("tcp://127.0.0.1:1088");
    MqttConnectOptions options = new MqttConnectOptions();
    options.setUsername("app");
    options.setPassword("passwd".getBytes());
    options.setClientId("test-app");
    options.setAutomaticReconnect(true);
    client.connect(options);

    new CountDownLatch(1).await();
}
```

to subscribe a topic
```java
client.subscribe("/topic/order", MqttQoS.AT_LEAST_ONCE, message -> {
    System.out.println(new String(message.payload()));    
});
```

to publish a message with retain flag on qos level 2
```java
client.publish("/topic/order", "hello".getBytes(), MqttQoS.AT_LEAST_ONCE, true);
```

and at last close the connection
```java
client.close();
```
