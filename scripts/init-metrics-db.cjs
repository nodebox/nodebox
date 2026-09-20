"use strict";

var mysql = require("mysql2");

var dbData = {
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  multipleStatements: true,
};

var tableSignups =
  "create TABLE if not exists signups (id INT primary key auto_increment, userid VARCHAR(120) not null, joindate DATETIME not null);";
var tableProjects =
  "create TABLE if not exists projects (id INT primary key auto_increment, projectid VARCHAR(120) not null, userid VARCHAR(120) not null, date DATETIME not null);";
var tableAssets =
  "create TABLE if not exists assets (id INT primary key auto_increment, assetid VARCHAR(120) not null, userid VARCHAR(120) not null, projectid VARCHAR(120) not null, size BIGINT not null, date DATETIME not null);";

function createTables() {
  var connection = mysql.createConnection(dbData);
  connection.query(tableSignups + tableProjects + tableAssets, function (err) {
    if (err) {
      if (err.code === "ER_NO_DB_ERROR") {
        console.warn("Couldn't connect to metrics database. Has the env variable DB_NAME been set?");
      } else {
        console.warn("Couldn't query the database:", err.code);
      }
    }
    connection.end();
  });
  connection.on("error", function (err) {
    if (err.code === "ECONNREFUSED") {
      console.warn("WARN: Couldn't connect to metrics database. Maybe it has not been set up or it is not running.");
    } else if (
      err.code === "ER_DBACCESS_DENIED_ERROR" ||
      err.code === "ER_ACCESS_DENIED_ERROR" ||
      err.code === "ETIMEDOUT"
    ) {
      console.warn("WARN: Could't connect to metrics database. Check your environment variables.", err.code);
    }
  });
}

createTables();
