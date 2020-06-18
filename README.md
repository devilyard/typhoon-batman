# typhoon-batman

typhoon-batman is a java mqtt client for v3.1 and v3.1.1, supports socket, socket with ssl, web scoket and secure web socket protocol.

## how to begin?

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
