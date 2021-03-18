# Cassandra Loader

The Cassandra Loader automates launching and stopping of Cassandra nodes. It watches a repository (HTTP directory) for changes of 
the two files ``default.conf`` and ``hackation.jar``. If the files were changed, the Cassandra node is shutdown and relaunched with new fetched
configuration and JAR. If one of the files is not available in the repository, the node is shutdown and waiting for a valid launch configuration.
The loader tries to get a valid configuration every 2 seconds (default) until it is closed.

## Requirements
### Java JRE 11+
Ubuntu
````
sudo apt-get install openjdk-11-jre
````



## One-Liner (pre-built)
Create an empty working directory first like ``mkdir -p ~/cassandra`` or ``mkdir -p /opt/cassandra`` (recommend)

Replace ``<url>`` with the url of the repository containing the launch configuration, and the JAR. 
````
rm -f cassandra-loader-1.0-all.jar \
&& wget https://github.com/fpieper/cassandra-loader/releases/download/1.0/cassandra-loader-1.0-all.jar \
&& java -jar cassandra-loader-1.0-all.jar --repository <url> --console
````

## Build
````
git clone https://github.com/fpieper/cassandra-loader.git
cd cassandra-loader
sh gradlew build
java -jar build/libs/cassandra-loader-1.0-all.jar --repository <url> --console
````

## Usage
````
java -jar cassandra-loader.jar --repository <url> --console
````

## Repository Setup
This is only required for adding/removing a Cassandra node configuration/version to the repository.
Every HTTP directory is a valid repository as long as the HTTP response contains the ETag or entity tag.
The loader considers a repository as valid if ``default.conf`` and ``hackation.jar`` are found in that directory.
