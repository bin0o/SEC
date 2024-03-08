# HDSLedger

## Introduction

HDSLedger is a simplified permissioned (closed membership) blockchain system with high dependability
guarantees. It uses the Istanbul BFT consensus algorithm to ensure that all nodes run commands
in the same order, achieving State Machine Replication (SMR) and guarantees that all nodes
have the same state.

## Requirements

- [Java 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) - Programming language;

- [Maven 3.8](https://maven.apache.org/) - Build and dependency management tool;

- [Python 3](https://www.python.org/downloads/) - Programming language;

---

### Instalation

Compile and install all modules using:

```
mvn clean install
```

### Execution

```
# Execute service node
                                        Configuration file
cd Service/                                    |
mvn exec:java -Dexec.args='1 ../Configs/regular_config.json'
                           |
            (service node id registered at regular_config.json) 
            
# Execute client
cd Client/
mvn exec:java -Dexec.args="client1 ../Configs/regular_config.json false"
                             |
            (client node id registered at regular_config.json)            
```
---

## Testing
Integration tests can be run with the command below. 
Be in mind that these tests can take a lot of time to complete and sometimes can give false positives, in case of doubt test manually. The configuration file tests are designed to change nodes behavior according to the current instance.
```
python3 tests.py
```

## Configuration Files
Set time for each round
```json
{
"roundTime": 10000
}
```
Set interval for round time refresh
```json
{
  "timerInterval": 500
}
```
Location for all public and private keys
```json
{
"keysLocation": "/Users/goncalosilva/Documents/MEIC/SEC/SEC/Keys"
}
```
### Node configuration

The configuration files can be found at configs folder.

```json
{
    "id": <NODE_ID>,
    "isLeader": <IS_LEADER>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
}
```
### Test configuration

Each test is associated with an instance

```json
{
  "id": <NODE_IDs>,
  "state": {
    "DROP": [
      <Message_Types>
    ],
    "TAMPER": {
      <Message_Type>
    }
  }
}
```
Example:
```json
{
  "tests": [
    [],
    [
      {
        "id": "1",
        "state": {
          "DROP": ["PRE_PREPARE","PREPARE","COMMIT", "DECIDE", "ROUND_CHANGE"],
          "TAMPER": {}
        }
      },
      {
        "id": "2",
        "state": {
          "DROP": [],
          "TAMPER": {
            "PRE_PREPARE": {
              "value": "another_value",
              "signature": ""
            },
            "PREPARE": {
              "value": "another_value"
            },
            "COMMIT": {
              "value": "another_value"
            },
            "DECIDE": {
              "confirmation": "false",
              "index": "2",
              "value": "another_value"
            }
          }
        }
      }
    ],
    []
  ]
}
```

