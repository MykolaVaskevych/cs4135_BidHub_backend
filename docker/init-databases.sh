#!/bin/bash
# Creates additional databases for services.
# bidhub_accounts is already created by POSTGRES_DB env var.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE bidhub_auction;
    CREATE DATABASE bidhub_admin;
EOSQL
