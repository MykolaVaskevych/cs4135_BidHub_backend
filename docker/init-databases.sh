#!/bin/bash
# Creates additional databases for services.
# bidhub_accounts is already created by POSTGRES_DB env var.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE bidhub_auction;
    CREATE DATABASE bidhub_admin;
    CREATE DATABASE bidhub_notifications;
    CREATE DATABASE bidhub_delivery;
EOSQL
