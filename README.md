# README #

This is a testing framework for Cleo Harmony (VersaLex) Connector Shell implementations.

## TL;DR ##

Import the testing JAR into your POM:

```xml
<connector.api.testing.version>5.5.0.0-SNAPSHOT</connector.api.testing.version>
```
```xml
<dependency>
    <groupId>com.cleo.labs</groupId>
    <artifactId>connector-api-testing</artifactId>
    <version>${connector.api.testing.version}</version>
    <scope>test</scope>
</dependency>
```

Use the `TestConnectorClientBuilder` to obtain an instance of your `@Client` class, wired
to the test harness.

```java
private ConnectorClient setup() throws Exception {
    return new TestConnectorClientBuilder(MyConnectorSchema.class)
        .logger(System.err)
        .debug(true)
        .set("Property1", "Value1")
        .set("Property2", "Value2", "Property3", "Value3")
        .build();
}
```

Use `Commands`, `StringSource`, and `StringCollector` to build test cases:

```java
@Test
public void testRoundTrip() throws Exception {
    ConnectorClient client = setup();
    ConnectorCommandResult result;
    StringSource source = new StringSource("sample", StringSource.lorem);
    StringCollector destination = new StringCollector().name("sample");

    // do a dir
    result = Commands.dir("").go(client);
    assertEquals(Status.Success, result.getStatus());
    assertTrue(result.getDirEntries().isPresent());
    assertTrue(result.getDirEntries().get().isEmpty());

    // put a file
    result = Commands.put(source, "sample").go(client);
    assertEquals(Status.Success, result.getStatus());

    // another dir
    result = Commands.dir("").go(client);
    assertEquals(Status.Success, result.getStatus());
    assertTrue(result.getDirEntries().isPresent());
    assertEquals(1, result.getDirEntries().get().size());
    for (Entry e : result.getDirEntries().get()) {
        assertEquals(e.isDir(), Commands.attr(e.getPath()).go(client).readAttributes().isDirectory());
    }
    String fileID = result.getDirEntries().get().get(0).getPath();

    // now get the file
    result = Commands.get(fileID, destination).go(client);
    assertEquals(Status.Success, result.getStatus());
    assertEquals(StringSource.lorem, destination.toString());

    // should still be a single file in the directory
    result = Commands.dir("").go(client);
    assertEquals(Status.Success, result.getStatus());
    assertTrue(result.getDirEntries().isPresent());
    assertEquals(1, result.getDirEntries().get().size());

    // now delete it
    result = Commands.delete(fileID).go(client);
    assertEquals(Status.Success, result.getStatus());

    // now directory should be empty again
    result = Commands.dir("").go(client);
    assertEquals(Status.Success, result.getStatus());
    assertTrue(result.getDirEntries().isPresent());
    assertTrue(result.getDirEntries().get().isEmpty());
}
```

## TestConnectorClientBuilder

The connector test harness provides the `TestConnectorClientBuilder` class to allow you to obtain instances of your connector client class for testing.
The test harness processes the annotations on your connector configuration schema class, which is typically declared and annotated as follows:

```java
@Connector(scheme = "myscheme", description = "My Connector")
@Client(MyConnectorClient.class)
public class MyConnectorSchema extends ConnectorConfig {
    @Property
    ...
    @Property
    ...
```

The configuration schema names your connector client class and describes the properties necessary to configure it.  Your connector client class will typically be defined as follows:

```java
public class MyConnectorClient extends ConnectorClient {
    // primary constructor
    public MyConnectorClient(MyConnectorSchema schema) {
        ...
    }
    // sometimes for testing additional constructors may be provided to pass mocks
    public MyConnectorClient(MyConnectorSchema schema, MyTestingThing thing) {
        ...
    }
```

To use the `TestClientConnectorBuilder`:

1. Create a builder instance passing your configuration schema class.
2. Configure your connector instance using key/value `String` pairs.
3. Optionally intercept logging output to a `PrintStream` (by default the test harness discards logs).
4. Build your client instance from the builder instance.

### Builder Constructor

```java
TestConnectorClientBuilder builder = new TestConnectorClientBuilder(MyConnectorSchema.class);
```

### Configuring the Connector

```java
builder.set("propertyName", "propertyValue");
```

