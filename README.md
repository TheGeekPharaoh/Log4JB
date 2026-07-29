# Log4JB

## Overview

Log4JB is a plugin for JetBrains IntelliJ IDEA designed to simplify the addition of log statements to your Java code. It is
inspired by the _excellent_ [Log4e plugin](http://log4e.jayefem.de) for Eclipse that seems to have been abandoned. The
goal of this project is to implement all of the functionality that was found in Log4e, as well as the addition of some
new items.

## License

This project is licensed under the [Apache 2.0](APACHE_LICENSE.md) license.

## Status

The tables below outlines the current status of the implementation:

### Supported Log Frameworks

| Status             | Item              | Notes                                                            |
| ------------------ | ----------------- | ---------------------------------------------------------------- |
| :heavy_check_mark: | SLF4j             | Format of statements, logger name, etc. are currently hard-coded |
|                    | Log4j             |                                                                  |
|                    | Log4j2            |                                                                  |
|                    | commons-logging   |                                                                  |
|                    | java.util.logging |                                                                  |

### Base Functionality

| Status              | Item                                 | Notes                                                                                                        |
|---------------------|--------------------------------------|--------------------------------------------------------------------------------------------------------------|
| :heavy_check_mark:  | Define a logger for a class          | Defines a `private static` logger property for the selected class                                            |
| :heavy_check_mark:  | *Log this method*                    | Generates start/end log statements for the selected method                                                   |
| :heavy_check_mark:  | *Log this variable*                  | Generates a log statement for the selected variable.  Currently, this is hard-coded to the `DEBUG` log level |
| :heavy_check_mark:  | *Log at this position...*            | Generates a user-defined log statement, including selected variables                                         |
|                     | *Exchange logging framework*         | Replaces the logging statements generated for one log framework (i.e. log4j2) with another (i.e. slf4j)      |
| :heavy_check_mark:  | Log caught exceptions                | Generates log statements for any caught exceptions                                                           |
| :heavy_check_mark:  | *Replace `System.out.println(...)`*  | Replaces any `System.out.println` calls with generated logging statements                                    |
| :heavy_check_mark:  | *Replace `System.err.println(...)`*  | Replaces any `System.err.println` calls with generated logging statements                                    |
|                     | *Reapply in this method/class*       | Re-generates all generated logging statements.  This is useful when the method parameters have been modified |
|                     | *Remove logger of this method/class* |                                                                                                              |

### Customization

| Status | Item            | Notes |
| ------ |-----------------| ----- |
| | Settings dialog | |
| | Customizable log-levels for *Log this variable*, etc. | |
| | Customizable log statement templates | Using Velocity, JEXL, etc. |

## Contributing

Contributions, enhancements, bugfixes, etc. are always welcome!