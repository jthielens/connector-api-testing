# README #

This is a testing framework for Cleo Harmony (VersaLex) Connector Shell implementations.

## TL;DR ##

Import the testing JAR into your POM:

```Maven POM
<connector.api.testing.version>5.5.0.0-SNAPSHOT</connector.api.testing.version>
```
```Maven POM
<dependency>
    <groupId>com.cleo.labs</groupId>
    <artifactId>connector-api-testing</artifactId>
    <version>${connector.api.testing.version}</version>
    <scope>test</scope>
</dependency>
```

Use the `TestConnectorClientBuilder` to obtain an instance of your `@Client` class, wired
to the test harness.

```Java
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

```Java
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

coming soon...

## Commands

coming soon...