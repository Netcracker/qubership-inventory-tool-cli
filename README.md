# Starter

[![Vert.x](https://img.shields.io/badge/vert.x-4.5.14-purple.svg)](https://vertx.io)

This application was generated using [http://start.vertx.io](http://start.vertx.io)

## Building project using maven

To package your application:

```bash
mvn package
```

To build the application:

```bash
mvn clean install
```

The 'clean' command will delete all previously compiled Java .class files
and resources (like .properties) in your project.
Your build will start entirely from the beginning.
So if you build again after running exec with 'clean',
it will remove your 'super repository', so be careful and use command `mvn install` only

## Running

To Launch as an application

1. Select Run/Debug Configuration
2. Select 'Application'
3. Click on 'Modify Options'
4. Select 'VM Options'

Values need to be added

1. In VM Options: `-cp $Classpath$`
2. Main Class: `io.vertx.core.Launcher`
3. Program Arguments: ~Select from below configs according to specific runs

Config for dev run

```bash
exec -l ~userid
```

Config for default run

```bash
exec -l ~userid -p default
```

Config for query run

```bash
query -l ~userid
```

Config for ci execution run (correct the params according to the data of the component)

```bash
ci-exec
--inputDirectory=~component location in local system
--outputFile=~file location for storing the result
--repository= ~git link of the component
--componentName=~componentName
```

## Help

* [Vert.x Documentation](https://vertx.io/docs/)
* [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
* [Vert.x User Group](https://groups.google.com/forum/?fromgroups#!forum/vertx)
* [Vert.x Gitter](https://gitter.im/docs/)