Use the property name passed as the first argument to `PropertyBuilder` as the name, and the desired value converted to a `String`.  If the property is an `@Array`, the property value must be a JSON array of objects corresponding to the `@Property`s in your `@Array` class.

The builder supports a fluent coding style, so you can set multiple properties in sequence:

```java
builder.set("propertyName1", "propertyValue1").set("propertyName2", "propertyValue2");
```

or you can use a varargs approach and supply a number of name/value pairs in a single call:

```java
builder.set("propertyName1", "propertyValue1", "propertyName2", "propertyValue2");
```

Finally, if you wish to encapsulate your settings in a lambda, you can supply a `UnaryOperator<TestConnector>`:

```java
builder.set(c -> c.set("propertyName1", "propertyValue1").set("propertyName2", "propertyValue2"));
```

### Debug Output

You can capture the debug (and other logger) output to a `PrintStream`:

```java
builder.logger(System.err);
```

As a convenience you can also turn debugging on and off:

```java
builder.debug(true);
```

which is just shorthand for `set(CommonProperty.EnableDebug.name(), String.valueOf(true))` (make sure to add the debug flag to your configuration schema of course):

```java
@Property
final IConnectorProperty<Boolean> enableDebug = CommonProperties.of(CommonProperty.EnableDebug);

```

### Building the Connector Client

Finally, call `build` to obtain an instance of your connector client class, configured as requested, and wired to the test harness.

```java
ConnectorClient client = builder.build();
```

The builder constructs a properly configured instance of your configuraion schema class, including other components of the test harness that are required, and passes this to your connector client constructor.

If you have additional constructors that include additional objects needed for mocking or testing, you can add those objects to the `build` method, and it will locate an appropriate constructor to invoke.  Note that the configuration schema object must still be the first argument to any such constructors.

```java
ConnectorClient client = builder.build(testingThing);
```

### Fluent Style

The `TestConnectorClientBuilder` fully supports fluent style, so you can combine these steps into a single expression:

```java
private ConnectorClient setup() throws Exception {
    return new TestConnectorClientBuilder(MyConnectorSchema.class)
        .logger(System.err)
        .debug(true)
        .set("Property1", "Value1")
        .set("Property2", "Value2", "Property3", "Value3")
        .build();
}
```

## Commands

Once you have a testable instance of your client in hand, you can drive its command methods using the `Commands` builders, including emulators for sources (e.g. for a `PUT`) and destinations (for a `GET`).

The general pattern is `Commands.`_command(argument[s])_`.`_optionals_`.go(client)`.

### DIR

```java
ConnectorCommandResult result = Commands.dir("path")
    .option("option")    // 0 or more times for each option
    .pattern("pattern")  // optional
    .go(client);
```

### PUT

```java
IConnectorOutgoing source = new StringSource("source.txt", StringSource.lorem);
ConnectorCommandResult result = Commands.put(source, "output.txt")
    .option("-MUL")                                // 0 or more times for each option by name
    .option(ConnectorCommandOption.MultipleFiles)  // 0 or more times for each option by enum
    .parameter("option", "value")                  // 0 or more times for each parameter
    .go(client);
```

### GET

```java
StringCollector destination = new StringCollector().name("output.txt");
ConnectorCommandResult result = Commands.get("source.txt", destination)
    .option("-DEL")                         // 0 or more times for each option by name
    .option(ConnectorCommandOption.Delete)  // 0 or more times for each option by enum
    .parameter("option", "value")           // 0 or more times for each parameter
    .go(client);
```

### DELETE

```java
ConnectorCommandResult result = Commands.delete("source.txt")
    .option("-REC")                          // 0 or more times for each option by name
    .option(ConnectorCommandOption.Recurse)  // 0 or more times for each option by enum
    .go(client);
```

### MKDIR

```java
ConnectorCommandResult result = Commands.mkdir("folder")
    .go(client);
```

### RMDIR

```java
ConnectorCommandResult result = Commands.rmdir("folder")
    .go(client);
```

### RENAME

```java
ConnectorCommandResult result = Commands.rename("oldname", "newname")
    .go(client);
```

### ATTR

```java
BasicFileAttributeView attrs = Commands.attr("path")
    .go(client)
```
