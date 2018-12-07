# Java port of the irods icommands using jargon (https://github.com/DICE-UNC/jargon).

## Running
Requires java > 1.8, it can be run directly from the jar file using:
```bash
java -cp jvmicmd.jar org.computational_plant_science.jvmicmd.Main
```

## Install Using deb package
For a full install allowing the program to be run by calling ```jvmicmd```.

Java 1.8 or greater must be installed separately for jvmicmd to work.


```bash
wget https://github.com/cottersci/irods_java_client/releases/download/1.1ALPHA/jvmicmd_1.1ALPHA_all.deb
sudo dpkg -i jvmicmd_1.1ALPHA_all.deb
```

## Usage
```bash
Usage: jvmicmd [-hvV] [-p=<port>] [-r=<resource>] [-s=<password>] [-z=<zone>]
               <host> <username> [COMMAND]
Java wrapper for iRODS iCommands.
      <host>       hostname
      <username>   username
  -h, --help       Show this help message and exit.
  -p=<port>        port (default: 1247)
  -r=<resource>    resource (default: "")
  -s=<password>    password (default: "")
  -v               verbose
  -V, --version    Print version information and exit.
  -z=<zone>        zone (default: "tempZone")
Commands:
  get    Copy a file or directory from the iRods server to this comptuer.
  put    Copy a file or directory form this computer to the iRods server
  ls     List the files in a directory.
  test   Test connection to server.
  mkdir  Create a directory on the iRods server.
  rm     Delete directory or file on the iRods server.
```
