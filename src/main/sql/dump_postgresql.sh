#!/bin/bash

pg_dump --schema=public --schema-only -U esper -h localhost esper > src/main/resources/posgresql-ddl.sql
