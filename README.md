sqlstream: stream MySQL replication events to Apache Kafka.
===========================================================

Sqlstream connects to a MySQL instance as a replicant and
produces the replication events read onto an Apache Kafka
topic. The events are produced as a JSON-serialized map, keyed
by the server-id which produced the event.

## Configuration

Sqlstream accepts a single argument, its configuration file
which has the following format:

```
mysql.host=localhost
mysql.port=3306
mysql.user=replicant
mysql.password=replicant
topic=sqlstream
bootstrap.servers=localhost:9092
```

## Building and running

You'll need [leiningen](http://leiningen.org) to build the project, you can then
run `lein uberjar` to build the project.

To run it, just issue `java -jar target/sqlstream-0.1.0-standalone.jar <config-file>`

## Use-cases

- Live cache updates from MySQL
- Materialized views from MySQL events

## Event Types

Each JSON payload in the stream contains a `type` key which
determines the rest of the payload's shape.

These three fields will be present in all events:

- `type`: one of `unknown`, `start-v3`, `query`, `stop`, `rotate`, `intvar`,
`load`, `slave`, `create-file`, `append-block`, `exec-load`, `delete-file`, `new-load`,
`user-var`, `format-description`, `xid`, `begin-load-query`, `execute-load-query`,
`table-map`, `pre-ga-write-rows`, `pre-ga-update-rows`, `pre-ga-delete-rows`,
`write-rows`, `update-rows`, `delete-rows`, `incident`, `heartbeat`, `ignorable`,
`rows-query`, `ext-write-rows`, `ext-update-rows`, `ext-delete-rows`, `gtid`,
`anonymous-gtid`, `previous-gtids`.
- `timestamp`: the timestamp of the event
- `server-id`: server-id from which this request originated, also used as the record key.

Here are the key event-type-specific fields:

<table>
<tr><th>type</th><th>fields</th></tr>
<tr><td>format-description</td><td>binlog-version, server-version, header-length</td></tr>
<tr><td>gtid</td><td>gtid, flags</td></tr>
<tr><td>query</td><td>sql, error-code, database, exec-time</td></tr>
<tr><td>rotate</td><td>binlog-filename, binlog-position</td></tr>
<tr><td>rows-query</td><td>binlog-filename</td></tr>
<tr><td>table-map</td><td>database, table, column-types, column-metadata, column-nullability</td></tr>
<tr><td>update-rows</td><td>cols-old, cols-new, rows, table-id</td></tr>
<tr><td>write-rows</td><td>cols, rows, table-id</td></tr>
<tr><td>delete-rows</td><td>cols, rows, table-id</td></tr>
<tr><td>xid</td><td>xid</td></tr>
</table>



Assuming a client had the following conversation with
a MySQL server:

```SQL
create database foobar;
use foobar;
create table user (
  id int primary key not null auto_increment,
  login varchar(255),
  name varchar(255)
);
insert into user(login,name) values('bob','bob');
insert into user(login,name) values('bill','bill');
update user set name="Billy Bob" where id=2;
delete from user where id=1;
```

The following JSON payloads would be produced in the kafka topic:

#### Preamble

```json
{"server-id":1,
 "timestamp":0,
 "binlog-position":574,
 "binlog-filename":"mysql-bin.000064",
 "type":"rotate"}
{"server-id":1,
 "timestamp":1426511022000,
 "header-length":19,
 "server-version":"10.0.17-MariaDB-log",
 "binlog-version":4,
 "type":"format-description"}
```

#### Create database and Create table

```json
{"server-id":1,
 "timestamp":1426512246000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"# Dum",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512246000,
 "exec-time":0,
 "database":"foobar",
 "error-code":0,
 "sql":"create database foobar",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512336000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"# Dum",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512336000,
 "exec-time":0,
 "database":"foobar",
 "error-code":0,
 "sql":"create table user ( id int primary key not null auto_increment, login varchar(255), name varchar(255))",
 "type":"query"}
```

#### Inserts

```json
{"server-id":1,
 "timestamp":1426512368000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"BEGIN",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512368000,
 "column-nullability":[1,2],
 "column-metadata":[0,765,765],
 "column-types":[3,15,15],
 "table":"user",
 "database":"foobar",
 "type":"table-map"}
{"server-id":1,
 "timestamp":1426512368000,
 "table-id":73,
 "rows":[["1","bob","bob"]],
 "cols":[0,1,2],
 "type":"write-rows"}
{"server-id":1,
 "timestamp":1426512368000,
 "xid":32,
 "type":"xid"}

{"server-id":1,
 "timestamp":1426512383000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"BEGIN",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512383000,
 "column-nullability":[1,2],
 "column-metadata":[0,765,765],
 "column-types":[3,15,15],
 "table":"user",
 "database":"foobar",
 "type":"table-map"}
{"server-id":1,
 "timestamp":1426512383000,
 "table-id":73,
 "rows":[["2","bill","bill"]],
 "cols":[0,1,2],
 "type":"write-rows"}
{"server-id":1,
 "timestamp":1426512383000,
 "xid":33,
 "type":"xid"}
```

#### Updates

```json
{"server-id":1,
 "timestamp":1426512399000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"BEGIN",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512399000,
 "column-nullability":[1,2],
 "column-metadata":[0,765,765],
 "column-types":[3,15,15],
 "table":"user",
 "database":"foobar",
 "type":"table-map"}
{"server-id":1,
 "timestamp":1426512399000,
 "table-id":73,
 "rows":[[["2","bill","bill"],["2","bill","Billy Bob"]]],
 "cols-new":[0,1,2],
 "cols-old":[0,1,2],
 "type":"update-rows"}
{"server-id":1,
 "timestamp":1426512399000,
 "xid":34,
 "type":"xid"}
```

#### Deletes

```json
{"server-id":1,
 "timestamp":1426512411000,
 "exec-time":0,
 "database":"",
 "error-code":0,
 "sql":"BEGIN",
 "type":"query"}
{"server-id":1,
 "timestamp":1426512411000,
 "column-nullability":[1,2],
 "column-metadata":[0,765,765],
 "column-types":[3,15,15],
 "table":"user",
 "database":"foobar",
 "type":"table-map"}
{"server-id":1,
 "timestamp":1426512411000,
 "table-id":73,
 "rows":[["1","bob","bob"]],
 "cols":[0,1,2],
 "type":"delete-rows"}
{"server-id":1,
 "timestamp":1426512411000,
 "xid":35,
 "type":"xid"}
```

## Caveats

- No support for keeping track of offsets
- No configurable key or serialization
- MySQL only (a PostgreSQL implementation would be nice)

## License

Copyright 2015 Pierre-Yves Ritschard <pyr@spootnik.org>

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
