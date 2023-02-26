# Xeros #
Based on Ascend.

## Setup
- Enable annotation processing for Lombok.
- Create a config.yaml in the working directory with the server state: `server_state: DEBUG`.
Debug will give you the developer rank on login and fill your bank with various items.
It will also give you printouts by default.

## Commands
- `::debug` to toggle debug messages.
- `::combatstats` to toggle combat statistic messages.

## SQL Queries
SqlQuery is the class you use to execute all sql statements and queries.

```
// Anonymous class
new SqlQuery<Object>() {
    @Override
    public Object execute(DatabaseManager context, Connection connection) throws SQLException {
        // do queries on connection here
        return null;
    }
}

// Functional interface
(context, connection) -> {
    return null;
});
```

To actually execute the query you created you call the Server's DatabaseManager: `Server.getDatabaseManager().execute(DatabaseCredentials, SqlQuery)`.
The database credentials are usually stored in the config.yaml (ServerConfiguration): `Server.getConfiguration().getLocalDatabase()`. To use the embedded database 
(which doesn't require a local sql server, it runs in memory) you can call: `Server.getEmbeddedDatabase()`.

Say you wanted to execute a query and use the returned data on a player in some fashion, you would do this:
```
// As of July 10th 2021 you can use execute alone to access the local 
// (non-embedded) sql database which is configured in config.yaml as 
// a DatabaseConfiguration named local_database
Server.getDatabaseManager().execute((context, connection) -> {
    // Notice here we call another sql query, this will share the connection
    List<String> myStrings = new SqlQueryImplementation().execute(context, connection);

    // We have to queue this because we aren't on the main thread and the player
    // will throw an exception for packets sent from outside that thread.
    // Note: this will sometimes require a locking mechanism to prevent the player from moving or leaving
    // that isn't always neccesary though.
    player.addQueuedAction(plr -> {
        myStrings.forEach(it -> plr.sendMessage(it));
    });
}));

```
