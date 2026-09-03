Put mysql-connector-j-8.x.x.jar in this folder.

1. Download from https://dev.mysql.com/downloads/connector/j/
   Choose "Platform Independent", download the ZIP, extract it.
2. Copy mysql-connector-j-8.x.x.jar into this lib folder.
3. In NetBeans: right-click Libraries -> Add JAR/Folder -> select that jar.

The project COMPILES without it but FAILS AT RUNTIME with
"No suitable driver found", because the driver is loaded by name at run time.
